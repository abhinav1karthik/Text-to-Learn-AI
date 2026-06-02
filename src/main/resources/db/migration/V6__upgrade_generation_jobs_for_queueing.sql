alter table generation_jobs
    add column priority varchar(16) not null default 'NORMAL';

alter table generation_jobs
    add column lesson_id uuid references lessons(id) on delete cascade;

alter table generation_jobs
    add column attempt_count integer not null default 0;

alter table generation_jobs
    add column max_attempts integer not null default 3;

alter table generation_jobs
    add column next_run_at timestamp with time zone not null default current_timestamp;

alter table generation_jobs
    add column locked_at timestamp with time zone;

alter table generation_jobs
    add column locked_by varchar(128);

alter table generation_jobs
    add column last_error_type varchar(64);

alter table generation_jobs
    add column last_published_at timestamp with time zone;

alter table courses
    add column generation_job_id uuid references generation_jobs(id) on delete set null;

alter table courses
    add constraint uk_courses_generation_job_id unique (generation_job_id);

create index idx_generation_jobs_lesson_id on generation_jobs(lesson_id);
create index idx_generation_jobs_next_run_at on generation_jobs(status, next_run_at);
