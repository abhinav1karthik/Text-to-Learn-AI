create table lesson_audio (
    id uuid primary key,
    lesson_id uuid not null references lessons(id) on delete cascade,
    language varchar(32) not null,
    voice_name varchar(64) not null,
    storage_provider varchar(32) not null,
    storage_key varchar(512) not null,
    content_type varchar(100) not null,
    file_name varchar(255) not null,
    file_size_bytes bigint not null,
    created_at timestamp with time zone not null default current_timestamp,
    updated_at timestamp with time zone not null default current_timestamp,
    constraint uk_lesson_audio_lesson_language_voice unique (lesson_id, language, voice_name)
);

create index idx_lesson_audio_lesson_id on lesson_audio(lesson_id);
