package br.com.busco.planejamento.infra.rabbit;

import org.aopalliance.aop.Advice;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.retry.RejectAndDontRequeueRecoverer;
import org.springframework.amqp.support.converter.SimpleMessageConverter;
import org.springframework.boot.autoconfigure.amqp.SimpleRabbitListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.support.RetryTemplate;

import br.com.busco.planejamento.sk.ddd.TenantAwareMessagePostProcessor;

/**
 * Topologia e transporte RabbitMQ conforme CONTRACT §1 e §3:
 * comando roteia para {@value RabbitTopologia#COMMANDS_QUEUE}; evento de
 * resultado para {@value RabbitTopologia#EVENTS_QUEUE}; falha permanente vai
 * para a DLQ dedicada via {@value RabbitTopologia#DLX}.
 *
 * <p>As mensagens trafegam como JSON puro (String pré-serializada) com
 * {@link SimpleMessageConverter}; nunca há {@code __TypeId__}.</p>
 */
@Configuration
public class RabbitMQConfig {

    @Bean
    public TopicExchange planejamentoExchange() {
        return ExchangeBuilder.topicExchange(RabbitTopologia.COMMANDS_EXCHANGE).durable(true).build();
    }

    @Bean
    public TopicExchange planejamentoEventsExchange() {
        return ExchangeBuilder.topicExchange(RabbitTopologia.EVENTS_EXCHANGE).durable(true).build();
    }

    @Bean
    public TopicExchange planejamentoDlx() {
        return ExchangeBuilder.topicExchange(RabbitTopologia.DLX).durable(true).build();
    }

    @Bean
    public Queue planejamentoCommandsQueue() {
        return QueueBuilder.durable(RabbitTopologia.COMMANDS_QUEUE)
                .withArgument("x-dead-letter-exchange", RabbitTopologia.DLX)
                .withArgument("x-dead-letter-routing-key", RabbitTopologia.COMMANDS_DLQ)
                .build();
    }

    @Bean
    public Queue planejamentoCommandsDlq() {
        return QueueBuilder.durable(RabbitTopologia.COMMANDS_DLQ).build();
    }

    @Bean
    public Queue buscoApiPlanejamentoQueue() {
        return QueueBuilder.durable(RabbitTopologia.EVENTS_QUEUE)
                .withArgument("x-dead-letter-exchange", RabbitTopologia.DLX)
                .withArgument("x-dead-letter-routing-key", RabbitTopologia.EVENTS_DLQ)
                .build();
    }

    @Bean
    public Queue buscoApiPlanejamentoDlq() {
        return QueueBuilder.durable(RabbitTopologia.EVENTS_DLQ).build();
    }

    @Bean
    public Binding commandsBinding(Queue planejamentoCommandsQueue, TopicExchange planejamentoExchange) {
        return BindingBuilder.bind(planejamentoCommandsQueue).to(planejamentoExchange).with("#");
    }

    @Bean
    public Binding eventsBinding(Queue buscoApiPlanejamentoQueue, TopicExchange planejamentoEventsExchange) {
        return BindingBuilder.bind(buscoApiPlanejamentoQueue).to(planejamentoEventsExchange).with("#");
    }

    @Bean
    public Binding commandsDlqBinding(Queue planejamentoCommandsDlq, TopicExchange planejamentoDlx) {
        return BindingBuilder.bind(planejamentoCommandsDlq).to(planejamentoDlx).with(RabbitTopologia.COMMANDS_DLQ);
    }

    @Bean
    public Binding eventsDlqBinding(Queue buscoApiPlanejamentoDlq, TopicExchange planejamentoDlx) {
        return BindingBuilder.bind(buscoApiPlanejamentoDlq).to(planejamentoDlx).with(RabbitTopologia.EVENTS_DLQ);
    }

    /**
     * Consumidores leem {@code String} e desserializam por header; retry de 3
     * tentativas com backoff exponencial (1s, 2s, 4s; teto 10s) e rejeição
     * para a DLQ ao esgotar (CONTRACT §3).
     */
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            SimpleRabbitListenerContainerFactoryConfigurer configurer,
            TenantAwareMessagePostProcessor tenantAwareMessagePostProcessor) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        // Aplica as propriedades spring.rabbitmq.listener.simple.* (inclui
        // auto-startup, usado para desligar os listeners nos testes).
        configurer.configure(factory, connectionFactory);
        factory.setDefaultRequeueRejected(false);
        factory.setPrefetchCount(10);
        factory.setAdviceChain(retryAdvice());
        factory.setAfterReceivePostProcessors(tenantAwareMessagePostProcessor);
        return factory;
    }

    private Advice retryAdvice() {
        RetryTemplate retryTemplate = RetryTemplate.builder()
                .maxAttempts(3)
                .exponentialBackoff(1000, 2, 10000)
                .build();
        return RetryInterceptorBuilder.stateless()
                .retryOperations(retryTemplate)
                .recoverer(new RejectAndDontRequeueRecoverer())
                .build();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(new SimpleMessageConverter());
        return template;
    }
}
