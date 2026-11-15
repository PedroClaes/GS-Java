-- =====================================================================
-- V2 - Dados de demonstracao
--
-- Todos os usuarios usam a senha "senha123" (hash BCrypt abaixo).
-- Datas relativas a NOW(): o cenario continua coerente em qualquer dia.
-- A ocorrencia 1 existe de proposito: os exemplos da API .NET usam o id 1.
-- =====================================================================

INSERT INTO usuarios (nome, email, senha_hash, papel, bairro, criado_em) VALUES
    ('Maria Souza',     'maria@consertaai.com',  '$2a$10$XtNSdM4pp0hO5rVo0pm6HeQNlqlzn545wzIxXJapxwk0JpqITLjvq', 'CIDADAO', 'Centro',       NOW() - INTERVAL '40 days'),
    ('Joao Lima',       'joao@consertaai.com',   '$2a$10$XtNSdM4pp0hO5rVo0pm6HeQNlqlzn545wzIxXJapxwk0JpqITLjvq', 'CIDADAO', 'Jardim Itália', NOW() - INTERVAL '30 days'),
    ('Ana Gestora',     'gestor@consertaai.com', '$2a$10$XtNSdM4pp0hO5rVo0pm6HeQNlqlzn545wzIxXJapxwk0JpqITLjvq', 'GESTOR',  NULL,           NOW() - INTERVAL '60 days');

INSERT INTO categorias (nome, descricao, orgao_responsavel, prazo_sla_dias) VALUES
    ('Buraco na via',         'Buracos, afundamentos e asfalto danificado',     'Secretaria de Obras',              10),
    ('Iluminação pública',    'Poste apagado, lâmpada piscando, fiação exposta', 'Departamento de Iluminação Pública', 5),
    ('Lixo e entulho',        'Descarte irregular, lixo acumulado, entulho',     'Secretaria de Serviços Urbanos',     3),
    ('Calçada e acessibilidade', 'Calçada quebrada, rampa obstruída',          'Secretaria de Obras',              20),
    ('Sinalização de trânsito', 'Placa caída, semáforo com defeito, faixa apagada', 'Departamento de Trânsito',       4),
    ('Árvores e áreas verdes',  'Árvore com risco de queda, poda, galhos na via', 'Secretaria de Meio Ambiente',      7),
    ('Alagamento e esgoto',   'Bueiro entupido, esgoto a céu aberto, alagamento', 'Companhia de Saneamento',        5),
    ('Outros',                'Problemas que não se encaixam nas demais',       'Subprefeitura',                    15);

-- Ocorrencias ja triadas, para a fila do gestor e o mapa ja terem conteudo
INSERT INTO ocorrencias (titulo, descricao, endereco, bairro, latitude, longitude, status,
                         categoria_id, urgencia, risco_imediato, resumo_ia, orgao_sugerido, origem_triagem,
                         triada_em, qtd_apoios, prioridade, autor_id, criado_em, atualizado_em) VALUES
    ('Buraco grande na Rua XV de Novembro',
     'Buraco de quase um metro perto da faixa de pedestres, carros desviando pela contramão.',
     'Rua XV de Novembro, 820', 'Centro', -27.100345, -52.615600, 'ABERTA',
     1, 'ALTA', TRUE, 'Buraco profundo em via movimentada, com carros invadindo a contramão para desviar.',
     'Secretaria de Obras', 'IA', NOW() - INTERVAL '5 days', 7, 84, 1,
     NOW() - INTERVAL '5 days', NOW() - INTERVAL '1 day'),

    ('Poste apagado na praça',
     'Três postes da praça estão apagados há uma semana, a praça fica totalmente escura à noite.',
     'Praça Coronel Bertaso', 'Centro', -27.101900, -52.616800, 'EM_ANDAMENTO',
     2, 'MEDIA', FALSE, 'Iluminação apagada em praça pública, causando insegurança à noite.',
     'Departamento de Iluminação Pública', 'IA', NOW() - INTERVAL '8 days', 4, 43, 2,
     NOW() - INTERVAL '8 days', NOW() - INTERVAL '2 days'),

    ('Lixo acumulado em terreno baldio',
     'Moradores estão jogando lixo e entulho no terreno da esquina, já tem rato.',
     'Rua Marechal Floriano, 1500', 'Jardim Itália', -27.090500, -52.620100, 'ABERTA',
     3, 'MEDIA', FALSE, 'Descarte irregular de lixo e entulho em terreno, com presença de roedores.',
     'Secretaria de Serviços Urbanos', 'REGRAS', NOW() - INTERVAL '3 days', 2, 37, 2,
     NOW() - INTERVAL '3 days', NOW() - INTERVAL '3 days'),

    ('Árvore inclinada sobre a fiação',
     'Árvore grande inclinada encostando nos fios da rede elétrica em frente à escola.',
     'Rua Nereu Ramos, 300', 'Centro', -27.098700, -52.612300, 'ABERTA',
     6, 'CRITICA', TRUE, 'Árvore com risco de queda sobre a rede elétrica em frente a uma escola.',
     'Secretaria de Meio Ambiente', 'IA', NOW() - INTERVAL '1 day', 3, 87, 1,
     NOW() - INTERVAL '1 day', NOW() - INTERVAL '1 day'),

    ('Bueiro entupido',
     'Quando chove a rua alaga porque o bueiro está entupido de folhas.',
     'Av. Getúlio Vargas, 2100', 'Jardim Itália', -27.093300, -52.618900, 'RESOLVIDA',
     7, 'MEDIA', FALSE, 'Bueiro obstruído causando alagamento em dias de chuva.',
     'Companhia de Saneamento', 'IA', NOW() - INTERVAL '20 days', 5, 45, 1,
     NOW() - INTERVAL '20 days', NOW() - INTERVAL '10 days');

INSERT INTO historico_status (ocorrencia_id, status_anterior, status_novo, observacao, responsavel_id, data) VALUES
    (1, NULL,           'EM_TRIAGEM',   'Ocorrência registrada pelo cidadão',          1,    NOW() - INTERVAL '5 days'),
    (1, 'EM_TRIAGEM',   'ABERTA',       'Triagem automática concluída (IA)',            NULL, NOW() - INTERVAL '5 days'),
    (2, NULL,           'EM_TRIAGEM',   'Ocorrência registrada pelo cidadão',          2,    NOW() - INTERVAL '8 days'),
    (2, 'EM_TRIAGEM',   'ABERTA',       'Triagem automática concluída (IA)',            NULL, NOW() - INTERVAL '8 days'),
    (2, 'ABERTA',       'EM_ANDAMENTO', 'Equipe de manutenção agendada',                3,    NOW() - INTERVAL '2 days'),
    (3, NULL,           'EM_TRIAGEM',   'Ocorrência registrada pelo cidadão',          2,    NOW() - INTERVAL '3 days'),
    (3, 'EM_TRIAGEM',   'ABERTA',       'Triagem automática concluída (regras)',        NULL, NOW() - INTERVAL '3 days'),
    (4, NULL,           'EM_TRIAGEM',   'Ocorrência registrada pelo cidadão',          1,    NOW() - INTERVAL '1 day'),
    (4, 'EM_TRIAGEM',   'ABERTA',       'Triagem automática concluída (IA)',            NULL, NOW() - INTERVAL '1 day'),
    (5, NULL,           'EM_TRIAGEM',   'Ocorrência registrada pelo cidadão',          1,    NOW() - INTERVAL '20 days'),
    (5, 'EM_TRIAGEM',   'ABERTA',       'Triagem automática concluída (IA)',            NULL, NOW() - INTERVAL '20 days'),
    (5, 'ABERTA',       'EM_ANDAMENTO', 'Limpeza programada',                           3,    NOW() - INTERVAL '15 days'),
    (5, 'EM_ANDAMENTO', 'RESOLVIDA',    'Bueiro desobstruído',                          3,    NOW() - INTERVAL '10 days');
