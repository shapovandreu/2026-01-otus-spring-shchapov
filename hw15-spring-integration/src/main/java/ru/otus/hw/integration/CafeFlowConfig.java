package ru.otus.hw.integration;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.Pollers;
import org.springframework.integration.handler.LoggingHandler;
import org.springframework.messaging.MessageChannel;

import ru.otus.hw.domain.Delivery;
import ru.otus.hw.domain.Drink;
import ru.otus.hw.domain.Order;
import ru.otus.hw.domain.OrderItem;
import ru.otus.hw.service.Barista;
import ru.otus.hw.service.Waiter;

@Configuration
public class CafeFlowConfig {

    private static final String ORDER_ID_HEADER = "orderId";

    private static final int QUEUE_CAPACITY = 20;

    private static final long POLL_INTERVAL_MS = 100;

    @Bean
    public MessageChannel ordersInput() {
        return new DirectChannel();
    }

    @Bean
    public MessageChannel invalidOrders() {
        return new DirectChannel();
    }

    @Bean
    public MessageChannel hotDrinks() {
        return new QueueChannel(QUEUE_CAPACITY);
    }

    @Bean
    public MessageChannel coldDrinks() {
        return new QueueChannel(QUEUE_CAPACITY);
    }

    @Bean
    public MessageChannel preparedDrinks() {
        return new DirectChannel();
    }

    /**
     * Основной поток: приём заказа -> фильтр пустых заказов -> разбиение на позиции -> маршрутизация
     * по признаку "горячий/холодный".
     */
    @Bean
    public IntegrationFlow orderFlow() {
        return IntegrationFlow.from(ordersInput())
                .filter(Order.class, order -> !order.getItems().isEmpty(),
                        filter -> filter.discardChannel(invalidOrders()))
                .enrichHeaders(headers -> headers.headerExpression(ORDER_ID_HEADER, "payload.id"))
                .split(Order.class, Order::getItems)
                .<OrderItem, String>route(item -> item.isIced() ? "iced" : "hot",
                        mapping -> mapping
                                .channelMapping("iced", "coldDrinks")
                                .channelMapping("hot", "hotDrinks"))
                .get();
    }

    /**
     * Subflow приготовления горячих напитков: опрашивает очередь, готовит напиток и отдаёт его на сборку.
     */
    @Bean
    public IntegrationFlow hotBaristaFlow(Barista barista) {
        return IntegrationFlow.from(hotDrinks())
                .handle(barista, "prepareHot",
                        endpoint -> endpoint.poller(
                                Pollers.fixedDelay(Duration.ofMillis(POLL_INTERVAL_MS)).maxMessagesPerPoll(1)))
                .channel(preparedDrinks())
                .get();
    }

    /**
     * Subflow приготовления холодных напитков.
     */
    @Bean
    public IntegrationFlow coldBaristaFlow(Barista barista) {
        return IntegrationFlow.from(coldDrinks())
                .handle(barista, "prepareCold",
                        endpoint -> endpoint.poller(
                                Pollers.fixedDelay(Duration.ofMillis(POLL_INTERVAL_MS)).maxMessagesPerPoll(1)))
                .channel(preparedDrinks())
                .get();
    }

    /**
     * Поток сборки и выдачи: агрегирует готовые напитки одного заказа в доставку и передаёт официанту.
     */
    @Bean
    public IntegrationFlow deliveryFlow(Waiter waiter) {
        return IntegrationFlow.from(preparedDrinks())
                .aggregate(aggregator -> aggregator.outputProcessor(group -> {
                    List<Drink> drinks = group.getMessages().stream()
                            .map(message -> (Drink) message.getPayload())
                            .collect(Collectors.toList());
                    Long orderId = group.getMessages().stream()
                            .findFirst()
                            .map(msg -> msg.getHeaders().get(ORDER_ID_HEADER, Long.class))
                            .orElse(null);
                    return new Delivery(orderId, drinks);
                }))
                .handle(waiter, "deliver")
                .get();
    }

    /**
     * Обработка отбракованных заказов (без позиций).
     */
    @Bean
    public IntegrationFlow invalidOrdersFlow() {
        return IntegrationFlow.from(invalidOrders())
                .log(LoggingHandler.Level.WARN, "ru.otus.hw.cafe",
                        message -> "Заказ без позиций отклонён: " + message.getPayload())
                .nullChannel();
    }
}
