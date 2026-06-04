create table authors (
    id bigserial,
    full_name varchar(255),
    primary key (id)
);

create table genres (
    id bigserial,
    name varchar(255),
    primary key (id)
);

create table books (
    id bigserial,
    title varchar(255),
    author_id bigint references authors (id) on delete cascade,
    primary key (id)
);

create table books_genres (
    book_id bigint references books(id) on delete cascade,
    genre_id bigint references genres(id) on delete cascade,
    primary key (book_id, genre_id)
);

create table comments (
    id bigserial,
    text varchar(1024),
    book_id bigint references books(id) on delete cascade,
    primary key (id)
);

create table users (
    id bigserial,
    username varchar(255) not null unique,
    password varchar(255) not null,
    primary key (id)
);

create table user_roles (
    user_id bigint not null references users(id) on delete cascade,
    role varchar(64) not null,
    primary key (user_id, role)
);