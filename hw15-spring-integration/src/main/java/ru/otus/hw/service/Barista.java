package ru.otus.hw.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import ru.otus.hw.domain.Drink;
import ru.otus.hw.domain.OrderItem;

@Slf4j
@Service
public class Barista {

    public Drink prepareHot(OrderItem item) {
        log.info("Готовлю горячий {} ({} shots)", item.getType(), item.getShots());
        return new Drink(item.getType(), false, item.getShots(), "hot-barista");
    }

    public Drink prepareCold(OrderItem item) {
        log.info("Готовлю холодный {} ({} shots)", item.getType(), item.getShots());
        return new Drink(item.getType(), true, item.getShots(), "cold-barista");
    }
}
