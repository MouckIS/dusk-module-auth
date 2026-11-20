ALTER TABLE  sys_user ADD COLUMN IF not EXISTS active_start_date timestamp;
ALTER TABLE  sys_user ADD COLUMN IF not EXISTS active_end_date timestamp;