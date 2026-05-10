-- =============================================
-- 学生信息表（独立于sys_user，学生系统自成一个体系）
-- =============================================

-- ----------------------------
-- 1、学生信息表
-- ----------------------------
drop table if exists stu_student;
create table stu_student (
  student_id        bigint(20)      not null auto_increment    comment '学生ID',
  admission_year    int(4)          not null                   comment '入学年份（如2024，据此可推算年级=当前年份-入学年份）',
  class_num         int(2)          not null                   comment '班级（1-6班）',
  student_num       int(2)          not null                   comment '学号（1-99）',
  account           varchar(10)     not null                   comment '登录账号（年份2位+班级2位+学号2位，如240101）',
  password          varchar(100)    default ''                 comment '密码',
  student_name      varchar(30)     default ''                 comment '学生姓名',
  sex               char(1)         default '0'                comment '性别（0男 1女 2未知）',
  status            char(1)         default '0'                comment '状态（0正常 1停用）',
  del_flag          char(1)         default '0'                comment '删除标志（0存在 2删除）',
  login_ip          varchar(128)    default ''                 comment '最后登录IP',
  login_date        datetime                                   comment '最后登录时间',
  login_token       varchar(100)    default null               comment '登录令牌',
  create_by         varchar(64)     default ''                 comment '创建者',
  create_time       datetime                                   comment '创建时间',
  update_by         varchar(64)     default ''                 comment '更新者',
  update_time       datetime                                   comment '更新时间',
  remark            varchar(500)    default null               comment '备注',
  primary key (student_id),
  unique key idx_account (account),
  key idx_admission_year (admission_year),
  key idx_class (admission_year, class_num)
) engine=innodb auto_increment=1 comment = '学生信息表';

-- ----------------------------
-- 4、新增教师角色（学生不使用sys_user，无需学生角色）
-- ----------------------------
insert into sys_role values('3', '教师', 'teacher', 3, 1, 1, 1, '0', '0', 'admin', sysdate(), '', null, '教师角色');

-- ----------------------------
-- 5、学生管理菜单
-- ----------------------------
-- 一级目录：学生管理
insert into sys_menu values('2000', '学生管理', '0', '5', 'students', null, '', '', 1, 0, 'M', '0', '0', '', 'education', 'admin', sysdate(), '', null, '学生管理目录');
-- 二级菜单：学生列表
insert into sys_menu values('2001', '学生列表', '2000', '1', 'list', 'student/list/index', '', '', 1, 0, 'C', '0', '0', 'student:list', 'user', 'admin', sysdate(), '', null, '学生列表菜单');
-- 按钮权限
insert into sys_menu values('2002', '学生查询', '2001', '1', '', '', '', '', 1, 0, 'F', '0', '0', 'student:list:query', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2003', '学生新增', '2001', '2', '', '', '', '', 1, 0, 'F', '0', '0', 'student:list:add', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2004', '学生修改', '2001', '3', '', '', '', '', 1, 0, 'F', '0', '0', 'student:list:edit', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2005', '学生删除', '2001', '4', '', '', '', '', 1, 0, 'F', '0', '0', 'student:list:remove', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2006', '学生导出', '2001', '5', '', '', '', '', 1, 0, 'F', '0', '0', 'student:list:export', '#', 'admin', sysdate(), '', null, '');

-- ----------------------------
-- 6、给超级管理员分配学生管理菜单权限
-- ----------------------------
insert into sys_role_menu values ('1', '2000');
insert into sys_role_menu values ('1', '2001');
insert into sys_role_menu values ('1', '2002');
insert into sys_role_menu values ('1', '2003');
insert into sys_role_menu values ('1', '2004');
insert into sys_role_menu values ('1', '2005');
insert into sys_role_menu values ('1', '2006');

-- ----------------------------
-- 7、给教师角色分配学生管理菜单权限（只有查询权限）
-- ----------------------------
insert into sys_role_menu values ('3', '2000');
insert into sys_role_menu values ('3', '2001');
insert into sys_role_menu values ('3', '2002');

-- ----------------------------
-- 8、示例学生数据（密码统一为 admin123 的BCrypt加密）
-- 密码: $2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2
-- ----------------------------
insert into stu_student values (1, 2024, 1, 1, '20240101', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '张三', '0', '0', '0', '127.0.0.1', sysdate(), 'admin', sysdate(), '', null, '示例学生');
