package fr.frogdevelopment.jenkins.plugins.mq;

import com.rabbitmq.client.ConnectionFactory;

import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

class RabbitMqFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(RabbitMqFactory.class);

    static ConnectionFactory mockConnectionFactory; // keep it, for test use
    static RabbitTemplate mockRabbitTemplate;

    static ConnectionFactory createConnectionFactory(String username, String password, String host, int port) {
        if (mockConnectionFactory == null) {
            LOGGER.info("Mocking ConnectionFactory");
            mockConnectionFactory = Mockito.mock(ConnectionFactory.class);
        } else {
            LOGGER.info("Re-using mocked ConnectionFactory");
        }

        return mockConnectionFactory;
    }

    static RabbitTemplate getRabbitTemplate(RabbitMqBuilder.RabbitConfig rabbitConfig) {
        if (mockRabbitTemplate == null) {
            LOGGER.info("Mocking RabbitTemplate");
            mockRabbitTemplate = Mockito.mock(RabbitTemplate.class);
        } else {
            LOGGER.info("Re-using mocked RabbitTemplate");
        }

        return mockRabbitTemplate;
    }
}
