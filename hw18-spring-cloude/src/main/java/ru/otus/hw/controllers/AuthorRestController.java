package ru.otus.hw.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.otus.hw.dto.AuthorDto;
import ru.otus.hw.services.AuthorService;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/authors")
public class AuthorRestController {

    private final AuthorService authorService;

    @GetMapping
    public List<AuthorDto> listAuthors() {
        return authorService.findAll().stream()
                .map(AuthorDto::fromDomain)
                .toList();
    }
}
