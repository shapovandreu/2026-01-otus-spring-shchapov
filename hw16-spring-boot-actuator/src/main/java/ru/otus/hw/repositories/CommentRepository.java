package ru.otus.hw.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.data.rest.core.annotation.RestResource;
import ru.otus.hw.models.Comment;

import java.util.List;
import java.util.Optional;

@RepositoryRestResource(path = "comments", collectionResourceRel = "comments")
public interface CommentRepository extends JpaRepository<Comment, Long> {

    @Override
    @Query("select c from Comment c left join fetch c.book where c.id = :id")
    Optional<Comment> findById(@Param("id") Long id);

    @RestResource(exported = false)
    @Query("select c from Comment c left join fetch c.book where c.book.id = :bookId order by c.id")
    List<Comment> findByBookId(@Param("bookId") long bookId);
}