package ru.otus.hw.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import ru.otus.hw.dto.GenreDto;
import ru.otus.hw.services.GenreService;

@Controller
@RequiredArgsConstructor
@RequestMapping("/genres")
public class GenreController {

    private final GenreService genreService;

    @GetMapping
    public String listGenres(Model model) {
        model.addAttribute("genres", genreService.findAll().map(GenreDto::fromDomain));
        return "genres/list";
    }
}
