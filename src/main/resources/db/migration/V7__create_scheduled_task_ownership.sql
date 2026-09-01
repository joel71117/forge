CREATE TABLE scheduled_task_ownership (
    task_name VARCHAR(100) PRIMARY KEY,
    last_fencing_token BIGINT NOT NULL DEFAULT 0,
    last_run_at TIMESTAMPTZ
);

INSERT INTO scheduled_task_ownership (task_name) VALUES ('job-maintenance');
