alter table if exists sys_user add column if not exists id_card varchar(255);
alter table if exists sys_user add column if not exists access_card varchar(255);
alter table if exists sys_user add column if not exists user_type varchar(255);
alter table if exists sys_user add column if not exists job varchar(255);
alter table if exists sys_user add column if not exists enter_date date ;


alter table if exists sys_organization add column if not exists type varchar(255);
alter table if exists sys_organization add column if not exists label varchar(255);