create table if not exists dashboard_theme (
  id int8 NOT NULL,
  create_id int8,
  create_time TIMESTAMP,
  dr int4,
  last_modify_id int8,
  last_modify_time TIMESTAMP,
  tenant_id int8,
  version int4,
  name varchar(255),
  theme_type varchar(255),
  title varchar(255),
  show_time bool,
  show_weather bool,
  PRIMARY KEY ( id )
);


create table if not exists dashboard_premission (
  id int8 NOT NULL,
  create_id int8,
  create_time TIMESTAMP,
  dr int4,
  last_modify_id int8,
  last_modify_time TIMESTAMP,
  tenant_id int8,
  version int4,
  theme_id int8,
  user_id int8,
  PRIMARY KEY ( id )
);
create index if not exists dashboardPremissionUserId on dashboard_premission (user_id);


create table if not exists dashboard_module (
  id int8 NOT NULL,
  create_id int8,
  create_time TIMESTAMP,
  dr int4,
  last_modify_id int8,
  last_modify_time TIMESTAMP,
  tenant_id int8,
  version int4,
  name varchar(255),
  module_type varchar(255),
  PRIMARY KEY ( id )
);


create table if not exists module_classify (
  id int8 NOT NULL,
  create_id int8,
  create_time TIMESTAMP,
  dr int4,
  last_modify_id int8,
  last_modify_time TIMESTAMP,
  tenant_id int8,
  version int4,
  name varchar(255),
  layout_id varchar(255),
  theme_id int8,
  seq int4,
  PRIMARY KEY ( id )
);

create table if not exists module_classify_ref (
  id int8 NOT NULL,
  classify_id int8,
  module_id int8,
  layout_location int4,
  PRIMARY KEY ( id )
);
create index if not exists moduleClassifyRefClassifyId on module_classify_ref (classify_id);

create table if not exists module_item_ref (
  id int8 NOT NULL,
  item_id int8,
  module_id int8,
  PRIMARY KEY ( id )
);
create index if not exists moduleItemRefModuleId on module_item_ref (module_id);


create table if not exists business_module_item (
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
  url varchar(255),
  PRIMARY KEY ( id )
);


create table if not exists description_module_item (
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
  code_snippet varchar(4000),
  PRIMARY KEY ( id )
);


create table if not exists thirdparty_module_item (
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
  third_party_id int8,
  PRIMARY KEY ( id )
);


create table if not exists center_module_item (
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
  url varchar(255),
  PRIMARY KEY ( id )
);
create table if not exists center_module_type (
  id int8 NOT NULL,
  create_id int8,
  create_time TIMESTAMP,
  dr int4,
  last_modify_id int8,
  last_modify_time TIMESTAMP,
  tenant_id int8,
  version int4,
  name varchar(255),
  PRIMARY KEY ( id )
);
create table if not exists center_module_type_item_ref (
  id int8 NOT NULL,
  type_id int8,
  item_id int8,
  PRIMARY KEY ( id )
);
create index if not exists centerModuleTypeItemRefTypeId on center_module_type_item_ref (type_id);


alter table if exists dashboard_module drop column module_type;
alter table if exists dashboard_module add column code varchar(255);

alter table if exists module_item_ref add column module_type varchar(255);

alter table if exists center_module_type add column code varchar(255);
alter table if exists center_module_type add column module_type varchar(255);

alter table if exists business_module_item add column module_type varchar(255);
alter table if exists description_module_item add column module_type varchar(255);
alter table if exists thirdparty_module_item add column module_type varchar(255);