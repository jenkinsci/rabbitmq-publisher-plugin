package fr.frogdevelopment.jenkins.plugins.mq;

import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

class RabbitMqFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(RabbitMqFactory.class);

    static RabbitTemplate getRabbitTemplate(RabbitMqBuilder.RabbitConfig rabbitConfig) {
        LOGGER.info("Mocking RabbitTemplate");
        return Mockito.mock(RabbitTemplate.class);
    }
}
