package ru.otus.hw.repositories;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import ru.otus.hw.exceptions.EntityNotFoundException;
import ru.otus.hw.models.Author;
import ru.otus.hw.models.Book;
import ru.otus.hw.models.Genre;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class JdbcBookRepository implements BookRepository {

    private final NamedParameterJdbcOperations namedParameterJdbcOperations;

    private final GenreRepository genreRepository;

    @Override
    public Optional<Book> findById(long id) {
        String sql = """
            SELECT
                b.id as book_id,
                b.title as book_title,
                a.id as author_id,
                a.full_name as author_full_name,
                g.id as genre_id,
                g.name as genre_name
            FROM books b
            LEFT JOIN authors a ON b.author_id = a.id
            LEFT JOIN books_genres bg ON b.id = bg.book_id
            LEFT JOIN genres g ON bg.genre_id = g.id
            WHERE b.id = :id
            """;

        MapSqlParameterSource mapSqlParameterSource = new MapSqlParameterSource("id", id);

        return Optional.ofNullable(namedParameterJdbcOperations.query(
                sql,
                mapSqlParameterSource,
                new BookResultSetExtractor()
        ));
    }

    @Override
    public List<Book> findAll() {
        var genres = genreRepository.findAll();
        var books = getAllBooksWithoutGenres();
        var relations = getAllGenreRelations();
        mergeBooksInfo(books, genres, relations);
        return books;
    }

    @Override
    @Transactional
    public Book save(Book book) {
        if (book.getId() == 0) {
            return insert(book);
        }
        return update(book);
    }

    @Override
    public void deleteById(long id) {
        MapSqlParameterSource mapSqlParameterSource = new MapSqlParameterSource("id", id);

        namedParameterJdbcOperations.update(
                "delete from BOOKS where id = :id", mapSqlParameterSource
        );
    }

    private List<Book> getAllBooksWithoutGenres() {
        return namedParameterJdbcOperations.query("""
                SELECT
                    b.id AS book_id,
                    b.title AS book_title,
                    a.id AS author_id,
                    a.full_name AS author_name
                FROM books b
                JOIN authors a ON b.author_id = a.id;
                """,
                new BookRowMapper()
        );
    }

    private List<BookGenreRelation> getAllGenreRelations() {
        return namedParameterJdbcOperations.query(
                "SELECT book_id, genre_id FROM books_genres",
                new BookGenreRelationRowMapper()
        );
    }

    private void mergeBooksInfo(List<Book> booksWithoutGenres, List<Genre> genres,
                                List<BookGenreRelation> relations) {
        Map<Long, Genre> genreMap = genres.stream()
                .collect(Collectors.toMap(Genre::getId, Function.identity()));

        Map<Long, List<Long>> bookGenresMap = relations.stream()
                .collect(Collectors.groupingBy(
                        BookGenreRelation::bookId,
                        Collectors.mapping(BookGenreRelation::genreId, Collectors.toList())
                ));

        for (Book book : booksWithoutGenres) {
            List<Long> genreIds = bookGenresMap.get(book.getId());
            if (genreIds != null && !genreIds.isEmpty()) {
                List<Genre> bookGenres = genreIds.stream()
                        .map(genreMap::get)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());
                book.setGenres(bookGenres);
            } else {
                book.setGenres(new ArrayList<>());
            }
        }
    }

    private Book insert(Book book) {
        var keyHolder = new GeneratedKeyHolder();

        String insertBookSql = """
            INSERT INTO books (title, author_id) 
            VALUES (:title, :author_id)
            """;

        MapSqlParameterSource mapSqlParameterSource = new MapSqlParameterSource();
        mapSqlParameterSource.addValue("title", book.getTitle());
        mapSqlParameterSource.addValue("author_id", book.getAuthor().getId());

        namedParameterJdbcOperations.update(
                insertBookSql,
                mapSqlParameterSource,
                keyHolder,
                new String[]{"id"}
        );

        //noinspection DataFlowIssue
        book.setId(keyHolder.getKeyAs(Long.class));
        batchInsertGenresRelationsFor(book);
        return book;
    }

    private Book update(Book book) {
        String updateSql = """
            UPDATE books 
            SET title = :title, author_id = :author_id 
            WHERE id = :id
            """;

        Map<String, Object> params = Map.of(
                "id", book.getId(),
                "title", book.getTitle(),
                "author_id", book.getAuthor().getId()
        );

        int rowsUpdated = namedParameterJdbcOperations.update(updateSql, params);

        if (rowsUpdated == 0) {
            throw new EntityNotFoundException("Book with id " + book.getId() + " not found");
        }

        removeGenresRelationsFor(book);
        batchInsertGenresRelationsFor(book);

        return book;
    }

    private void batchInsertGenresRelationsFor(Book book) {
        if (book.getGenres() != null && !book.getGenres().isEmpty()) {
            String insertGenresSql = """
                INSERT INTO books_genres (book_id, genre_id)
                VALUES (:book_id, :genre_id)
                """;

            List<Map<String, Long>> batchParams = book.getGenres().stream()
                    .map(genre -> Map.of(
                            "book_id", book.getId(),
                            "genre_id", genre.getId()
                    ))
                    .collect(Collectors.toList());

            namedParameterJdbcOperations.batchUpdate(insertGenresSql,
                    batchParams.toArray(new Map[0]));
        }
    }

    private void removeGenresRelationsFor(Book book) {
        if (book.getGenres() != null && !book.getGenres().isEmpty()) {
            String removeGenresSql = """
                DELETE FROM books_genres
                WHERE book_id = :book_id
                """;

            List<Map<String, Long>> batchParams = book.getGenres().stream()
                    .map(genre -> Map.of(
                            "book_id", book.getId()
                    ))
                    .collect(Collectors.toList());

            namedParameterJdbcOperations.batchUpdate(removeGenresSql,
                    batchParams.toArray(new Map[0]));
        }
    }

    private static class BookRowMapper implements RowMapper<Book> {

        @Override
        public Book mapRow(ResultSet rs, int rowNum) throws SQLException {
            Book book = new Book();
            book.setId(rs.getLong("book_id"));
            book.setTitle(rs.getString("book_title"));

            Author author = new Author();
            author.setId(rs.getLong("author_id"));
            author.setFullName(rs.getString("author_name"));
            book.setAuthor(author);

            return book;
        }
    }

    @SuppressWarnings("ClassCanBeRecord")
    @RequiredArgsConstructor
    private static class BookResultSetExtractor implements ResultSetExtractor<Book> {

        @Override
        public Book extractData(ResultSet rs) throws SQLException, DataAccessException {
            Book book = null;
            while (rs.next()) {
                if (book == null) {
                    Author author = new Author(
                            rs.getLong("author_id"),
                            rs.getString("author_full_name")
                    );
                    book = new Book(
                            rs.getLong("book_id"),
                            rs.getString("book_title"),
                            author,
                            new ArrayList<>()
                    );
                }
                Genre genre = new Genre(
                        rs.getLong("genre_id"),
                        rs.getString("genre_name")
                );
                book.getGenres().add(genre);
            }
            return book;
        }
    }

    private record BookGenreRelation(long bookId, long genreId) {
    }

    private static class BookGenreRelationRowMapper implements RowMapper<BookGenreRelation> {

        @Override
        public BookGenreRelation mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new BookGenreRelation(
                    rs.getLong("book_id"),
                    rs.getLong("genre_id")
            );
        }
    }
}
