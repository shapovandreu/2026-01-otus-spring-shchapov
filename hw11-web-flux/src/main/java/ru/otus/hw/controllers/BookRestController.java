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
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.otus.hw.dto.BookDto;
import ru.otus.hw.dto.BookFormDto;
import ru.otus.hw.dto.CommentDto;
import ru.otus.hw.exceptions.EntityNotFoundException;
import ru.otus.hw.services.BookService;
import ru.otus.hw.services.CommentService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/books")
public class BookRestController {

    private final BookService bookService;

    private final CommentService commentService;

    @GetMapping
    public Flux<BookDto> listBooks() {
        return bookService.findAll().map(BookDto::fromDomain);
    }

    @GetMapping("/{id}")
    public Mono<BookDto> getBook(@PathVariable String id) {
        return bookService.findById(id)
                .map(BookDto::fromDomain)
                .switchIfEmpty(Mono.error(
                        new EntityNotFoundException("Book with id %s not found".formatted(id))));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<BookDto> createBook(@Valid @RequestBody BookFormDto form) {
        return bookService.insert(form.getTitle(), form.getAuthorId(), form.getGenreIds())
                .map(BookDto::fromDomain);
    }

    @PutMapping("/{id}")
    public Mono<BookDto> updateBook(@PathVariable String id, @Valid @RequestBody BookFormDto form) {
        return bookService.update(id, form.getTitle(), form.getAuthorId(), form.getGenreIds())
                .map(BookDto::fromDomain);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> deleteBook(@PathVariable String id) {
        return bookService.deleteById(id);
    }

    @GetMapping("/{id}/comments")
    public Flux<CommentDto> getComments(@PathVariable String id) {
        return commentService.findByBookId(id).map(CommentDto::fromDomain);
    }
}
