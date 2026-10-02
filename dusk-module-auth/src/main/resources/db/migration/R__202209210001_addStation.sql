create table if not exists sys_station(
	id int8 not null,
	create_id int8,
	create_time timestamp,
	last_modify_id int8,
	last_modify_time timestamp,
	version int4,
	dr int4,
	tenant_id int8,
	serial_no varchar(100),
	parent_id int8,
	path varchar(255),
	display_name varchar(255),
	sort_index int4,
	primary key (id)
);

create table if not exists sys_station_user (user_id int8 not null, station_id int8 not null);

alter table if exists sys_station_user add constraint FK9w2kut71bao182brra0tvssuo foreign key (station_id) references sys_station;
alter table if exists sys_station_user add constraint FKhg7pjxss6bitqx3sfyrlwssuu foreign key (user_id) references sys_user;