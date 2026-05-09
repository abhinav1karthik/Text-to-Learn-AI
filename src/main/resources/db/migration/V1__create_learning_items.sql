create table learning_items (
    id uuid primary key,
    title varchar(160) not null,
    source_text text not null,
    summary text not null,
    status varchar(32) not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null
);
