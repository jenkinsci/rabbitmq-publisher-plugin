package fr.frogdevelopment.jenkins.plugins.mq;

import com.rabbitmq.client.ConnectionFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;

class RabbitMqFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(RabbitMqFactory.class);

    static RabbitTemplate mock; // keep it, for test use

    static RabbitTemplate getRabbitTemplate(RabbitMqBuilder.RabbitConfig rabbitConfig) {

        LOGGER.info("Initialisation Rabbit-MQ :\n\t-Host : {}\n\t-Port : {}\n\t-User : {}", rabbitConfig.getHost(), rabbitConfig.getPort(), rabbitConfig.getUsername());

        //
        ConnectionFactory connectionFactory = new ConnectionFactory();
        connectionFactory.setAutomaticRecoveryEnabled(false);
        connectionFactory.setUsername(rabbitConfig.getUsername());
        connectionFactory.setPassword(rabbitConfig.getPassword());
        connectionFactory.setHost(rabbitConfig.getHost());
        connectionFactory.setPort(rabbitConfig.getPort());

        //
        RabbitTemplate rabbitTemplate = new RabbitTemplate(new CachingConnectionFactory(connectionFactory));
        rabbitTemplate.setMessageConverter(new Jackson2JsonMessageConverter());

        return rabbitTemplate;
    }
}
