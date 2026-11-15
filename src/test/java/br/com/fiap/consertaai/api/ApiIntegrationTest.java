package br.com.fiap.consertaai.api;

import br.com.fiap.consertaai.domain.entity.Usuario;
import br.com.fiap.consertaai.domain.enums.Papel;
import br.com.fiap.consertaai.repository.UsuarioRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Fluxo HTTP de ponta a ponta com o contexto completo (H2 em memoria, sem broker e sem IA):
 * login gera JWT, papeis protegem as rotas e os erros saem no formato padrao.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApiIntegrationTest {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper json;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void usuarios() {
        criar("cidada@teste.com", Papel.CIDADAO);
        criar("gestor@teste.com", Papel.GESTOR);
    }

    private void criar(String email, Papel papel) {
        if (usuarioRepository.existsByEmailIgnoreCase(email)) return;
        usuarioRepository.save(Usuario.builder().nome(papel.name()).email(email)
                .senhaHash(passwordEncoder.encode("senha123")).papel(papel).build());
    }

    private String token(String email) throws Exception {
        String corpo = mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"senha\":\"senha123\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode node = json.readTree(corpo);
        return node.get("token").asText();
    }

    private static final String OCORRENCIA = """
            {"titulo":"Poste apagado na esquina","descricao":"O poste da esquina está apagado há dias",
             "endereco":"Rua A, 10","bairro":"Centro"}""";

    @Test
    void semTokenRecebe401NoFormatoPadrao() throws Exception {
        mvc.perform(post("/ocorrencias").contentType(MediaType.APPLICATION_JSON).content(OCORRENCIA))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void senhaErradaRecebe401() throws Exception {
        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"cidada@teste.com\",\"senha\":\"errada\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void cidadaoAbreOcorrenciaQueComecaEmTriagem() throws Exception {
        mvc.perform(post("/ocorrencias").header("Authorization", "Bearer " + token("cidada@teste.com"))
                        .contentType(MediaType.APPLICATION_JSON).content(OCORRENCIA))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.status").value("EM_TRIAGEM"))
                .andExpect(jsonPath("$.autor.nome").value("CIDADAO"));
    }

    @Test
    void cidadaoNaoAcessaAFilaDoGestor() throws Exception {
        mvc.perform(get("/ocorrencias/fila").header("Authorization", "Bearer " + token("cidada@teste.com")))
                .andExpect(status().isForbidden());
    }

    @Test
    void gestorAcessaAFila() throws Exception {
        mvc.perform(get("/ocorrencias/fila").header("Authorization", "Bearer " + token("gestor@teste.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conteudo").isArray());
    }

    @Test
    void gestorNaoAbreOcorrencia() throws Exception {
        mvc.perform(post("/ocorrencias").header("Authorization", "Bearer " + token("gestor@teste.com"))
                        .contentType(MediaType.APPLICATION_JSON).content(OCORRENCIA))
                .andExpect(status().isForbidden());
    }

    @Test
    void validacaoDevolveOsCamposComErro() throws Exception {
        mvc.perform(post("/ocorrencias").header("Authorization", "Bearer " + token("cidada@teste.com"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"titulo\":\"x\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos").isArray())
                .andExpect(jsonPath("$.campos[?(@.campo == 'descricao')]").exists());
    }

    @Test
    void cadastroPublicoCriaSempreCidadao() throws Exception {
        mvc.perform(post("/auth/registro").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Novo\",\"email\":\"novo@teste.com\",\"senha\":\"senha123\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.usuario.papel").value("CIDADAO"))
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void ocorrenciaInexistenteDa404() throws Exception {
        mvc.perform(get("/ocorrencias/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensagem").exists());
    }
}
