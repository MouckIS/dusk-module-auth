create table if not exists sys_extend_field(
	id int8 not null,
	create_id int8,
	create_time timestamp,
	last_modify_id int8,
	last_modify_time timestamp,
	version int4,
	dr int4,
	tenant_id int8,
	entity_id int8,
	entity_class varchar(255),
	key varchar(50),
	value varchar(2000),
	primary key (id)
);
