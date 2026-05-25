package br.com.busco.planejamento.infra.rabbit;


import br.com.busco.planejamento.sk.ddd.TenantAwareMessagePostProcessor;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.DefaultJackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Value("${viagem-exchange}")
    private String viagemExchange;

    @Value("${presenca-queue}")
    private String presencaQueue;

    @Bean
    public TopicExchange viagemExchange() {
        return new TopicExchange(viagemExchange);
    }

    @Bean
    public Queue presencaQueue() {
        return QueueBuilder.durable(presencaQueue)
                .build();
    }

    @Bean
    public Binding presencaBindingCriada() {
        return BindingBuilder
                .bind(presencaQueue())
                .to(viagemExchange())
                .with("viagem.criada");
    }

    @Bean
    public Binding presencaBindingFinalizada() {
        return BindingBuilder
                .bind(presencaQueue())
                .to(viagemExchange())
                .with("viagem.finalizada");
    }

    @Bean
    public Jackson2JsonMessageConverter jsonMessageConverter() {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter();

        DefaultJackson2JavaTypeMapper mapper = new DefaultJackson2JavaTypeMapper();
        mapper.setTrustedPackages("*");

        converter.setJavaTypeMapper(mapper);
        return converter;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            EventTypeAwareMessageConverter converter,
            TenantAwareMessagePostProcessor tenantProcessor

    ) {

        SimpleRabbitListenerContainerFactory factory =
                new SimpleRabbitListenerContainerFactory();

        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(converter);
        factory.setAfterReceivePostProcessors(tenantProcessor);

        return factory;
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jsonMessageConverter());
        return rabbitTemplate;
    }
}