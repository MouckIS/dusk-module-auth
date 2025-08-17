--删除BusinessModuleItem, DescriptionModuleItem, ThirdPartyModuleItem, CenterModuleItem
drop table if exists business_module_item;
drop table if exists center_module_item;
drop table if exists description_module_item;
drop table if exists thirdparty_module_item;

--删除CenterModuleType, CenterModuleTypeItemRef
drop table if exists center_module_type;
drop table if exists center_module_type_item_ref;

--新建ModuleItem
create table if not exists dashboard_module_item (
  id int8 NOT NULL,
  create_id int8,
  create_time TIMESTAMP,
  dr int4,
  last_modify_id int8,
  last_modify_time TIMESTAMP,
  tenant_id int8,
  version int4,
  code varchar(255),
  name varchar(255),
  detail_path varchar(255),
  module_type varchar(255),
  data_source varchar(4000),
  PRIMARY KEY ( id )
);

--新建CenterModuleGroupRef, CenterModuleGroup, CenterModuleGroupItemRef

create table if not exists center_module_group (
  id int8 NOT NULL,
  create_id int8,
  create_time TIMESTAMP,
  dr int4,
  last_modify_id int8,
  last_modify_time TIMESTAMP,
  tenant_id int8,
  version int4,
  code varchar(255),
  name varchar(255),
  PRIMARY KEY ( id )
);

create table if not exists center_module_group_ref (
  id int8 NOT NULL,
  group_id int8,
  module_id int8,
  seq int4,
  PRIMARY KEY ( id )
);
create index if not exists centerModuleGroupRefModuleId on center_module_group_ref (module_id);

create table if not exists center_module_group_item_ref (
  id int8 NOT NULL,
  group_id int8,
  item_id int8,
  seq int4,
  PRIMARY KEY ( id )
);
create index if not exists centerModuleGroupItemRefGroupId on center_module_group_item_ref (group_id);

--新增字段Module:centerModule
alter table if exists dashboard_module add column if not exists center_module bool;

--删除字段ModuleItemRef:type;  新增seq字段
alter table if exists module_item_ref add column if not exists seq int4;
alter table if exists module_item_ref drop column if exists module_type;


----------2021/8/3----------------
--ModuleItem表新增requestMode字段
alter table if exists dashboard_module_item add column if not exists request_mode varchar(255);

--新建统计项参数表
create table if not exists dashboard_module_item_parameter (
  id int8 NOT NULL,
  create_id int8,
  create_time TIMESTAMP,
  dr int4,
  last_modify_id int8,
  last_modify_time TIMESTAMP,
  tenant_id int8,
  version int4,
  name varchar(255),
  value varchar(255),
  type varchar(255),
  module_item_id int8,
  PRIMARY KEY ( id )
);
create index if not exists moduleItemParameterItemId on dashboard_module_item_parameter (module_item_id);


---------------2021/8/4--------------------
--删除CenterModuleGroup, CenterModuleGroupRef, CenterModuleGroupItemRef
drop table if exists center_module_group;
drop table if exists center_module_group_item_ref;
drop table if exists center_module_group_ref;

--ModuleItem 新增chartType, ChartColor字段
alter table if exists dashboard_module_item add column if not exists chart_type varchar(255);
alter table if exists dashboard_module_item add column if not exists chart_color varchar(255);
alter table if exists dashboard_module_item add column if not exists seq int4;
alter table if exists dashboard_module_item add column if not exists module_id int8;
alter table if exists dashboard_module_item drop column if exists request_mode;

--删除ModuleItemRef, ModuleItemParameter
drop table if exists module_item_ref;
drop table if exists dashboard_module_item_parameter;


-----------------2021/8/6-----------------------------
--ModuleItem表删除ChartColor, seq字段
alter table if exists dashboard_module_item drop column if exists chart_color;
alter table if exists dashboard_module_item drop column if exists seq;

--删除ClassifyModuleRef表
drop table if exists module_classify_ref;

--DashBoardTheme 新增description字段
alter table if exists dashboard_theme add column if not exists description varchar(255);

--ModuleClassify 改名 DashBoardClassify
alter table IF EXISTS module_classify rename to dashboard_classify;
alter table if exists dashboard_classify add column if not exists zone_num int4;

--新增DashBoardZone表
create table if not exists dashboard_zone (
  id int8 NOT NULL,
  create_id int8,
  create_time TIMESTAMP,
  dr int4,
  last_modify_id int8,
  last_modify_time TIMESTAMP,
  tenant_id int8,
  version int4,
  name varchar(255),
  classify_id int8,
  orientation varchar(255),
  zone_position int4,
  PRIMARY KEY ( id )
);
create index if not exists dashboardZoneClassifyId on dashboard_zone (classify_id);

--新增DashBoardZoneItemRef
create table if not exists dashboard_zone_item_ref (
  id int8 NOT NULL,
  module_id int8,
  module_item_id int8,
  zone_id int8,
  seq int4,
  chart_color varchar(255),
  PRIMARY KEY ( id )
);
create index if not exists dashboardZoneItemRefId on dashboard_zone_item_ref (zone_id);