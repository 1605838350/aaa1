-- 作业表增加上传文件大小限制字段
ALTER TABLE stu_homework ADD COLUMN max_file_size int(4) DEFAULT 500 COMMENT '上传文件大小限制(MB)';
