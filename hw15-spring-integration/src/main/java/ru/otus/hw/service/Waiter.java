package ru.otus.hw.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import ru.otus.hw.domain.Delivery;

@Slf4j
@Service
public class Waiter {

    public Delivery deliver(Delivery delivery) {
        log.info("Выдаю заказ #{}: {} напитков", delivery.getOrderId(), delivery.getDrinks().size());
        return delivery;
    }
}
