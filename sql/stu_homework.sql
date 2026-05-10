-- ----------------------------
-- 作业管理模块表结构
-- ----------------------------

-- 1、作业任务表
-- ----------------------------
drop table if exists stu_homework;
create table stu_homework (
    homework_id       bigint(20)      not null auto_increment    comment '作业ID',
    title             varchar(100)    not null                   comment '作业标题',
    content           varchar(500)    default null               comment '作业内容/要求',
    deadline          datetime        default null               comment '截止时间',
    admission_year    int(4)          not null                   comment '目标入学年份',
    class_num         int(2)          not null                   comment '目标班级',
    storage_path      varchar(200)    not null                   comment '存储路径',
    max_file_size     int(4)          default 500                comment '上传文件大小限制(MB)',
    status            char(1)         default '0'                comment '状态（0正常 1停用）',
    del_flag          char(1)         default '0'                comment '删除标志（0代表存在 2代表删除）',
    create_by         varchar(64)     default ''                 comment '创建者',
    create_time       datetime        default null               comment '创建时间',
    update_by         varchar(64)     default ''                 comment '更新者',
    update_time       datetime        default null               comment '更新时间',
    remark            varchar(500)    default null               comment '备注',
    primary key (homework_id)
) engine=innodb auto_increment=1 comment='作业任务表';

-- 2、作业提交表
-- ----------------------------
drop table if exists stu_homework_submit;
create table stu_homework_submit (
    submit_id         bigint(20)      not null auto_increment    comment '提交ID',
    homework_id       bigint(20)      not null                   comment '作业ID',
    student_id        bigint(20)      not null                   comment '学生ID',
    file_name         varchar(100)    default null               comment '文件名',
    file_path         varchar(200)    default null               comment '文件路径',
    submit_time       datetime        default null               comment '提交时间',
    score             int(3)          default null               comment '分数',
    comment           varchar(200)    default null               comment '评语',
    status            char(1)         default '0'                comment '状态（0已提交 1已批改）',
    create_time       datetime        default null               comment '创建时间',
    update_time       datetime        default null               comment '更新时间',
    primary key (submit_id)
) engine=innodb auto_increment=1 comment='作业提交表';

-- 3、菜单插入
-- ----------------------------
-- 先删除旧数据防止重复
DELETE FROM sys_menu WHERE menu_name in ('作业管理', '作业列表', '作业发布');


-- 插入作业管理菜单
insert into sys_menu values('2000', '作业管理', '0', '5', 'homework', null, '', '', 1, 0, 'M', '0', '0', '', 'el-icon-s-order', 'admin', sysdate(), '', null, '作业管理菜单');

-- 作业列表
insert into sys_menu values('2007', '作业列表', '2000', '1', 'list', 'student/homework/index', '', '', 1, 0, 'C', '0', '0', 'homework:list:list', 'el-icon-document', 'admin', sysdate(), '', null, '作业列表菜单');

-- 作业发布按钮权限
insert into sys_menu values('2008', '作业新增', '2007', '1', '#', '', '', '', 1, 0, 'F', '0', '0', 'homework:list:add', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2009', '作业修改', '2007', '2', '#', '', '', '', 1, 0, 'F', '0', '0', 'homework:list:edit', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2010', '作业删除', '2007', '3', '#', '', '', '', 1, 0, 'F', '0', '0', 'homework:list:remove', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2011', '作业详情', '2007', '4', '#', '', '', '', 1, 0, 'F', '0', '0', 'homework:list:detail', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2012', '提交批改', '2007', '5', '#', '', '', '', 1, 0, 'F', '0', '0', 'homework:list:grade', '#', 'admin', sysdate(), '', null, '');
