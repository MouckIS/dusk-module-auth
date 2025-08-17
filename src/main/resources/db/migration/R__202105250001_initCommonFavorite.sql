create table if not exists common_favorite
(
    id               int8         not null,
    create_id        int8,
    create_time      timestamp,
    dr               int4,
    last_modify_id   int8,
    last_modify_time timestamp,
    tenant_id        int8,
    version          int4,
    name             varchar(255) not null,
    type             varchar(100) not null,
    content          text,
    primary key (id)
);
--创建索引
create index if not exists idx_common_favorite on common_favorite (tenant_id, type);
--删除旧表
drop table if exists device_analyze_dashboard;
--添加厂站id
alter table if exists common_favorite add column if not exists org_id int8;
--更改content 类型为varchar
alter table if exists common_favorite alter column content type varchar(10485760);
-- 增加收藏是否公开
alter table if exists common_favorite add column if not exists is_public bool;
-- 设置默认为公开
DO $$
BEGIN
    if exists (SELECT 1 from pg_class where relname = 'common_favorite' and relkind = 'r')
    then
        update common_favorite set is_public = true where is_public is null;
    end if;
end $$;