package ru.otus.hw.runner;

import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import ru.otus.hw.domain.Delivery;
import ru.otus.hw.domain.DrinkType;
import ru.otus.hw.domain.Order;
import ru.otus.hw.domain.OrderItem;
import ru.otus.hw.gateway.CafeGateway;

@Slf4j
@Component
@Profile("!test")
@RequiredArgsConstructor
public class CafeDemoRunner implements CommandLineRunner {

    private final CafeGateway cafeGateway;

    @Override
    public void run(String... args) {
        Order order = new Order(1L, List.of(
                new OrderItem(DrinkType.LATTE, false, 2),
                new OrderItem(DrinkType.ESPRESSO, false, 1),
                new OrderItem(DrinkType.MOCHA, true, 1)));

        log.info("Размещаю заказ #{}: {}", order.getId(), order.getItems());

        Delivery delivery = cafeGateway.placeOrder(order);

        log.info("Заказ #{} собран, напитков: {}", delivery.getOrderId(), delivery.getDrinks().size());
        delivery.getDrinks().forEach(drink -> log.info("  готово: {}", drink));
    }
}
