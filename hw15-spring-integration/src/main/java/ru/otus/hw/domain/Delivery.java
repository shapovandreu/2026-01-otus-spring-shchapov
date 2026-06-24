package ru.otus.hw.domain;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Delivery {

    private long orderId;

    private List<Drink> drinks;
}
