package ru.otus.hw.gateway;

import org.springframework.integration.annotation.Gateway;
import org.springframework.integration.annotation.MessagingGateway;

import ru.otus.hw.domain.Delivery;
import ru.otus.hw.domain.Order;

@MessagingGateway
public interface CafeGateway {

    @Gateway(requestChannel = "orders.input")
    Delivery placeOrder(Order order);
}
