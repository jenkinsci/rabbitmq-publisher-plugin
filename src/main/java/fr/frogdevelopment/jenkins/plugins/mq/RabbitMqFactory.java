package fr.frogdevelopment.jenkins.plugins.mq;

import com.rabbitmq.client.ConnectionFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;

class RabbitMqFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(RabbitMqFactory.class);

    static ConnectionFactory mockConnectionFactory; // keep it, for test use
    static RabbitTemplate mockRabbitTemplate; // keep it, for test use

    static ConnectionFactory createConnectionFactory(String username, String password, String host, int port) {

        LOGGER.info("Initialisation Rabbit-MQ :\n\t-Host : {}\n\t-Port : {}\n\t-User : {}", host, port, username);

        //
        ConnectionFactory connectionFactory = new ConnectionFactory();
        connectionFactory.setAutomaticRecoveryEnabled(false);
        connectionFactory.setHost(host);
        connectionFactory.setPort(port);
        connectionFactory.setUsername(username);
        connectionFactory.setPassword(password);

        return connectionFactory;
    }

    static RabbitTemplate getRabbitTemplate(RabbitMqBuilder.RabbitConfig rabbitConfig) {
        ConnectionFactory connectionFactory = createConnectionFactory(
                rabbitConfig.getUsername(),
                rabbitConfig.getPassword(),
                rabbitConfig.getHost(),
                rabbitConfig.getPort()
        );

        //
        RabbitTemplate rabbitTemplate = new RabbitTemplate(new CachingConnectionFactory(connectionFactory));
        rabbitTemplate.setMessageConverter(new Jackson2JsonMessageConverter());

        return rabbitTemplate;
    }
}
