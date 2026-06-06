package com.texttolearn.generation.config;

import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableRabbit
public class GenerationRabbitMqConfig {

    public static final String GENERATION_EXCHANGE = "text-to-learn.generation.exchange";
    public static final String COURSE_GENERATION_QUEUE = "course.generation.queue";
    public static final String LESSON_GENERATION_HIGH_QUEUE = "lesson.generation.high.queue";
    public static final String LESSON_GENERATION_LOW_QUEUE = "lesson.generation.low.queue";
    public static final String COURSE_GENERATION_ROUTING_KEY = "generation.course";
    public static final String LESSON_GENERATION_HIGH_ROUTING_KEY = "generation.lesson.high";
    public static final String LESSON_GENERATION_LOW_ROUTING_KEY = "generation.lesson.low";

    @Bean
    DirectExchange generationExchange() {
        return ExchangeBuilder.directExchange(GENERATION_EXCHANGE)
                .durable(true)
                .build();
    }

    @Bean
    Queue courseGenerationQueue() {
        return QueueBuilder.durable(COURSE_GENERATION_QUEUE).build();
    }

    @Bean
    Queue lessonGenerationHighQueue() {
        return QueueBuilder.durable(LESSON_GENERATION_HIGH_QUEUE).build();
    }

    @Bean
    Queue lessonGenerationLowQueue() {
        return QueueBuilder.durable(LESSON_GENERATION_LOW_QUEUE).build();
    }

    @Bean
    Binding courseGenerationBinding(Queue courseGenerationQueue, DirectExchange generationExchange) {
        return BindingBuilder.bind(courseGenerationQueue)
                .to(generationExchange)
                .with(COURSE_GENERATION_ROUTING_KEY);
    }

    @Bean
    Binding lessonGenerationHighBinding(Queue lessonGenerationHighQueue, DirectExchange generationExchange) {
        return BindingBuilder.bind(lessonGenerationHighQueue)
                .to(generationExchange)
                .with(LESSON_GENERATION_HIGH_ROUTING_KEY);
    }

    @Bean
    Binding lessonGenerationLowBinding(Queue lessonGenerationLowQueue, DirectExchange generationExchange) {
        return BindingBuilder.bind(lessonGenerationLowQueue)
                .to(generationExchange)
                .with(LESSON_GENERATION_LOW_ROUTING_KEY);
    }

    @Bean
    RabbitAdmin generationRabbitAdmin(ConnectionFactory connectionFactory) {
        RabbitAdmin rabbitAdmin = new RabbitAdmin(connectionFactory);
        rabbitAdmin.setAutoStartup(true);
        return rabbitAdmin;
    }

    @Bean
    @ConditionalOnProperty(name = "app.rabbitmq.declare-on-startup", havingValue = "true", matchIfMissing = true)
    ApplicationRunner declareGenerationRabbitInfrastructure(RabbitAdmin generationRabbitAdmin) {
        return args -> generationRabbitAdmin.initialize();
    }

    @Bean
    MessageConverter generationMessageConverter() {
        return new JacksonJsonMessageConverter();
    }

    @Bean
    RabbitTemplate generationRabbitTemplate(
            ConnectionFactory connectionFactory,
            MessageConverter generationMessageConverter
    ) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(generationMessageConverter);
        rabbitTemplate.setBeforePublishPostProcessors(message -> {
            message.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
            return message;
        });
        return rabbitTemplate;
    }

    @Bean
    SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            MessageConverter generationMessageConverter
    ) {
        return listenerContainerFactory(connectionFactory, generationMessageConverter, 1);
    }

    @Bean
    SimpleRabbitListenerContainerFactory highPriorityLessonRabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            MessageConverter generationMessageConverter
    ) {
        return listenerContainerFactory(connectionFactory, generationMessageConverter, 2);
    }

    @Bean
    SimpleRabbitListenerContainerFactory lowPriorityLessonRabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            MessageConverter generationMessageConverter
    ) {
        return listenerContainerFactory(connectionFactory, generationMessageConverter, 1);
    }

    private SimpleRabbitListenerContainerFactory listenerContainerFactory(
            ConnectionFactory connectionFactory,
            MessageConverter generationMessageConverter,
            int concurrency
    ) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(generationMessageConverter);
        factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        factory.setDefaultRequeueRejected(false);
        factory.setPrefetchCount(1);
        factory.setConcurrentConsumers(concurrency);
        factory.setMaxConcurrentConsumers(concurrency);
        return factory;
    }
}
