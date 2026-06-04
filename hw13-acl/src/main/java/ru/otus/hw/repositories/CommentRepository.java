package ru.otus.hw.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.otus.hw.models.Comment;

import java.util.List;
import java.util.Optional;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    @Override
    @Query("select c from Comment c left join fetch c.book where c.id = :id")
    Optional<Comment> findById(@Param("id") Long id);

    @Query("select c from Comment c left join fetch c.book where c.book.id = :bookId order by c.id")
    List<Comment> findByBookId(@Param("bookId") long bookId);
}
