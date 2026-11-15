package br.com.fiap.consertaai.service;

import br.com.fiap.consertaai.domain.entity.Categoria;
import br.com.fiap.consertaai.domain.entity.HistoricoStatus;
import br.com.fiap.consertaai.domain.entity.Ocorrencia;
import br.com.fiap.consertaai.domain.entity.Usuario;
import br.com.fiap.consertaai.domain.enums.OrigemTriagem;
import br.com.fiap.consertaai.domain.enums.StatusOcorrencia;
import br.com.fiap.consertaai.dto.OcorrenciaDtos.*;
import br.com.fiap.consertaai.exception.RecursoNaoEncontradoException;
import br.com.fiap.consertaai.exception.RegraNegocioException;
import br.com.fiap.consertaai.ia.ResultadoTriagem;
import br.com.fiap.consertaai.ia.TriagemEntrada;
import br.com.fiap.consertaai.messaging.OcorrenciaCriadaEvento;
import br.com.fiap.consertaai.repository.CategoriaRepository;
import br.com.fiap.consertaai.repository.HistoricoStatusRepository;
import br.com.fiap.consertaai.repository.OcorrenciaRepository;
import br.com.fiap.consertaai.repository.UsuarioRepository;
import br.com.fiap.consertaai.security.UsuarioLogado;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class OcorrenciaService {

    private static final Set<StatusOcorrencia> EM_ABERTO =
            EnumSet.of(StatusOcorrencia.EM_TRIAGEM, StatusOcorrencia.ABERTA, StatusOcorrencia.EM_ANDAMENTO);
    private static final Set<StatusOcorrencia> EDITAVEL_PELO_AUTOR =
            EnumSet.of(StatusOcorrencia.EM_TRIAGEM, StatusOcorrencia.ABERTA);

    private final OcorrenciaRepository ocorrenciaRepository;
    private final CategoriaRepository categoriaRepository;
    private final UsuarioRepository usuarioRepository;
    private final HistoricoStatusRepository historicoRepository;
    private final PrioridadeCalculator prioridadeCalculator;
    private final ApplicationEventPublisher eventos;

    // ------------------------------------------------------------------ consultas

    @Transactional(readOnly = true)
    public Page<OcorrenciaResponse> listar(StatusOcorrencia status, String bairro, Long categoriaId, Pageable pageable) {
        Specification<Ocorrencia> filtro = (root, query, cb) -> cb.conjunction();
        if (status != null) {
            filtro = filtro.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }
        if (bairro != null && !bairro.isBlank()) {
            String b = bairro.trim().toLowerCase(Locale.ROOT);
            filtro = filtro.and((root, query, cb) -> cb.equal(cb.lower(root.get("bairro")), b));
        }
        if (categoriaId != null) {
            filtro = filtro.and((root, query, cb) -> cb.equal(root.get("categoria").get("id"), categoriaId));
        }
        return ocorrenciaRepository.findAll(filtro, pageable).map(OcorrenciaResponse::de);
    }

    @Transactional(readOnly = true)
    public Page<OcorrenciaResponse> minhas(Long usuarioId, Pageable pageable) {
        return ocorrenciaRepository.findByAutorId(usuarioId, pageable).map(OcorrenciaResponse::de);
    }

    /** Fila do gestor: em aberto, ordenada por prioridade (a ordenacao vem da consulta). */
    @Transactional(readOnly = true)
    public Page<OcorrenciaResponse> fila(Pageable pageable) {
        Pageable semOrdenacao = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
        return ocorrenciaRepository.findByStatusInOrderByPrioridadeDescCriadoEmAsc(EM_ABERTO, semOrdenacao)
                .map(OcorrenciaResponse::de);
    }

    @Transactional(readOnly = true)
    public OcorrenciaResponse buscar(Long id) {
        return OcorrenciaResponse.de(obterDetalhada(id));
    }

    @Transactional(readOnly = true)
    public List<HistoricoResponse> historico(Long id) {
        if (!ocorrenciaRepository.existsById(id)) {
            throw new RecursoNaoEncontradoException("Ocorrência", id);
        }
        return historicoRepository.findByOcorrenciaIdOrderByDataAscIdAsc(id).stream()
                .map(HistoricoResponse::de).toList();
    }

    // ------------------------------------------------------------------ escrita

    /**
     * Abre a ocorrencia em EM_TRIAGEM e publica "ocorrencia.criada". A resposta volta na hora;
     * a IA classifica em seguida, de forma assincrona (TriagemConsumer).
     */
    @Transactional
    public OcorrenciaResponse criar(OcorrenciaRequest request, Long autorId) {
        Usuario autor = usuarioRepository.findById(autorId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário", autorId));

        Ocorrencia ocorrencia = Ocorrencia.builder()
                .status(StatusOcorrencia.EM_TRIAGEM)
                .autor(autor)
                .build();
        preencher(ocorrencia, request);
        ocorrencia.setCriadoEm(LocalDateTime.now());
        prioridadeCalculator.recalcular(ocorrencia);
        ocorrencia = ocorrenciaRepository.save(ocorrencia);

        registrarHistorico(ocorrencia, null, StatusOcorrencia.EM_TRIAGEM, "Ocorrência registrada pelo cidadão", autor);

        // Evento do Spring: o EventosPublisher so o envia ao RabbitMQ depois do COMMIT
        eventos.publishEvent(OcorrenciaCriadaEvento.de(ocorrencia.getId()));
        return OcorrenciaResponse.de(ocorrencia);
    }

    /** O autor corrige o relato enquanto ninguem comecou a atender. */
    @Transactional
    public OcorrenciaResponse atualizar(Long id, OcorrenciaRequest request, UsuarioLogado usuario) {
        Ocorrencia ocorrencia = obterDetalhada(id);
        exigirAutor(ocorrencia, usuario);
        if (!EDITAVEL_PELO_AUTOR.contains(ocorrencia.getStatus())) {
            throw new RegraNegocioException("Só é possível editar ocorrências em triagem ou abertas. Status atual: "
                    + ocorrencia.getStatus() + ".");
        }
        preencher(ocorrencia, request);
        return OcorrenciaResponse.de(ocorrencia);
    }

    /** Autor exclui enquanto ninguem atendeu; o gestor pode excluir qualquer uma (ex.: spam). */
    @Transactional
    public void excluir(Long id, UsuarioLogado usuario) {
        Ocorrencia ocorrencia = ocorrenciaRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Ocorrência", id));
        if (!usuario.gestor()) {
            exigirAutor(ocorrencia, usuario);
            if (!EDITAVEL_PELO_AUTOR.contains(ocorrencia.getStatus())) {
                throw new RegraNegocioException("Esta ocorrência já está em atendimento e não pode ser excluída.");
            }
        }
        ocorrenciaRepository.delete(ocorrencia);
    }

    /** Gestor move a ocorrencia pelo ciclo de vida; transicoes invalidas sao recusadas (422). */
    @Transactional
    public OcorrenciaResponse alterarStatus(Long id, StatusRequest request, Long gestorId) {
        Ocorrencia ocorrencia = obterDetalhada(id);
        StatusOcorrencia atual = ocorrencia.getStatus();
        if (!atual.podeIrPara(request.status())) {
            throw new RegraNegocioException("Transição inválida: " + atual + " → " + request.status()
                    + ". Permitidas: " + atual.proximosPermitidos() + ".");
        }
        ocorrencia.setStatus(request.status());
        registrarHistorico(ocorrencia, atual, request.status(), request.observacao(),
                usuarioRepository.getReferenceById(gestorId));
        return OcorrenciaResponse.de(ocorrencia);
    }

    /**
     * Atualiza o total de apoios vindo da API .NET (RabbitMQ ou PATCH interno).
     *
     * @return false quando o evento foi descartado (ocorrencia inexistente ou evento antigo)
     */
    @Transactional
    public boolean atualizarApoios(Long id, int totalApoios, Instant dataEvento) {
        Optional<Ocorrencia> encontrada = ocorrenciaRepository.findById(id);
        if (encontrada.isEmpty()) {
            log.warn("Evento de apoio para a ocorrencia {} inexistente; descartado", id);
            return false;
        }
        Ocorrencia ocorrencia = encontrada.get();
        LocalDateTime quando = dataEvento == null ? null : LocalDateTime.ofInstant(dataEvento, ZoneOffset.UTC);

        // Mensagens podem chegar fora de ordem: um total mais antigo nao sobrescreve um mais novo
        if (quando != null && ocorrencia.getUltimoEventoApoioEm() != null
                && quando.isBefore(ocorrencia.getUltimoEventoApoioEm())) {
            log.info("Evento de apoio antigo para a ocorrencia {} ignorado", id);
            return false;
        }
        ocorrencia.setQtdApoios(Math.max(totalApoios, 0));
        if (quando != null) {
            ocorrencia.setUltimoEventoApoioEm(quando);
        }
        prioridadeCalculator.recalcular(ocorrencia);
        return true;
    }

    // ------------------------------------------------------------------ triagem

    /** Passo 1 da triagem: fotografia dos dados, em transacao curta de leitura. */
    @Transactional(readOnly = true)
    public Optional<TriagemEntrada> dadosParaTriagem(Long id) {
        return ocorrenciaRepository.buscarDetalhada(id).map(o -> {
            var categorias = categoriaRepository.findByAtivaTrueOrderByNomeAsc().stream()
                    .map(c -> new TriagemEntrada.CategoriaOpcao(c.getNome(), c.getDescricao(), c.getOrgaoResponsavel()))
                    .toList();
            var candidatas = ocorrenciaRepository
                    .findTop15ByBairroIgnoreCaseAndStatusInAndIdNotOrderByCriadoEmDesc(o.getBairro(), EM_ABERTO, o.getId())
                    .stream()
                    .map(c -> new TriagemEntrada.Candidata(c.getId(), c.getTitulo(), c.getDescricao(),
                            c.getCategoria() == null ? null : c.getCategoria().getNome()))
                    .toList();
            return new TriagemEntrada(o.getId(), o.getTitulo(), o.getDescricao(), o.getBairro(),
                    o.getCategoria() == null ? null : o.getCategoria().getNome(), categorias, candidatas);
        });
    }

    /** Passo 3 da triagem: grava o resultado e libera a ocorrencia para a fila. */
    @Transactional
    public void aplicarTriagem(Long id, ResultadoTriagem resultado) {
        Ocorrencia ocorrencia = ocorrenciaRepository.buscarDetalhada(id).orElse(null);
        if (ocorrencia == null) {
            return;
        }
        if (ocorrencia.getCategoria() == null) {
            ocorrencia.setCategoria(categoriaPorNome(resultado.categoria()));
        }
        Categoria categoria = ocorrencia.getCategoria();

        ocorrencia.setUrgencia(resultado.urgencia());
        ocorrencia.setRiscoImediato(resultado.riscoImediato());
        ocorrencia.setResumoIa(resultado.resumo());
        ocorrencia.setOrgaoSugerido(resultado.orgaoSugerido() != null && !resultado.orgaoSugerido().isBlank()
                ? resultado.orgaoSugerido() : categoria == null ? null : categoria.getOrgaoResponsavel());
        ocorrencia.setOrigemTriagem(resultado.origem());
        ocorrencia.setPossivelDuplicadaDe(resultado.possivelDuplicadaDeId() == null ? null
                : ocorrenciaRepository.getReferenceById(resultado.possivelDuplicadaDeId()));
        ocorrencia.setTriadaEm(LocalDateTime.now());
        prioridadeCalculator.recalcular(ocorrencia);

        if (ocorrencia.getStatus() == StatusOcorrencia.EM_TRIAGEM) {
            ocorrencia.setStatus(StatusOcorrencia.ABERTA);
            registrarHistorico(ocorrencia, StatusOcorrencia.EM_TRIAGEM, StatusOcorrencia.ABERTA,
                    "Triagem automática concluída (" + (resultado.origem() == OrigemTriagem.IA ? "IA" : "regras") + ")",
                    null);
        }
        log.info("Ocorrencia {} triada por {}: {} / {} / prioridade {}", id, resultado.origem(),
                categoria == null ? "-" : categoria.getNome(), resultado.urgencia(), ocorrencia.getPrioridade());
    }

    // ------------------------------------------------------------------ apoio

    private Ocorrencia obterDetalhada(Long id) {
        return ocorrenciaRepository.buscarDetalhada(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Ocorrência", id));
    }

    private void exigirAutor(Ocorrencia ocorrencia, UsuarioLogado usuario) {
        if (!ocorrencia.getAutor().getId().equals(usuario.id())) {
            throw new AccessDeniedException("Somente o autor pode alterar esta ocorrência.");
        }
    }

    private void preencher(Ocorrencia o, OcorrenciaRequest r) {
        o.setTitulo(r.titulo().trim());
        o.setDescricao(r.descricao().trim());
        o.setEndereco(r.endereco().trim());
        o.setBairro(r.bairro().trim());
        o.setLatitude(r.latitude());
        o.setLongitude(r.longitude());
        if (r.categoriaId() != null) {
            Categoria categoria = categoriaRepository.findById(r.categoriaId())
                    .filter(Categoria::getAtiva)
                    .orElseThrow(() -> new RegraNegocioException("Categoria " + r.categoriaId() + " inexistente ou inativa."));
            o.setCategoria(categoria);
        }
    }

    private void registrarHistorico(Ocorrencia o, StatusOcorrencia de, StatusOcorrencia para,
                                    String observacao, Usuario responsavel) {
        historicoRepository.save(HistoricoStatus.builder()
                .ocorrencia(o)
                .statusAnterior(de)
                .statusNovo(para)
                .observacao(observacao)
                .responsavel(responsavel)
                .data(LocalDateTime.now())
                .build());
    }

    /** A IA devolve o nome; compara sem acento e sem caixa. Nome desconhecido cai em "Outros". */
    private Categoria categoriaPorNome(String nome) {
        String alvo = normalizar(nome);
        List<Categoria> ativas = categoriaRepository.findByAtivaTrueOrderByNomeAsc();
        return ativas.stream().filter(c -> normalizar(c.getNome()).equals(alvo)).findFirst()
                .or(() -> ativas.stream().filter(c -> normalizar(c.getNome()).equals("outros")).findFirst())
                .orElse(null);
    }

    private static String normalizar(String s) {
        if (s == null) return "";
        return Normalizer.normalize(s.trim(), Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
    }
}
