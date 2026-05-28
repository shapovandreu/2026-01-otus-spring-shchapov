package ru.otus.hw.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import ru.otus.hw.dto.AuthorDto;
import ru.otus.hw.services.AuthorService;

@Controller
@RequiredArgsConstructor
@RequestMapping("/authors")
public class AuthorController {

    private final AuthorService authorService;

    @GetMapping
    public String listAuthors(Model model) {
        model.addAttribute("authors", authorService.findAll().map(AuthorDto::fromDomain));
        return "authors/list";
    }
}
