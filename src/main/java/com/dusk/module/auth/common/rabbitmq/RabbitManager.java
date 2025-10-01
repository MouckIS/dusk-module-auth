package com.dusk.module.auth.common.rabbitmq;

import com.dusk.common.mqs.rabbitmq.RabbitMQEnableCondition;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
@Conditional(RabbitMQEnableCondition.class)
public class RabbitManager implements ApplicationRunner {
    @Resource
    AmqpAdmin amqpAdmin;

    @Override
    public void run(ApplicationArguments args) {
        amqpAdmin.declareExchange(new FanoutExchange(RabbitConstant.CANCEL_USER_AUTH_FANOUT_EXCHANGE_V1));
    }
}
