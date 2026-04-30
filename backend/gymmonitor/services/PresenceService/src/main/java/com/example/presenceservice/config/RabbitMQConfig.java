package com.example.presenceservice.config;

import com.example.presenceservice.dto.AccessEvent;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.DefaultClassMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
public class RabbitMQConfig {

    @Bean
    public TopicExchange accessExchange() {
        return new TopicExchange("gymmonitor.access", true, false);
    }

    @Bean
    public Queue presenceQueue() {
        return new Queue("gymmonitor.presence", true);
    }

    @Bean
    public Binding presenceBinding(Queue presenceQueue, TopicExchange accessExchange) {
        return BindingBuilder.bind(presenceQueue).to(accessExchange).with("access.*");
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter();
        DefaultClassMapper classMapper = new DefaultClassMapper();
        // Trust all packages — internal system, messages only come from AccessControl or tests
        classMapper.setTrustedPackages("*");
        // Maps the type published by AccessControl to our local AccessEvent mirror
        classMapper.setIdClassMapping(Map.of(
                "com.example.accesscontrol.dto.AccessEvent", AccessEvent.class
        ));
        converter.setClassMapper(classMapper);
        return converter;
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                         MessageConverter messageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter);
        return template;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory, MessageConverter messageConverter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(messageConverter);
        return factory;
    }
}
