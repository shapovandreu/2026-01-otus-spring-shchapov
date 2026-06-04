package ru.otus.hw.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import ru.otus.hw.dto.GenreDto;
import ru.otus.hw.services.GenreService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/genres")
public class GenreRestController {

    private final GenreService genreService;

    @GetMapping
    public Flux<GenreDto> listGenres() {
        return genreService.findAll().map(GenreDto::fromDomain);
    }
}
