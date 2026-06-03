alter table generation_jobs
    alter column max_attempts set default 4;

update generation_jobs
set max_attempts = 4
where max_attempts = 3
  and status in ('QUEUED', 'RUNNING');
