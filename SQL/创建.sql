create schema taskplatform collate utf8mb4_general_ci;

create table mark
(
	id int auto_increment
		primary key,
	task_id int not null comment '任务id',
	oper_id varchar(128) not null comment '操作用户id',
	account varchar(128) null comment '管理账号',
	status tinyint(1) null comment '操作状态 0-申请成功 1-提交成功 2-提交失败 3-超时失败',
	locked varchar(128) null comment '任务锁'
);

create index locked_index
	on mark (locked);

create index operid_index
	on mark (oper_id);

create table taskList
(
	id int unsigned auto_increment
		primary key,
	video_id varchar(64) not null comment '点赞视频id',
	secu_id varchar(100) null comment '视频用户id',
	account varchar(64) null comment '创建用户',
	task_total int default 0 null comment '任务配置数量',
	task_count int default 0 null comment '已完成任务计数',
	status tinyint(1) default 0 null comment '任务状态 0-创建成功 1-已完成 2-处理中 3-任务到期失败',
	price int null comment '价格',
	type int null comment '任务类型 0-点赞 1-关注',
	create_time timestamp null comment '任务创建时间',
	finish_time timestamp null comment '任务完成时间',
	expire_time timestamp null comment '任务到期期限',
	description varchar(256) null comment '任务信息描述',
	task_level tinyint(1) null comment '任务级别,未启用',
	chanel char(2) null comment '任务渠道,未启用',
	constraint videoId
		unique (video_id)
);

create table user
(
	id int auto_increment comment 'uid'
		primary key,
	username varchar(64) not null,
	password varchar(64) not null,
	money int default 0 null,
	create_time timestamp null,
	upd_time timestamp null
);

