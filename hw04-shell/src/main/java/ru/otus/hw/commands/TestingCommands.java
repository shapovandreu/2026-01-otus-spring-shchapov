package ru.otus.hw.commands;

import lombok.AllArgsConstructor;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.stereotype.Component;
import ru.otus.hw.service.TestRunnerService;

@Component
@ShellComponent
@AllArgsConstructor
public class TestingCommands {

    private final TestRunnerService testRunnerService;

    @ShellMethod(
            key = {"r", "run"},
            value = "Run test"
    )
    public void run() {
        testRunnerService.run();
    }

}