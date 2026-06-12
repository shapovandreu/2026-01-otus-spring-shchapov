insert into authors(full_name)
values ('Author_1'), ('Author_2'), ('Author_3');

insert into genres(name)
values ('Genre_1'), ('Genre_2'), ('Genre_3'),
       ('Genre_4'), ('Genre_5'), ('Genre_6');

insert into books(title, author_id)
values ('BookTitle_1', 1), ('BookTitle_2', 2), ('BookTitle_3', 3);

insert into books_genres(book_id, genre_id)
values (1, 1),   (1, 2),
       (2, 3),   (2, 4),
       (3, 5),   (3, 6);

insert into comments(text, book_id)
values ('Comment_1_Book_1', 1),
       ('Comment_2_Book_1', 1),
       ('Comment_1_Book_2', 2),
       ('Comment_1_Book_3', 3);

insert into users(username, password)
values ('user',  '{noop}password'),
       ('admin', '{noop}admin');

insert into user_roles(user_id, role)
values (1, 'USER'),
       (2, 'USER'),
       (2, 'ADMIN');

insert into acl_sid(id, principal, sid)
values (1, true,  'user'),
       (2, true,  'admin'),
       (3, false, 'ROLE_ADMIN');

insert into acl_class(id, class)
values (1, 'ru.otus.hw.models.Book');

insert into acl_object_identity(id, object_id_class, object_id_identity, parent_object, owner_sid, entries_inheriting)
values (1, 1, 1, null, 2, true),
       (2, 1, 2, null, 2, true),
       (3, 1, 3, null, 2, true);

insert into acl_entry(id, acl_object_identity, ace_order, sid, mask, granting, audit_success, audit_failure)
values (1, 1, 0, 1, 1, true, false, false),
       (2, 1, 1, 1, 2, true, false, false),
       (3, 2, 0, 1, 1, true, false, false);
