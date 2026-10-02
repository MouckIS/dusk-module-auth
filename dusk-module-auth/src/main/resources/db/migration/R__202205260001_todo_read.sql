create table if not exists sys_todo_read(
	id int8 not null,
	create_id int8,
	create_time timestamp,
	user_id int8,
	todo_id int8,
	primary key (id)
);

create index if not exists index_todo_id on sys_todo_read(todo_id);

create index if not exists index_user_id on sys_todo_read(user_id);