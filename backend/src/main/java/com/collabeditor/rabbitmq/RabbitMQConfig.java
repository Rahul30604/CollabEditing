package com.collabeditor.rabbitmq;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // Exchange
    public static final String EXCHANGE = "collabeditor.exchange";

    // Queues
    public static final String DOCUMENT_UPDATES_QUEUE = "document-updates";
    public static final String NOTIFICATIONS_QUEUE = "notifications";
    public static final String EMBEDDING_QUEUE = "embedding-generation";

    // Routing keys
    public static final String DOCUMENT_UPDATED_KEY = "document.updated";
    public static final String DOCUMENT_SAVED_KEY = "document.saved";
    public static final String NOTIFICATION_KEY = "notification.send";
    public static final String EMBEDDING_KEY = "embedding.generate";

    @Bean
    public TopicExchange exchange() {
        return new TopicExchange(EXCHANGE);
    }

    @Bean
    public Queue documentUpdatesQueue() {
        return QueueBuilder.durable(DOCUMENT_UPDATES_QUEUE).build();
    }

    @Bean
    public Queue notificationsQueue() {
        return QueueBuilder.durable(NOTIFICATIONS_QUEUE).build();
    }

    @Bean
    public Queue embeddingQueue() {
        return QueueBuilder.durable(EMBEDDING_QUEUE).build();
    }

    @Bean
    public Binding documentUpdatesBinding(Queue documentUpdatesQueue, TopicExchange exchange) {
        return BindingBuilder.bind(documentUpdatesQueue).to(exchange).with("document.*");
    }

    @Bean
    public Binding notificationsBinding(Queue notificationsQueue, TopicExchange exchange) {
        return BindingBuilder.bind(notificationsQueue).to(exchange).with("notification.*");
    }

    @Bean
    public Binding embeddingBinding(Queue embeddingQueue, TopicExchange exchange) {
        return BindingBuilder.bind(embeddingQueue).to(exchange).with("embedding.*");
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter jsonMessageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter);
        return template;
    }
}
