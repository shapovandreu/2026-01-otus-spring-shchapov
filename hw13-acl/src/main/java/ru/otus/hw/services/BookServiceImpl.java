package ru.otus.hw.services;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PostFilter;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.acls.domain.BasePermission;
import org.springframework.security.acls.domain.ObjectIdentityImpl;
import org.springframework.security.acls.domain.PrincipalSid;
import org.springframework.security.acls.model.MutableAcl;
import org.springframework.security.acls.model.MutableAclService;
import org.springframework.security.acls.model.ObjectIdentity;
import org.springframework.security.acls.model.Sid;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.otus.hw.exceptions.EntityNotFoundException;
import ru.otus.hw.models.Book;
import ru.otus.hw.repositories.AuthorRepository;
import ru.otus.hw.repositories.BookRepository;
import ru.otus.hw.repositories.GenreRepository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.springframework.util.CollectionUtils.isEmpty;

@RequiredArgsConstructor
@Service
public class BookServiceImpl implements BookService {

    private final AuthorRepository authorRepository;

    private final GenreRepository genreRepository;

    private final BookRepository bookRepository;

    private final MutableAclService aclService;

    @Override
    @PostAuthorize("returnObject.isEmpty() or hasRole('ADMIN') or hasPermission(returnObject.get(), 'READ')")
    public Optional<Book> findById(long id) {
        return bookRepository.findById(id);
    }

    @Override
    @PostFilter("hasRole('ADMIN') or hasPermission(filterObject, 'READ')")
    public List<Book> findAll() {
        return bookRepository.findAll();
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public Book insert(String title, long authorId, Set<Long> genresIds) {
        var book = save(0, title, authorId, genresIds);
        createAcl(book);
        return book;
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN') or hasPermission(#id, 'ru.otus.hw.models.Book', 'WRITE')")
    public Book update(long id, String title, long authorId, Set<Long> genresIds) {
        return save(id, title, authorId, genresIds);
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteById(long id) {
        bookRepository.deleteById(id);
        aclService.deleteAcl(new ObjectIdentityImpl(Book.class, id), true);
    }

    private Book save(long id, String title, long authorId, Set<Long> genresIds) {
        if (isEmpty(genresIds)) {
            throw new IllegalArgumentException("Genres ids must not be null");
        }

        var author = authorRepository.findById(authorId)
                .orElseThrow(() -> new EntityNotFoundException("Author with id %d not found".formatted(authorId)));
        var genres = genreRepository.findAllByIds(genresIds);
        if (isEmpty(genres) || genresIds.size() != genres.size()) {
            throw new EntityNotFoundException("One or all genres with ids %s not found".formatted(genresIds));
        }

        var book = new Book(id, title, author, genres);
        return bookRepository.save(book);
    }

    private void createAcl(Book book) {
        ObjectIdentity oid = new ObjectIdentityImpl(Book.class, book.getId());
        Sid owner = new PrincipalSid(SecurityContextHolder.getContext().getAuthentication());

        MutableAcl acl = aclService.createAcl(oid);
        acl.insertAce(acl.getEntries().size(), BasePermission.READ, owner, true);
        acl.insertAce(acl.getEntries().size(), BasePermission.WRITE, owner, true);
        acl.insertAce(acl.getEntries().size(), BasePermission.ADMINISTRATION, owner, true);
        aclService.updateAcl(acl);
    }

}
