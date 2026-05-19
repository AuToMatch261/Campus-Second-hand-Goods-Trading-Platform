package com.campus.product.config;

import com.campus.common.mq.RabbitMqConstants;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
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
    public RabbitTemplate rabbitTemplate(ConnectionFactory cf, MessageConverter mc) {
        RabbitTemplate t = new RabbitTemplate(cf);
        t.setMessageConverter(mc);
        return t;
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
    public TopicExchange productEventsExchange() {
        return new TopicExchange(RabbitMqConstants.PRODUCT_EVENTS_EXCHANGE, true, false);
    }

    @Bean
    public DirectExchange dlxExchange() {
        return new DirectExchange(RabbitMqConstants.DLX_EXCHANGE, true, false);
    }

    // ---------- order.cancelled 队列 ----------

    @Bean
    public Queue orderCancelledDlq() {
        return QueueBuilder.durable(RabbitMqConstants.DLQ_PRODUCT_ORDER_CANCELLED).build();
    }

    @Bean
    public Binding orderCancelledDlqBinding() {
        return BindingBuilder.bind(orderCancelledDlq()).to(dlxExchange())
                .with(RabbitMqConstants.DLQ_PRODUCT_ORDER_CANCELLED);
    }

    @Bean
    public Queue orderCancelledQueue() {
        return QueueBuilder.durable(RabbitMqConstants.QUEUE_PRODUCT_ORDER_CANCELLED)
                .withArgument("x-dead-letter-exchange", RabbitMqConstants.DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", RabbitMqConstants.DLQ_PRODUCT_ORDER_CANCELLED)
                .build();
    }

    @Bean
    public Binding orderCancelledBinding() {
        return BindingBuilder.bind(orderCancelledQueue()).to(orderEventsExchange())
                .with(RabbitMqConstants.RK_ORDER_CANCELLED);
    }

    // ---------- product 索引同步队列（自产自消） ----------

    @Bean
    public Queue searchProductEventsDlq() {
        return QueueBuilder.durable(RabbitMqConstants.DLQ_SEARCH_PRODUCT_EVENTS).build();
    }

    @Bean
    public Binding searchProductEventsDlqBinding() {
        return BindingBuilder.bind(searchProductEventsDlq()).to(dlxExchange())
                .with(RabbitMqConstants.DLQ_SEARCH_PRODUCT_EVENTS);
    }

    @Bean
    public Queue searchProductEventsQueue() {
        return QueueBuilder.durable(RabbitMqConstants.QUEUE_SEARCH_PRODUCT_EVENTS)
                .withArgument("x-dead-letter-exchange", RabbitMqConstants.DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", RabbitMqConstants.DLQ_SEARCH_PRODUCT_EVENTS)
                .build();
    }

    @Bean
    public Binding searchProductEventsBinding() {
        return BindingBuilder.bind(searchProductEventsQueue()).to(productEventsExchange())
                .with(RabbitMqConstants.RK_PRODUCT_ALL);
    }
}
