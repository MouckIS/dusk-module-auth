create table if not exists sys_todo_ignore(
	id int8 not null,
	create_id int8,
	create_time timestamp,
	last_modify_id int8,
	last_modify_time timestamp,
	version int4,
	dr int4,
	tenant_id int8,
	todo_id int8,
	primary key (id)
);
