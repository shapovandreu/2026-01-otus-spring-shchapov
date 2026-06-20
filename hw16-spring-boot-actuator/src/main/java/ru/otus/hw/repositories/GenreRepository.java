package ru.otus.hw.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.data.rest.core.annotation.RestResource;
import ru.otus.hw.models.Genre;

import java.util.List;
import java.util.Set;

@RepositoryRestResource(path = "genres", collectionResourceRel = "genres")
public interface GenreRepository extends JpaRepository<Genre, Long> {

    @RestResource(exported = false)
    @Query("select g from Genre g where g.id in :ids order by g.id")
    List<Genre> findAllByIds(Set<Long> ids);
}