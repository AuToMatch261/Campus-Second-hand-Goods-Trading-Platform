package com.campus.message.config;

import com.campus.common.mq.RabbitMqConstants;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {

    @Bean
    public MessageConverter jacksonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory cf, MessageConverter mc) {
        SimpleRabbitListenerContainerFactory f = new SimpleRabbitListenerContainerFactory();
        f.setConnectionFactory(cf);
        f.setMessageConverter(mc);
        f.setDefaultRequeueRejected(false);
        return f;
    }

    @Bean
    public TopicExchange orderEventsExchange() {
        return new TopicExchange(RabbitMqConstants.ORDER_EVENTS_EXCHANGE, true, false);
    }

    @Bean
    public DirectExchange dlxExchange() {
        return new DirectExchange(RabbitMqConstants.DLX_EXCHANGE, true, false);
    }

    @Bean
    public Queue messageOrderEventsDlq() {
        return QueueBuilder.durable(RabbitMqConstants.DLQ_MESSAGE_ORDER_EVENTS).build();
    }

    @Bean
    public Binding messageOrderEventsDlqBinding() {
        return BindingBuilder.bind(messageOrderEventsDlq()).to(dlxExchange())
                .with(RabbitMqConstants.DLQ_MESSAGE_ORDER_EVENTS);
    }

    @Bean
    public Queue messageOrderEventsQueue() {
        return QueueBuilder.durable(RabbitMqConstants.QUEUE_MESSAGE_ORDER_EVENTS)
                .withArgument("x-dead-letter-exchange", RabbitMqConstants.DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", RabbitMqConstants.DLQ_MESSAGE_ORDER_EVENTS)
                .build();
    }

    @Bean
    public Binding messageOrderEventsBinding() {
        return BindingBuilder.bind(messageOrderEventsQueue()).to(orderEventsExchange())
                .with(RabbitMqConstants.RK_ORDER_ALL);
    }
}
