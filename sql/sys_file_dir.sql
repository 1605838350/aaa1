-- 文件目录权限表
DROP TABLE IF EXISTS sys_file_dir;
CREATE TABLE sys_file_dir (
    dir_id          BIGINT(20)      NOT NULL AUTO_INCREMENT    COMMENT '目录ID',
    dir_name        VARCHAR(100)    NOT NULL                   COMMENT '目录名称',
    parent_id       BIGINT(20)      DEFAULT 0                  COMMENT '父目录ID',
    path            VARCHAR(500)    NOT NULL                   COMMENT '文件系统路径',
    order_num       INT(4)          DEFAULT 0                  COMMENT '显示顺序',
    perms           VARCHAR(100)    DEFAULT ''                 COMMENT '权限标识',
    status          CHAR(1)         DEFAULT '0'                COMMENT '状态（0正常 1停用）',
    create_by       VARCHAR(64)     DEFAULT ''                 COMMENT '创建者',
    create_time     DATETIME                                   COMMENT '创建时间',
    update_by       VARCHAR(64)     DEFAULT ''                 COMMENT '更新者',
    update_time     DATETIME                                   COMMENT '更新时间',
    remark          VARCHAR(500)    DEFAULT ''                 COMMENT '备注',
    PRIMARY KEY (dir_id)
) ENGINE=InnoDB AUTO_INCREMENT=1000 COMMENT = '文件目录权限表';

-- 角色-文件目录权限表（细粒度权限控制）
DROP TABLE IF EXISTS sys_role_file_dir;
CREATE TABLE sys_role_file_dir (
    role_id         BIGINT(20)      NOT NULL    COMMENT '角色ID',
    dir_id          BIGINT(20)      NOT NULL    COMMENT '目录ID',
    can_view        TINYINT(1)      DEFAULT 0   COMMENT '查看权限（0-否 1-是）',
    can_download    TINYINT(1)      DEFAULT 0   COMMENT '下载权限（0-否 1-是）',
    can_upload      TINYINT(1)      DEFAULT 0   COMMENT '上传权限（0-否 1-是）',
    can_delete      TINYINT(1)      DEFAULT 0   COMMENT '删除权限（0-否 1-是）',
    PRIMARY KEY (role_id, dir_id)
) ENGINE=InnoDB COMMENT = '角色和文件目录权限表';

-- 初始化示例数据
INSERT INTO sys_file_dir (dir_id, dir_name, parent_id, path, order_num, perms, status, create_by, create_time, remark) 
VALUES 
(1, '根目录', 0, '/', 0, 'file:root', '0', 'admin', NOW(), '文件系统根目录'),
(2, '00_公共资讯', 1, '/00_公共资讯', 1, 'file:public', '0', 'admin', NOW(), '公共资讯目录'),
(3, '01_行政管理', 1, '/01_行政管理', 2, 'file:admin', '0', 'admin', NOW(), '行政管理目录'),
(4, '02_教学资源', 1, '/02_教学资源', 3, 'file:teaching', '0', 'admin', NOW(), '教学资源目录'),
(5, '03_学生档案', 1, '/03_学生档案', 4, 'file:student', '0', 'admin', NOW(), '学生档案目录'),
(6, '04_财务', 1, '/04_财务', 5, 'file:finance', '0', 'admin', NOW(), '财务目录'),
(7, 'docker', 1, '/docker', 6, 'file:docker', '0', 'admin', NOW(), 'Docker相关文件'),
(8, 'lost+found', 1, '/lost+found', 7, 'file:lostfound', '0', 'admin', NOW(), '系统恢复目录'),
(9, 'test', 1, '/test', 8, 'file:test', '0', 'admin', NOW(), '测试目录'),
(10, '活动记录', 1, '/活动记录', 9, 'file:activity', '0', 'admin', NOW(), '活动记录目录'),
(11, '系统备份（管理员使用）', 1, '/系统备份（管理员使用）', 10, 'file:backup', '0', 'admin', NOW(), '系统备份目录'),
(12, '老照片', 1, '/老照片', 11, 'file:oldphotos', '0', 'admin', NOW(), '老照片目录');

-- 给管理员角色（role_id=1）分配所有目录的所有权限
INSERT INTO sys_role_file_dir (role_id, dir_id, can_view, can_download, can_upload, can_delete) VALUES 
(1, 1, 1, 1, 1, 1), 
(1, 2, 1, 1, 1, 1), 
(1, 3, 1, 1, 1, 1), 
(1, 4, 1, 1, 1, 1), 
(1, 5, 1, 1, 1, 1),
(1, 6, 1, 1, 1, 1),
(1, 7, 1, 1, 1, 1),
(1, 8, 1, 1, 1, 1),
(1, 9, 1, 1, 1, 1),
(1, 10, 1, 1, 1, 1),
(1, 11, 1, 1, 1, 1),
(1, 12, 1, 1, 1, 1);

-- 给普通用户角色（role_id=2）分配只读权限示例
-- INSERT INTO sys_role_file_dir (role_id, dir_id, can_view, can_download, can_upload, can_delete) VALUES 
-- (2, 3, 1, 1, 0, 0),  -- 照片目录：只能查看和下载
-- (2, 4, 1, 0, 0, 0);  -- 文档目录：只能查看

-- ============================================
-- 以下是数据库迁移更新脚本（用于更新已存在的数据库）
-- 如果是新安装，请忽略以下内容；如果是已有数据库需要更新，请执行以下SQL
-- ============================================

-- 删除旧的子目录数据（保留根目录 dir_id=1）
DELETE FROM sys_role_file_dir WHERE dir_id > 1;
DELETE FROM sys_file_dir WHERE dir_id > 1;

-- 重置自增ID（从13开始，避免与新增数据冲突）
ALTER TABLE sys_file_dir AUTO_INCREMENT = 13;

-- 插入新的目录数据
INSERT INTO sys_file_dir (dir_id, dir_name, parent_id, path, order_num, perms, status, create_by, create_time, remark) 
VALUES 
(2, '00_公共资讯', 1, '/00_公共资讯', 1, 'file:public', '0', 'admin', NOW(), '公共资讯目录'),
(3, '01_行政管理', 1, '/01_行政管理', 2, 'file:admin', '0', 'admin', NOW(), '行政管理目录'),
(4, '02_教学资源', 1, '/02_教学资源', 3, 'file:teaching', '0', 'admin', NOW(), '教学资源目录'),
(5, '03_学生档案', 1, '/03_学生档案', 4, 'file:student', '0', 'admin', NOW(), '学生档案目录'),
(6, '04_财务', 1, '/04_财务', 5, 'file:finance', '0', 'admin', NOW(), '财务目录'),
(7, 'docker', 1, '/docker', 6, 'file:docker', '0', 'admin', NOW(), 'Docker相关文件'),
(8, 'lost+found', 1, '/lost+found', 7, 'file:lostfound', '0', 'admin', NOW(), '系统恢复目录'),
(9, 'test', 1, '/test', 8, 'file:test', '0', 'admin', NOW(), '测试目录'),
(10, '活动记录', 1, '/活动记录', 9, 'file:activity', '0', 'admin', NOW(), '活动记录目录'),
(11, '系统备份（管理员使用）', 1, '/系统备份（管理员使用）', 10, 'file:backup', '0', 'admin', NOW(), '系统备份目录'),
(12, '老照片', 1, '/老照片', 11, 'file:oldphotos', '0', 'admin', NOW(), '老照片目录');

-- 给管理员角色分配所有新目录的所有权限
INSERT INTO sys_role_file_dir (role_id, dir_id, can_view, can_download, can_upload, can_delete) VALUES 
(1, 2, 1, 1, 1, 1),
(1, 3, 1, 1, 1, 1),
(1, 4, 1, 1, 1, 1),
(1, 5, 1, 1, 1, 1),
(1, 6, 1, 1, 1, 1),
(1, 7, 1, 1, 1, 1),
(1, 8, 1, 1, 1, 1),
(1, 9, 1, 1, 1, 1),
(1, 10, 1, 1, 1, 1),
(1, 11, 1, 1, 1, 1),
(1, 12, 1, 1, 1, 1);
