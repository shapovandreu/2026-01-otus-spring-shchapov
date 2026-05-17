package ru.otus.hw.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import ru.otus.hw.dto.BookDto;
import ru.otus.hw.dto.BookFormDto;
import ru.otus.hw.dto.CommentDto;
import ru.otus.hw.exceptions.EntityNotFoundException;
import ru.otus.hw.services.AuthorService;
import ru.otus.hw.services.BookService;
import ru.otus.hw.services.CommentService;
import ru.otus.hw.services.GenreService;

import java.util.List;

@Controller
@RequiredArgsConstructor
@RequestMapping("/books")
public class BookController {

    private final BookService bookService;

    private final AuthorService authorService;

    private final GenreService genreService;

    private final CommentService commentService;

    @GetMapping
    public String listBooks(Model model) {
        List<BookDto> books = bookService.findAll().stream()
                .map(BookDto::fromDomain)
                .toList();
        model.addAttribute("books", books);
        return "books/list";
    }

    @GetMapping("/{id}")
    public String viewBook(@PathVariable long id, Model model) {
        var book = bookService.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Book with id %d not found".formatted(id)));
        var comments = commentService.findByBookId(id).stream()
                .map(CommentDto::fromDomain)
                .toList();
        model.addAttribute("book", BookDto.fromDomain(book));
        model.addAttribute("comments", comments);
        return "books/view";
    }

    @GetMapping("/new")
    public String newBookForm(Model model) {
        model.addAttribute("book", new BookFormDto());
        populateAuthorsAndGenres(model);
        return "books/edit";
    }

    @PostMapping
    public String createBook(@Valid @ModelAttribute("book") BookFormDto form,
                             BindingResult bindingResult,
                             Model model) {
        if (bindingResult.hasErrors()) {
            populateAuthorsAndGenres(model);
            return "books/edit";
        }
        bookService.insert(form.getTitle(), form.getAuthorId(), form.getGenreIds());
        return "redirect:/books";
    }

    @GetMapping("/{id}/edit")
    public String editBookForm(@PathVariable long id, Model model) {
        var book = bookService.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Book with id %d not found".formatted(id)));
        model.addAttribute("book", BookFormDto.fromDomain(book));
        populateAuthorsAndGenres(model);
        return "books/edit";
    }

    @PostMapping("/{id}/edit")
    public String updateBook(@PathVariable long id,
                             @Valid @ModelAttribute("book") BookFormDto form,
                             BindingResult bindingResult,
                             Model model) {
        if (bindingResult.hasErrors()) {
            form.setId(id);
            populateAuthorsAndGenres(model);
            return "books/edit";
        }
        bookService.update(id, form.getTitle(), form.getAuthorId(), form.getGenreIds());
        return "redirect:/books/" + id;
    }

    @PostMapping("/{id}/delete")
    public String deleteBook(@PathVariable long id) {
        bookService.deleteById(id);
        return "redirect:/books";
    }

    private void populateAuthorsAndGenres(Model model) {
        var authors = authorService.findAll().stream()
                .map(ru.otus.hw.dto.AuthorDto::fromDomain)
                .toList();
        var genres = genreService.findAll().stream()
                .map(ru.otus.hw.dto.GenreDto::fromDomain)
                .toList();
        model.addAttribute("authors", authors);
        model.addAttribute("genres", genres);
    }
}
