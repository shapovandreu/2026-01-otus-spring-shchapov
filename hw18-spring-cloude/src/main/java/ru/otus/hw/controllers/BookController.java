package ru.otus.hw.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/books")
public class BookController {

    @GetMapping
    public String listBooks() {
        return "books/list";
    }

    @GetMapping("/new")
    public String newBookForm() {
        return "books/edit";
    }

    @GetMapping("/{id}")
    public String viewBook() {
        return "books/view";
    }

    @GetMapping("/{id}/edit")
    public String editBookForm() {
        return "books/edit";
    }
}
