drop table if exists learning_items;

create table app_users (
    id uuid primary key,
    auth0_subject varchar(255) not null unique,
    email varchar(320),
    name varchar(160),
    picture_url text,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null
);

create table courses (
    id uuid primary key,
    user_id uuid not null references app_users(id) on delete cascade,
    prompt text not null,
    title varchar(200) not null,
    description text,
    status varchar(32) not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null
);

create table course_modules (
    id uuid primary key,
    course_id uuid not null references courses(id) on delete cascade,
    title varchar(200) not null,
    summary text,
    position integer not null,
    unique (course_id, position)
);

create table lessons (
    id uuid primary key,
    module_id uuid not null references course_modules(id) on delete cascade,
    title varchar(200) not null,
    position integer not null,
    status varchar(32) not null,
    objectives_json jsonb,
    content_json jsonb,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    unique (module_id, position)
);
