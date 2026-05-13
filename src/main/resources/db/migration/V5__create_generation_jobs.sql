create table generation_jobs (
    id uuid primary key,
    user_id uuid not null references app_users(id) on delete cascade,
    type varchar(32) not null,
    status varchar(32) not null,
    prompt text not null,
    course_id uuid references courses(id) on delete set null,
    error_message text,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    started_at timestamp with time zone,
    completed_at timestamp with time zone
);

create index idx_generation_jobs_user_created_at on generation_jobs(user_id, created_at);
create index idx_generation_jobs_status_created_at on generation_jobs(status, created_at);
