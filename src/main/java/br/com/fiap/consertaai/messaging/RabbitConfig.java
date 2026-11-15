package br.com.fiap.consertaai.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.DefaultJackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Topologia do RabbitMQ (requisito 5, opcao A).
 *
 * <pre>
 *                       exchange TOPIC "consertaai.eventos"  (compartilhada com a API .NET)
 *                        │                                   │
 *   "ocorrencia.criada"  │  publicado pela Java              │  "apoio.*"  publicado pela .NET
 *                        ▼                                   ▼
 *        fila consertaai.java.triagem            fila consertaai.apoios
 *        (DLQ: consertaai.java.triagem.dlq)      (declarada igual a .NET)
 *                        │                                   │
 *                 TriagemConsumer                      ApoiosConsumer
 *            (Spring AI classifica)            (atualiza apoios e prioridade)
 * </pre>
 *
 * A fila consertaai.apoios e declarada com os MESMOS argumentos que a API .NET usa
 * (duravel, sem argumentos extras): o RabbitMQ recusa redeclarar uma fila com argumentos
 * diferentes (PRECONDITION_FAILED). Por isso ela nao tem DLQ.
 */
@Configuration
@EnableAsync
public class RabbitConfig {

    public static final String ROUTING_OCORRENCIA_CRIADA = "ocorrencia.criada";
    public static final String ROUTING_APOIOS = "apoio.*";
    public static final String ROUTING_DLQ = "triagem.morta";

    @Value("${consertaai.rabbit.exchange}")
    private String exchange;
    @Value("${consertaai.rabbit.fila-apoios}")
    private String filaApoios;
    @Value("${consertaai.rabbit.fila-triagem}")
    private String filaTriagem;
    @Value("${consertaai.rabbit.dead-letter-exchange}")
    private String deadLetterExchange;
    @Value("${consertaai.rabbit.fila-triagem-dlq}")
    private String filaTriagemDlq;

    /** Topic: roteia por padrao de routing key ("apoio.*" pega registrado e removido). */
    @Bean
    public TopicExchange eventosExchange() {
        return new TopicExchange(exchange, true, false);
    }

    @Bean
    public Queue filaTriagem() {
        return QueueBuilder.durable(filaTriagem)
                .deadLetterExchange(deadLetterExchange)
                .deadLetterRoutingKey(ROUTING_DLQ)
                .build();
    }

    @Bean
    public Binding bindingTriagem() {
        return BindingBuilder.bind(filaTriagem()).to(eventosExchange()).with(ROUTING_OCORRENCIA_CRIADA);
    }

    @Bean
    public Queue filaApoios() {
        return QueueBuilder.durable(filaApoios).build();
    }

    @Bean
    public Binding bindingApoios() {
        return BindingBuilder.bind(filaApoios()).to(eventosExchange()).with(ROUTING_APOIOS);
    }

    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(deadLetterExchange, true, false);
    }

    @Bean
    public Queue filaTriagemDlq() {
        return QueueBuilder.durable(filaTriagemDlq).build();
    }

    @Bean
    public Binding bindingDlq() {
        return BindingBuilder.bind(filaTriagemDlq()).to(deadLetterExchange()).with(ROUTING_DLQ);
    }

    /**
     * Mensagens em JSON. O tipo do parametro do @RabbitListener define a classe de destino,
     * o que permite ler o JSON do .NET (que nao manda cabecalho de tipo Java).
     * Pacotes confiaveis: um cabecalho de tipo forjado nao cria classes arbitrarias.
     */
    @Bean
    public MessageConverter jsonMessageConverter(ObjectMapper objectMapper) {
        DefaultJackson2JavaTypeMapper typeMapper = new DefaultJackson2JavaTypeMapper();
        typeMapper.setTrustedPackages(OcorrenciaCriadaEvento.class.getPackageName());
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter(objectMapper);
        converter.setJavaTypeMapper(typeMapper);
        return converter;
    }
}
