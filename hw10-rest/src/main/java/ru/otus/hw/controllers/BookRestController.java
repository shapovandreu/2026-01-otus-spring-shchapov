package ru.otus.hw.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.otus.hw.dto.BookDto;
import ru.otus.hw.dto.BookFormDto;
import ru.otus.hw.dto.CommentDto;
import ru.otus.hw.exceptions.EntityNotFoundException;
import ru.otus.hw.services.BookService;
import ru.otus.hw.services.CommentService;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/books")
public class BookRestController {

    private final BookService bookService;

    private final CommentService commentService;

    @GetMapping
    public List<BookDto> listBooks() {
        return bookService.findAll().stream()
                .map(BookDto::fromDomain)
                .toList();
    }

    @GetMapping("/{id}")
    public BookDto getBook(@PathVariable long id) {
        return bookService.findById(id)
                .map(BookDto::fromDomain)
                .orElseThrow(() -> new EntityNotFoundException("Book with id %d not found".formatted(id)));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BookDto createBook(@Valid @RequestBody BookFormDto form) {
        return BookDto.fromDomain(bookService.insert(form.getTitle(), form.getAuthorId(), form.getGenreIds()));
    }

    @PutMapping("/{id}")
    public BookDto updateBook(@PathVariable long id, @Valid @RequestBody BookFormDto form) {
        return BookDto.fromDomain(bookService.update(id, form.getTitle(), form.getAuthorId(), form.getGenreIds()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteBook(@PathVariable long id) {
        bookService.deleteById(id);
    }

    @GetMapping("/{id}/comments")
    public List<CommentDto> getComments(@PathVariable long id) {
        return commentService.findByBookId(id).stream()
                .map(CommentDto::fromDomain)
                .toList();
    }
}
