-- ============================================
-- 2020学年第一学期教学资源目录结构数据
-- 层级关系：02_教学资源 → 各学期教务处工作文件夹 → 2020学年第一学期 → 各子目录
-- 注意：使用1000起始的ID避免与现有数据冲突
-- ============================================

-- ============================================
-- 一级目录：各学期教务处工作文件夹 (parent_id=4, 即02_教学资源)
-- ============================================
INSERT INTO sys_file_dir (dir_id, dir_name, parent_id, path, order_num, perms, status, create_by, create_time, remark) VALUES
(1000, '各学期教务处工作文件夹', 4, '/02_教学资源/各学期教务处工作文件夹', 1, 'file:teaching:jiaowu', '0', 'admin', NOW(), '各学期教务处工作文件夹');

-- ============================================
-- 二级目录：2020学年第一学期 (parent_id=1000, 即各学期教务处工作文件夹)
-- ============================================
INSERT INTO sys_file_dir (dir_id, dir_name, parent_id, path, order_num, perms, status, create_by, create_time, remark) VALUES
(1100, '2020学年第一学期', 1000, '/02_教学资源/各学期教务处工作文件夹/2020学年第一学期', 1, 'file:teaching:jiaowu:2020a', '0', 'admin', NOW(), '2020学年第一学期');

-- ============================================
-- 三级目录：2020学年第一学期的直接子目录 (parent_id=1100)
-- ============================================
INSERT INTO sys_file_dir (dir_id, dir_name, parent_id, path, order_num, perms, status, create_by, create_time, remark) VALUES
-- 2020学困生 (dir_id=1200)
(1200, '2020学困生', 1100, '/02_教学资源/各学期教务处工作文件夹/2020学年第一学期/2020学困生', 1, 'file:teaching:jiaowu:2020a:xuekun', '0', 'admin', NOW(), '2020学年学困生资料'),

-- 2020学年学生名单 (dir_id=1201)
(1201, '2020学年学生名单', 1100, '/02_教学资源/各学期教务处工作文件夹/2020学年第一学期/2020学年学生名单', 2, 'file:teaching:jiaowu:2020a:studentlist', '0', 'admin', NOW(), '2020学年学生名单'),

-- 2020第一学期考试 (dir_id=1202)
(1202, '2020第一学期考试', 1100, '/02_教学资源/各学期教务处工作文件夹/2020学年第一学期/2020第一学期考试', 3, 'file:teaching:jiaowu:2020a:exam', '0', 'admin', NOW(), '2020学年第一学期考试资料'),

-- 创金牌 (dir_id=1203)
(1203, '创金牌', 1100, '/02_教学资源/各学期教务处工作文件夹/2020学年第一学期/创金牌', 4, 'file:teaching:jiaowu:2020a:chuangjin', '0', 'admin', NOW(), '创金牌活动资料'),

-- 劳动案例 (dir_id=1204)
(1204, '劳动案例', 1100, '/02_教学资源/各学期教务处工作文件夹/2020学年第一学期/劳动案例', 5, 'file:teaching:jiaowu:2020a:laodong', '0', 'admin', NOW(), '劳动教育案例'),

-- 教学常规工作 (dir_id=1205)
(1205, '教学常规工作', 1100, '/02_教学资源/各学期教务处工作文件夹/2020学年第一学期/教学常规工作', 6, 'file:teaching:jiaowu:2020a:changui', '0', 'admin', NOW(), '教学常规工作资料'),

-- 教育提质专项督查 (dir_id=1206)
(1206, '教育提质专项督查', 1100, '/02_教学资源/各学期教务处工作文件夹/2020学年第一学期/教育提质专项督查', 7, 'file:teaching:jiaowu:2020a:tizhi', '0', 'admin', NOW(), '教育提质专项督查资料'),

-- 文件 (dir_id=1207)
(1207, '文件', 1100, '/02_教学资源/各学期教务处工作文件夹/2020学年第一学期/文件', 8, 'file:teaching:jiaowu:2020a:wenjian', '0', 'admin', NOW(), '上级文件资料'),

-- 长坑小学2021课题申报 (dir_id=1208)
(1208, '长坑小学2021课题申报', 1100, '/02_教学资源/各学期教务处工作文件夹/2020学年第一学期/长坑小学2021课题申报', 9, 'file:teaching:jiaowu:2020a:keti2021', '0', 'admin', NOW(), '2021年课题申报资料'),

-- （新）长坑小学家庭教育三优评比资料 (dir_id=1209)
(1209, '（新）长坑小学家庭教育三优评比资料', 1100, '/02_教学资源/各学期教务处工作文件夹/2020学年第一学期/（新）长坑小学家庭教育三优评比资料', 10, 'file:teaching:jiaowu:2020a:jiating', '0', 'admin', NOW(), '家庭教育三优评比资料');

-- ============================================
-- 四级目录
-- ============================================

-- 2020学困生 的子目录 (parent_id=1200)
INSERT INTO sys_file_dir (dir_id, dir_name, parent_id, path, order_num, perms, status, create_by, create_time, remark) VALUES
(1300, '11 数学', 1200, '/02_教学资源/各学期教务处工作文件夹/2020学年第一学期/2020学困生/11 数学', 1, 'file:teaching:xuekun:11math', '0', 'admin', NOW(), '一年级一班数学学困生'),
(1301, '11 语文', 1200, '/02_教学资源/各学期教务处工作文件夹/2020学年第一学期/2020学困生/11 语文', 2, 'file:teaching:xuekun:11chinese', '0', 'admin', NOW(), '一年级一班语文学困生'),
(1302, '12 语文', 1200, '/02_教学资源/各学期教务处工作文件夹/2020学年第一学期/2020学困生/12 语文', 3, 'file:teaching:xuekun:12chinese', '0', 'admin', NOW(), '一年级二班语文学困生'),
(1303, '21 数学', 1200, '/02_教学资源/各学期教务处工作文件夹/2020学年第一学期/2020学困生/21 数学', 4, 'file:teaching:xuekun:21math', '0', 'admin', NOW(), '二年级一班数学学困生'),
(1304, '21 语文', 1200, '/02_教学资源/各学期教务处工作文件夹/2020学年第一学期/2020学困生/21 语文', 5, 'file:teaching:xuekun:21chinese', '0', 'admin', NOW(), '二年级一班语文学困生'),
(1305, '22 语文', 1200, '/02_教学资源/各学期教务处工作文件夹/2020学年第一学期/2020学困生/22 语文', 6, 'file:teaching:xuekun:22chinese', '0', 'admin', NOW(), '二年级二班语文学困生'),
(1306, '31 数学', 1200, '/02_教学资源/各学期教务处工作文件夹/2020学年第一学期/2020学困生/31 数学', 7, 'file:teaching:xuekun:31math', '0', 'admin', NOW(), '三年级一班数学学困生'),
(1307, '31 科学', 1200, '/02_教学资源/各学期教务处工作文件夹/2020学年第一学期/2020学困生/31 科学', 8, 'file:teaching:xuekun:31science', '0', 'admin', NOW(), '三年级一班科学学困生'),
(1308, '31 英语', 1200, '/02_教学资源/各学期教务处工作文件夹/2020学年第一学期/2020学困生/31 英语', 9, 'file:teaching:xuekun:31english', '0', 'admin', NOW(), '三年级一班英语学困生'),
(1309, '32 科学', 1200, '/02_教学资源/各学期教务处工作文件夹/2020学年第一学期/2020学困生/32 科学', 10, 'file:teaching:xuekun:32science', '0', 'admin', NOW(), '三年级二班科学学困生'),
(1310, '32 英语', 1200, '/02_教学资源/各学期教务处工作文件夹/2020学年第一学期/2020学困生/32 英语', 11, 'file:teaching:xuekun:32english', '0', 'admin', NOW(), '三年级二班英语学困生'),
(1311, '32 语文', 1200, '/02_教学资源/各学期教务处工作文件夹/2020学年第一学期/2020学困生/32 语文', 12, 'file:teaching:xuekun:32chinese', '0', 'admin', NOW(), '三年级二班语文学困生'),
(1312, '41 数学', 1200, '/02_教学资源/各学期教务处工作文件夹/2020学年第一学期/2020学困生/41 数学', 13, 'file:teaching:xuekun:41math', '0', 'admin', NOW(), '四年级一班数学学困生'),
(1313, '41 科学', 1200, '/02_教学资源/各学期教务处工作文件夹/2020学年第一学期/2020学困生/41 科学', 14, 'file:teaching:xuekun:41science', '0', 'admin', NOW(), '四年级一班科学学困生'),
(1314, '41 英语', 1200, '/02_教学资源/各学期教务处工作文件夹/2020学年第一学期/2020学困生/41 英语', 15, 'file:teaching:xuekun:41english', '0', 'admin', NOW(), '四年级一班英语学困生'),
(1315, '41 语文', 1200, '/02_教学资源/各学期教务处工作文件夹/2020学年第一学期/2020学困生/41 语文', 16, 'file:teaching:xuekun:41chinese', '0', 'admin', NOW(), '四年级一班语文学困生'),
(1316, '42 数学', 1200, '/02_教学资源/各学期教务处工作文件夹/2020学年第一学期/2020学困生/42 数学', 17, 'file:teaching:xuekun:42math', '0', 'admin', NOW(), '四年级二班数学学困生'),
(1317, '42 科学', 1200, '/02_教学资源/各学期教务处工作文件夹/2020学年第一学期/2020学困生/42 科学', 18, 'file:teaching:xuekun:42science', '0', 'admin', NOW(), '四年级二班科学学困生'),
(1318, '42 英语', 1200, '/02_教学资源/各学期教务处工作文件夹/2020学年第一学期/2020学困生/42 英语', 19, 'file:teaching:xuekun:42english', '0', 'admin', NOW(), '四年级二班英语学困生'),
(1319, '42 语文', 1200, '/02_教学资源/各学期教务处工作文件夹/2020学年第一学期/2020学困生/42 语文', 20, 'file:teaching:xuekun:42chinese', '0', 'admin', NOW(), '四年级二班语文学困生'),
(1320, '51 数学', 1200, '/02_教学资源/各学期教务处工作文件夹/2020学年第一学期/2020学困生/51 数学', 21, 'file:teaching:xuekun:51math', '0', 'admin', NOW(), '五年级一班数学学困生'),
(1321, '51 英语', 1200, '/02_教学资源/各学期教务处工作文件夹/2020学年第一学期/2020学困生/51 英语', 22, 'file:teaching:xuekun:51english', '0', 'admin', NOW(), '五年级一班英语学困生'),
(1322, '51 语文', 1200, '/02_教学资源/各学期教务处工作文件夹/2020学年第一学期/2020学困生/51 语文', 23, 'file:teaching:xuekun:51chinese', '0', 'admin', NOW(), '五年级一班语文学困生'),
(1323, '52 数学', 1200, '/02_教学资源/各学期教务处工作文件夹/2020学年第一学期/2020学困生/52 数学', 24, 'file:teaching:xuekun:52math', '0', 'admin', NOW(), '五年级二班数学学困生'),
(1324, '52 英语', 1200, '/02_教学资源/各学期教务处工作文件夹/2020学年第一学期/2020学困生/52 英语', 25, 'file:teaching:xuekun:52english', '0', 'admin', NOW(), '五年级二班英语学困生'),
(1325, '52 语文', 1200, '/02_教学资源/各学期教务处工作文件夹/2020学年第一学期/2020学困生/52 语文', 26, 'file:teaching:xuekun:52chinese', '0', 'admin', NOW(), '五年级二班语文学困生'),
(1326, '61 数学', 1200, '/02_教学资源/各学期教务处工作文件夹/2020学年第一学期/2020学困生/61 数学', 27, 'file:teaching:xuekun:61math', '0', 'admin', NOW(), '六年级一班数学学困生'),
(1327, '61 科学', 1200, '/02_教学资源/各学期教务处工作文件夹/2020学年第一学期/2020学困生/61 科学', 28, 'file:teaching:xuekun:61science', '0', 'admin', NOW(), '六年级一班科学学困生'),
(1328, '61 英语', 1200, '/02_教学资源/各学期教务处工作文件夹/2020学年第一学期/2020学困生/61 英语', 29, 'file:teaching:xuekun:61english', '0', 'admin', NOW(), '六年级一班英语学困生'),
(1329, '61 语文', 1200, '/02_教学资源/各学期教务处工作文件夹/2020学年第一学期/2020学困生/61 语文', 30, 'file:teaching:xuekun:61chinese', '0', 'admin', NOW(), '六年级一班语文学困生'),
(1330, '62 数学', 1200, '/02_教学资源/各学期教务处工作文件夹/2020学年第一学期/2020学困生/62 数学', 31, 'file:teaching:xuekun:62math', '0', 'admin', NOW(), '六年级二班数学学困生'),
(1331, '62 科学', 1200, '/02_教学资源/各学期教务处工作文件夹/2020学年第一学期/2020学困生/62 科学', 32, 'file:teaching:xuekun:62science', '0', 'admin', NOW(), '六年级二班科学学困生'),
(1332, '62 英语', 1200, '/02_教学资源/各学期教务处工作文件夹/2020学年第一学期/2020学困生/62 英语', 33, 'file:teaching:xuekun:62english', '0', 'admin', NOW(), '六年级二班英语学困生'),
(1333, '62 语文', 1200, '/02_教学资源/各学期教务处工作文件夹/2020学年第一学期/2020学困生/62 语文', 34, 'file:teaching:xuekun:62chinese', '0', 'admin', NOW(), '六年级二班语文学困生');

-- 2020学年学生名单 的子目录 (parent_id=1201)
INSERT INTO sys_file_dir (dir_id, dir_name, parent_id, path, order_num, perms, status, create_by, create_time, remark) VALUES
(1350, '202009学生名册', 1201, '/02_教学资源/各学期教务处工作文件夹/2020学年第一学期/2020学年学生名单/202009学生名册', 1, 'file:teaching:studentlist:202009', '0', 'admin', NOW(), '2020年9月学生名册'),
(1351, '各班名单（有身份证）', 1201, '/02_教学资源/各学期教务处工作文件夹/2020学年第一学期/2020学年学生名单/各班名单（有身份证）', 2, 'file:teaching:studentlist:idcard', '0', 'admin', NOW(), '各班学生名单（含身份证号）');

-- 2020第一学期考试 的子目录 (parent_id=1202)
INSERT INTO sys_file_dir (dir_id, dir_name, parent_id, path, order_num, perms, status, create_by, create_time, remark) VALUES
(1360, '2020第一学期期中考试资料', 1202, '/02_教学资源/各学期教务处工作文件夹/2020学年第一学期/2020第一学期考试/2020第一学期期中考试资料', 1, 'file:teaching:exam:midterm', '0', 'admin', NOW(), '2020第一学期期中考试资料'),
(1361, '2020第一学期期末考试', 1202, '/02_教学资源/各学期教务处工作文件夹/2020学年第一学期/2020第一学期考试/2020第一学期期末考试', 2, 'file:teaching:exam:final', '0', 'admin', NOW(), '2020第一学期期末考试');

-- 创金牌 的子目录 (parent_id=1203)
INSERT INTO sys_file_dir (dir_id, dir_name, parent_id, path, order_num, perms, status, create_by, create_time, remark) VALUES
(1370, '民族风俗我知道——主题班会', 1203, '/02_教学资源/各学期教务处工作文件夹/2020学年第一学期/创金牌/民族风俗我知道——主题班会', 1, 'file:teaching:chuangjin:minzu', '0', 'admin', NOW(), '民族风俗主题班会资料'),
(1371, '调查问卷', 1203, '/02_教学资源/各学期教务处工作文件夹/2020学年第一学期/创金牌/调查问卷', 2, 'file:teaching:chuangjin:diaocha', '0', 'admin', NOW(), '调查问卷资料');

-- 劳动案例 的子目录 (parent_id=1204)
INSERT INTO sys_file_dir (dir_id, dir_name, parent_id, path, order_num, perms, status, create_by, create_time, remark) VALUES
(1380, '李锦祥 人人会劳动 人人会生活', 1204, '/02_教学资源/各学期教务处工作文件夹/2020学年第一学期/劳动案例/李锦祥 人人会劳动 人人会生活', 1, 'file:teaching:laodong:lijinxiang', '0', 'admin', NOW(), '李锦祥劳动案例'),
(1381, '生活处处有劳动——全面培养小学生劳动生活技能', 1204, '/02_教学资源/各学期教务处工作文件夹/2020学年第一学期/劳动案例/生活处处有劳动——全面培养小学生劳动生活技能', 2, 'file:teaching:laodong:shenghuo', '0', 'admin', NOW(), '劳动生活技能培养案例');

-- 教学常规工作 的子目录 (parent_id=1205)
INSERT INTO sys_file_dir (dir_id, dir_name, parent_id, path, order_num, perms, status, create_by, create_time, remark) VALUES
(1390, '（教学常规工作、方案、表格）', 1205, '/02_教学资源/各学期教务处工作文件夹/2020学年第一学期/教学常规工作/（教学常规工作、方案、表格）', 1, 'file:teaching:changui:doc', '0', 'admin', NOW(), '教学常规工作相关文档'),
(1391, '教研组', 1205, '/02_教学资源/各学期教务处工作文件夹/2020学年第一学期/教学常规工作/教研组', 2, 'file:teaching:changui:jiaoyanzu', '0', 'admin', NOW(), '教研组资料');

-- 教育提质专项督查 的子目录 (parent_id=1206)
INSERT INTO sys_file_dir (dir_id, dir_name, parent_id, path, order_num, perms, status, create_by, create_time, remark) VALUES
(1400, '2020第一学期国旗下讲话', 1206, '/02_教学资源/各学期教务处工作文件夹/2020学年第一学期/教育提质专项督查/2020第一学期国旗下讲话', 1, 'file:teaching:tizhi:guoqi', '0', 'admin', NOW(), '国旗下讲话资料'),
(1401, '课改督查2020.12.30', 1206, '/02_教学资源/各学期教务处工作文件夹/2020学年第一学期/教育提质专项督查/课改督查2020.12.30', 2, 'file:teaching:tizhi:kegai', '0', 'admin', NOW(), '课改督查资料');

-- 文件 的子目录 (parent_id=1207)
INSERT INTO sys_file_dir (dir_id, dir_name, parent_id, path, order_num, perms, status, create_by, create_time, remark) VALUES
(1410, '关于开展2020年县学科（德育）带头人评选活动', 1207, '/02_教学资源/各学期教务处工作文件夹/2020学年第一学期/文件/关于开展2020年县学科（德育）带头人评选活动', 1, 'file:teaching:wenjian:xueke', '0', 'admin', NOW(), '学科带头人评选活动通知'),
(1411, '（关于开展2020年县级新苗、新秀、能手评选活动的通知）', 1207, '/02_教学资源/各学期教务处工作文件夹/2020学年第一学期/文件/（关于开展2020年县级新苗、新秀、能手评选活动的通知）', 2, 'file:teaching:wenjian:xinmiao', '0', 'admin', NOW(), '新苗新秀能手评选活动通知'),
(1412, '（关于组织开展先进教学管理工作现场陈述、考评活动的通知）', 1207, '/02_教学资源/各学期教务处工作文件夹/2020学年第一学期/文件/（关于组织开展先进教学管理工作现场陈述、考评活动的通知）', 3, 'file:teaching:wenjian:xianjin', '0', 'admin', NOW(), '先进教学管理考评活动通知');

-- 长坑小学2021课题申报 的子目录 (parent_id=1208)
INSERT INTO sys_file_dir (dir_id, dir_name, parent_id, path, order_num, perms, status, create_by, create_time, remark) VALUES
(1420, '数学', 1208, '/02_教学资源/各学期教务处工作文件夹/2020学年第一学期/长坑小学2021课题申报/数学', 1, 'file:teaching:keti:math', '0', 'admin', NOW(), '数学课题申报'),
(1421, '麻涵佳 心理', 1208, '/02_教学资源/各学期教务处工作文件夹/2020学年第一学期/长坑小学2021课题申报/麻涵佳 心理', 2, 'file:teaching:keti:xinli', '0', 'admin', NOW(), '心理健康课题申报'),
(1422, '麻涵佳 语文', 1208, '/02_教学资源/各学期教务处工作文件夹/2020学年第一学期/长坑小学2021课题申报/麻涵佳 语文', 3, 'file:teaching:keti:chinese', '0', 'admin', NOW(), '语文课题申报');

-- （新）长坑小学家庭教育三优评比资料 的子目录 (parent_id=1209)
INSERT INTO sys_file_dir (dir_id, dir_name, parent_id, path, order_num, perms, status, create_by, create_time, remark) VALUES
(1430, '家庭教育优秀案例', 1209, '/02_教学资源/各学期教务处工作文件夹/2020学年第一学期/（新）长坑小学家庭教育三优评比资料/家庭教育优秀案例', 1, 'file:teaching:jiating:anli', '0', 'admin', NOW(), '家庭教育优秀案例'),
(1431, '家庭教育优秀论文', 1209, '/02_教学资源/各学期教务处工作文件夹/2020学年第一学期/（新）长坑小学家庭教育三优评比资料/家庭教育优秀论文', 2, 'file:teaching:jiating:lunwen', '0', 'admin', NOW(), '家庭教育优秀论文');

-- ============================================
-- 五级目录
-- ============================================

-- 2020第一学期期中考试资料 的子目录 (parent_id=1360)
INSERT INTO sys_file_dir (dir_id, dir_name, parent_id, path, order_num, perms, status, create_by, create_time, remark) VALUES
(1500, '2020第一学期期中考试', 1360, '/02_教学资源/各学期教务处工作文件夹/2020学年第一学期/2020第一学期考试/2020第一学期期中考试资料/2020第一学期期中考试', 1, 'file:teaching:exam:midterm:exam', '0', 'admin', NOW(), '期中考试试卷'),
(1501, '试卷分析', 1360, '/02_教学资源/各学期教务处工作文件夹/2020学年第一学期/2020第一学期考试/2020第一学期期中考试资料/试卷分析', 2, 'file:teaching:exam:midterm:analysis', '0', 'admin', NOW(), '期中考试试卷分析');

-- 2020第一学期期末考试 的子目录 (parent_id=1361)
INSERT INTO sys_file_dir (dir_id, dir_name, parent_id, path, order_num, perms, status, create_by, create_time, remark) VALUES
(1510, '2020 第一学期期末参考答案', 1361, '/02_教学资源/各学期教务处工作文件夹/2020学年第一学期/2020第一学期考试/2020第一学期期末考试/2020 第一学期期末参考答案', 1, 'file:teaching:exam:final:answer', '0', 'admin', NOW(), '期末考试参考答案'),
(1511, '2020 第一学期期末成绩', 1361, '/02_教学资源/各学期教务处工作文件夹/2020学年第一学期/2020第一学期考试/2020第一学期期末考试/2020 第一学期期末成绩', 2, 'file:teaching:exam:final:score', '0', 'admin', NOW(), '期末考试成绩'),
(1512, '2020 第一学期期末英语听力', 1361, '/02_教学资源/各学期教务处工作文件夹/2020学年第一学期/2020第一学期考试/2020第一学期期末考试/2020 第一学期期末英语听力', 3, 'file:teaching:exam:final:english', '0', 'admin', NOW(), '期末英语听力资料'),
(1513, '2020 第一学期期末试卷分析', 1361, '/02_教学资源/各学期教务处工作文件夹/2020学年第一学期/2020第一学期考试/2020第一学期期末考试/2020 第一学期期末试卷分析', 4, 'file:teaching:exam:final:analysis', '0', 'admin', NOW(), '期末考试试卷分析'),
(1514, '2020 第一学期期末语文试题', 1361, '/02_教学资源/各学期教务处工作文件夹/2020学年第一学期/2020第一学期考试/2020第一学期期末考试/2020 第一学期期末语文试题', 5, 'file:teaching:exam:final:chinese', '0', 'admin', NOW(), '期末语文试题'),
(1515, '答案', 1361, '/02_教学资源/各学期教务处工作文件夹/2020学年第一学期/2020第一学期考试/2020第一学期期末考试/答案', 6, 'file:teaching:exam:final:ans', '0', 'admin', NOW(), '期末考试答案'),
(1516, '2021.1 期末二年级成绩登记', 1361, '/02_教学资源/各学期教务处工作文件夹/2020学年第一学期/2020第一学期考试/2020第一学期期末考试/2021.1 期末二年级成绩登记', 7, 'file:teaching:exam:final:grade2', '0', 'admin', NOW(), '二年级期末成绩登记'),
(1517, '2021.1 考务会材料', 1361, '/02_教学资源/各学期教务处工作文件夹/2020学年第一学期/2020第一学期考试/2020第一学期期末考试/2021.1 考务会材料', 8, 'file:teaching:exam:final:kaowu', '0', 'admin', NOW(), '考务会材料'),
(1518, '五东四年级抽测座位表', 1361, '/02_教学资源/各学期教务处工作文件夹/2020学年第一学期/2020第一学期考试/2020第一学期期末考试/五东四年级抽测座位表', 9, 'file:teaching:exam:final:zuowei', '0', 'admin', NOW(), '四年级抽测座位表'),
(1519, '学生名单', 1361, '/02_教学资源/各学期教务处工作文件夹/2020学年第一学期/2020第一学期考试/2020第一学期期末考试/学生名单', 10, 'file:teaching:exam:final:students', '0', 'admin', NOW(), '学生名单'),
(1520, '数学草稿纸使用', 1361, '/02_教学资源/各学期教务处工作文件夹/2020学年第一学期/2020第一学期考试/2020第一学期期末考试/数学草稿纸使用', 11, 'file:teaching:exam:final:caogao', '0', 'admin', NOW(), '数学草稿纸使用记录'),
(1521, '科学实验照片', 1361, '/02_教学资源/各学期教务处工作文件夹/2020学年第一学期/2020第一学期考试/2020第一学期期末考试/科学实验照片', 12, 'file:teaching:exam:final:photo', '0', 'admin', NOW(), '科学实验照片'),
(1522, '道德与法治成绩', 1361, '/02_教学资源/各学期教务处工作文件夹/2020学年第一学期/2020第一学期考试/2020第一学期期末考试/道德与法治成绩', 13, 'file:teaching:exam:final:daode', '0', 'admin', NOW(), '道德与法治成绩');

-- 民族风俗我知道——主题班会 的子目录 (parent_id=1370)
INSERT INTO sys_file_dir (dir_id, dir_name, parent_id, path, order_num, perms, status, create_by, create_time, remark) VALUES
(1530, '照片', 1370, '/02_教学资源/各学期教务处工作文件夹/2020学年第一学期/创金牌/民族风俗我知道——主题班会/照片', 1, 'file:teaching:chuangjin:minzu:photo', '0', 'admin', NOW(), '主题班会照片');

-- 数学（课题申报） 的子目录 (parent_id=1420)
INSERT INTO sys_file_dir (dir_id, dir_name, parent_id, path, order_num, perms, status, create_by, create_time, remark) VALUES
(1540, '廖斌斌', 1420, '/02_教学资源/各学期教务处工作文件夹/2020学年第一学期/长坑小学2021课题申报/数学/廖斌斌', 1, 'file:teaching:keti:math:liaobinbin', '0', 'admin', NOW(), '廖斌斌数学课题'),
(1541, '李佩蓓', 1420, '/02_教学资源/各学期教务处工作文件夹/2020学年第一学期/长坑小学2021课题申报/数学/李佩蓓', 2, 'file:teaching:keti:math:lipeibei', '0', 'admin', NOW(), '李佩蓓数学课题'),
(1542, '胡耀钦', 1420, '/02_教学资源/各学期教务处工作文件夹/2020学年第一学期/长坑小学2021课题申报/数学/胡耀钦', 3, 'file:teaching:keti:math:huyaoqin', '0', 'admin', NOW(), '胡耀钦数学课题'),
(1543, '马旭东', 1420, '/02_教学资源/各学期教务处工作文件夹/2020学年第一学期/长坑小学2021课题申报/数学/马旭东', 4, 'file:teaching:keti:math:maxudong', '0', 'admin', NOW(), '马旭东数学课题');

-- 家庭教育优秀案例 的子目录 (parent_id=1430)
INSERT INTO sys_file_dir (dir_id, dir_name, parent_id, path, order_num, perms, status, create_by, create_time, remark) VALUES
(1550, '徐 晓 长坑小学', 1430, '/02_教学资源/各学期教务处工作文件夹/2020学年第一学期/（新）长坑小学家庭教育三优评比资料/家庭教育优秀案例/徐 晓 长坑小学', 1, 'file:teaching:jiating:anli:xuxiao', '0', 'admin', NOW(), '徐晓家庭教育案例'),
(1551, '胡耀钦长坑小学', 1430, '/02_教学资源/各学期教务处工作文件夹/2020学年第一学期/（新）长坑小学家庭教育三优评比资料/家庭教育优秀案例/胡耀钦长坑小学', 2, 'file:teaching:jiating:anli:huyaoqin', '0', 'admin', NOW(), '胡耀钦家庭教育案例');

-- ============================================
-- 六级目录
-- ============================================

-- 2020第一学期期中考试（五级） 的子目录 (parent_id=1500)
INSERT INTO sys_file_dir (dir_id, dir_name, parent_id, path, order_num, perms, status, create_by, create_time, remark) VALUES
(1600, '2020第一学期期中考试', 1500, '/02_教学资源/各学期教务处工作文件夹/2020学年第一学期/2020第一学期考试/2020第一学期期中考试资料/2020第一学期期中考试/2020第一学期期中考试', 1, 'file:teaching:exam:midterm:exam:detail', '0', 'admin', NOW(), '期中考试详细资料');

-- 2020 第一学期期末参考答案 的子目录 (parent_id=1510)
INSERT INTO sys_file_dir (dir_id, dir_name, parent_id, path, order_num, perms, status, create_by, create_time, remark) VALUES
(1610, '科学', 1510, '/02_教学资源/各学期教务处工作文件夹/2020学年第一学期/2020第一学期考试/2020第一学期期末考试/2020 第一学期期末参考答案/科学', 1, 'file:teaching:exam:final:answer:science', '0', 'admin', NOW(), '科学参考答案'),
(1611, '英语', 1510, '/02_教学资源/各学期教务处工作文件夹/2020学年第一学期/2020第一学期考试/2020第一学期期末考试/2020 第一学期期末参考答案/英语', 2, 'file:teaching:exam:final:answer:english', '0', 'admin', NOW(), '英语参考答案');

-- 2021.1 考务会材料 的子目录 (parent_id=1517)
INSERT INTO sys_file_dir (dir_id, dir_name, parent_id, path, order_num, perms, status, create_by, create_time, remark) VALUES
(1620, '四年级考场编排', 1517, '/02_教学资源/各学期教务处工作文件夹/2020学年第一学期/2020第一学期考试/2020第一学期期末考试/2021.1 考务会材料/四年级考场编排', 1, 'file:teaching:exam:final:kaowu:grade4', '0', 'admin', NOW(), '四年级考场编排');

-- 学生名单（期末考试下的） 的子目录 (parent_id=1519)
INSERT INTO sys_file_dir (dir_id, dir_name, parent_id, path, order_num, perms, status, create_by, create_time, remark) VALUES
(1630, '202009学生名册', 1519, '/02_教学资源/各学期教务处工作文件夹/2020学年第一学期/2020第一学期考试/2020第一学期期末考试/学生名单/202009学生名册', 1, 'file:teaching:exam:final:students:202009', '0', 'admin', NOW(), '2020年9月学生名册'),
(1631, '各班名单（有身份证）', 1519, '/02_教学资源/各学期教务处工作文件夹/2020学年第一学期/2020第一学期考试/2020第一学期期末考试/学生名单/各班名单（有身份证）', 2, 'file:teaching:exam:final:students:idcard', '0', 'admin', NOW(), '各班学生名单（含身份证号）');

-- 科学实验照片 的子目录 (parent_id=1521)
INSERT INTO sys_file_dir (dir_id, dir_name, parent_id, path, order_num, perms, status, create_by, create_time, remark) VALUES
(1640, '三年级', 1521, '/02_教学资源/各学期教务处工作文件夹/2020学年第一学期/2020第一学期考试/2020第一学期期末考试/科学实验照片/三年级', 1, 'file:teaching:exam:final:photo:grade3', '0', 'admin', NOW(), '三年级科学实验照片'),
(1641, '五年级', 1521, '/02_教学资源/各学期教务处工作文件夹/2020学年第一学期/2020第一学期考试/2020第一学期期末考试/科学实验照片/五年级', 2, 'file:teaching:exam:final:photo:grade5', '0', 'admin', NOW(), '五年级科学实验照片'),
(1642, '六年级', 1521, '/02_教学资源/各学期教务处工作文件夹/2020学年第一学期/2020第一学期考试/2020第一学期期末考试/科学实验照片/六年级', 3, 'file:teaching:exam:final:photo:grade6', '0', 'admin', NOW(), '六年级科学实验照片'),
(1643, '四年级', 1521, '/02_教学资源/各学期教务处工作文件夹/2020学年第一学期/2020第一学期考试/2020第一学期期末考试/科学实验照片/四年级', 4, 'file:teaching:exam:final:photo:grade4', '0', 'admin', NOW(), '四年级科学实验照片'),
(1644, '长坑实验照片', 1521, '/02_教学资源/各学期教务处工作文件夹/2020学年第一学期/2020第一学期考试/2020第一学期期末考试/科学实验照片/长坑实验照片', 5, 'file:teaching:exam:final:photo:changkeng', '0', 'admin', NOW(), '长坑实验照片');

-- ============================================
-- 给管理员角色分配所有新增目录的权限
-- ============================================
INSERT INTO sys_role_file_dir (role_id, dir_id, can_view, can_download, can_upload, can_delete)
SELECT 1, dir_id, 1, 1, 1, 1 
FROM sys_file_dir 
WHERE dir_id >= 1000;
