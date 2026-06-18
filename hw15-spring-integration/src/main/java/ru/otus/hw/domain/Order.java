package ru.otus.hw.domain;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Order {

    private long id;

    private List<OrderItem> items;
}
