package ru.otus.hw.integration;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import ru.otus.hw.domain.Delivery;
import ru.otus.hw.domain.Drink;
import ru.otus.hw.domain.DrinkType;
import ru.otus.hw.domain.Order;
import ru.otus.hw.domain.OrderItem;
import ru.otus.hw.gateway.CafeGateway;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class CafeFlowTest {

    @Autowired
    private CafeGateway cafeGateway;

    @Test
    void shouldPrepareAndDeliverAllDrinksOfOrder() {
        Order order = new Order(42L, List.of(
                new OrderItem(DrinkType.LATTE, false, 2),
                new OrderItem(DrinkType.MOCHA, true, 1),
                new OrderItem(DrinkType.ESPRESSO, false, 1)));

        Delivery delivery = cafeGateway.placeOrder(order);

        assertThat(delivery).isNotNull();
        assertThat(delivery.getOrderId()).isEqualTo(42L);
        assertThat(delivery.getDrinks()).hasSize(3);
        assertThat(delivery.getDrinks())
                .extracting(Drink::getType)
                .containsExactlyInAnyOrder(DrinkType.LATTE, DrinkType.MOCHA, DrinkType.ESPRESSO);
        assertThat(delivery.getDrinks())
                .filteredOn(Drink::isIced)
                .hasSize(1);
    }

    @Test
    void shouldRouteHotAndColdDrinksToCorrectBaristas() {
        Order order = new Order(7L, List.of(
                new OrderItem(DrinkType.CAPPUCCINO, false, 1),
                new OrderItem(DrinkType.AMERICANO, true, 1)));

        Delivery delivery = cafeGateway.placeOrder(order);

        assertThat(delivery.getDrinks())
                .filteredOn(Drink::isIced)
                .extracting(Drink::getPreparedBy)
                .containsOnly("cold-barista");
        assertThat(delivery.getDrinks())
                .filteredOn(drink -> !drink.isIced())
                .extracting(Drink::getPreparedBy)
                .containsOnly("hot-barista");
    }
}
