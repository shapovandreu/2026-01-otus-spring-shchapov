package ru.otus.hw.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.otus.hw.fault.FaultInjector;

import java.util.Map;

/**
 * Управление эмуляцией сбоя БД для демонстрации работы circuit breaker'а.
 * POST /api/fault/enable  — включить сбой (вызовы к БД начнут падать);
 * POST /api/fault/disable — выключить сбой (БД снова доступна).
 */
@RestController
@RequestMapping("/api/fault")
@RequiredArgsConstructor
public class FaultController {

    private final FaultInjector faultInjector;

    @PostMapping("/enable")
    public Map<String, Boolean> enable() {
        faultInjector.enable();
        return Map.of("failing", faultInjector.isFailing());
    }

    @PostMapping("/disable")
    public Map<String, Boolean> disable() {
        faultInjector.disable();
        return Map.of("failing", faultInjector.isFailing());
    }
}
