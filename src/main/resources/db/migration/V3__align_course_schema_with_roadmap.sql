alter table courses
    add column tags jsonb not null default '[]';

alter table course_modules
    add column created_at timestamp with time zone not null default current_timestamp;

alter table course_modules
    add column updated_at timestamp with time zone not null default current_timestamp;

update lessons
set content_json = '[]'
where content_json is null;

alter table lessons
    alter column content_json set default '[]';

alter table lessons
    alter column content_json set not null;

alter table lessons
    add column is_enriched boolean not null default false;
