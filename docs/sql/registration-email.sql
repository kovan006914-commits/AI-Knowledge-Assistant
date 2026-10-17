-- MySQL 8.0: apply once to the application's database, after checking duplicate emails.
-- This statement preserves all rows. Multiple NULL emails remain allowed.
-- A duplicate non-NULL email makes this ALTER fail instead of removing data.
SET SESSION lock_wait_timeout = 5;
ALTER TABLE `user`
    MODIFY COLUMN email VARCHAR(254) NULL,
    ADD UNIQUE INDEX uk_user_email (email);
