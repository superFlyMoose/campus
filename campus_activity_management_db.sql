CREATE DATABASE IF NOT EXISTS campus_activity_management;
USE campus_activity_management;

CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(100) NOT NULL,
    real_name VARCHAR(50),
    avatar VARCHAR(255),
    role VARCHAR(20) NOT NULL,
    is_deleted TINYINT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS activity (
    id BIGINT PRIMARY KEY,
    title VARCHAR(100) NOT NULL,
    content TEXT,
    location VARCHAR(100),
    start_time DATETIME,
    end_time DATETIME,
    max_people INT DEFAULT 0,
    current_people INT DEFAULT 0,
    image_path VARCHAR(255),
    status TINYINT DEFAULT 0,
    is_deleted TINYINT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS activity_registration (
    id BIGINT PRIMARY KEY,
    activity_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    registration_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    is_deleted TINYINT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS sys_file (
    id BIGINT PRIMARY KEY,
    file_name VARCHAR(255) NOT NULL,
    file_path VARCHAR(255) NOT NULL,
    file_size BIGINT DEFAULT 0,
    upload_user_id BIGINT,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO sys_user (id, username, password, real_name, avatar, role, is_deleted, create_time, update_time)
VALUES
    (1, 'admin', '$2a$10$RYzN7MXGFx07X/P57KA7BOZ011.uErGLhA/RTasds4irs0BaD6yT6', '系统管理员', '/uploads/avatar/5e463ee4-433b-4052-b8ca-b21a4d479635.jpg', 'ADMIN', 0, NOW(), NOW()),
    (2, 'user', '$2a$10$RYzN7MXGFx07X/P57KA7BOZ011.uErGLhA/RTasds4irs0BaD6yT6', '普通用户', '/uploads/avatar/c42a8514-6a64-4711-9018-01ac308580c6.jpg', 'USER', 0, NOW(), NOW());
INSERT INTO activity (id, title, content, location, start_time, end_time, max_people, current_people, image_path, status, is_deleted, create_time, update_time)
VALUES
    (
        2071222277441826818,
        '校园美食展览',
        '汇聚校园食堂及学生社团特色美食，现场提供免费试吃、美食评选和制作展示，欢迎全校师生积极参与。',
        '学生食堂广场',
        '2026-09-01 18:00:00',
        '2026-09-01 20:00:00',
        100,
        0,
        '/uploads/activity/956a7132-2f15-4343-a6b2-c9efe20ccf30.jpg',
        0,
        0,
        NOW(),
        NOW()
    ),
    (
        2071222864908627970,
        '羽毛球友谊赛',
        '面向全校学生开展羽毛球友谊赛，以球会友，丰富校园体育文化生活，比赛采用男女混合双打形式。',
        '体育中心羽毛球馆',
        '2026-06-28 08:00:00',
        '2026-06-28 12:00:00',
        20,
        0,
        '/uploads/activity/3856a26a-e7c9-4193-9ede-d8c326862eb8.png',
        0,
        0,
        NOW(),
        NOW()
    ),
    (
        2071225626258026498,
        '宿舍楼卫生清洁志愿活动',
        '组织志愿者开展宿舍公共区域卫生清洁，对楼道、宣传栏及公共设施进行集中整理，共建文明宿舍环境。',
        '宿舍楼',
        '2026-09-01 08:00:00',
        '2026-09-01 10:00:00',
        2,
        0,
        '/uploads/activity/25da6a0c-809a-4ea1-a353-e3ce493af682.jpg',
        0,
        0,
        NOW(),
        NOW()
    ),
    (
        2071223709461098497,
        '图书整理志愿服务',
        '协助图书馆工作人员完成图书分类、上架及阅览区整理工作，培养志愿服务精神，提高图书管理效率。',
        '学校图书馆',
        '2026-09-01 13:00:00',
        '2026-09-01 17:00:00',
        1,
        1,
        '/uploads/activity/23ebfeb2-c7e6-492c-87e2-a2befe5fdbea.jpg',
        0,
        0,
        NOW(),
        NOW()
    ),
    (
        2071224357128744961,
        'test1',
        'test1',
        'test1',
        '2026-06-27 21:28:00',
        '2026-06-27 21:30:00',
        1,
        0,
        '',
        0,
        0,
        NOW(),
        NOW()
    ),
    (
        2071224472404996098,
        'test2',
        'test2',
        'test2',
        '2026-06-26 21:28:00',
        '2026-06-26 21:30:00',
        1,
        0,
        '',
        0,
        0,
        NOW(),
        NOW()
    ),
    (
        2071224596388622338,
        'test3',
        'test3',
        'test3',
        '2026-06-25 21:28:00',
        '2026-06-25 21:30:00',
        1,
        0,
        '',
        0,
        0,
        NOW(),
        NOW()
    ),
    (
        2071224740790120449,
        'test4',
        'test4',
        'test4',
        '2026-06-24 21:28:00',
        '2026-06-24 21:30:00',
        1,
        0,
        '',
        0,
        0,
        NOW(),
        NOW()
    );
INSERT INTO activity_registration (id,activity_id,user_id,registration_time,is_deleted)
VALUES
    (
        2071224915369635842,
        2071223709461098497,
        2,
        '2026-06-28 21:31:16',
        0
    ),
    (
        2071225680960139266,
        2071225626258026498,
        1,
        '2026-06-28 21:34:19',
        0
    );
INSERT INTO sys_file (id,file_name,file_path,file_size,upload_user_id,create_time)
VALUES
    (
        2071218353351667714,
        'IU.jpg',
        '/uploads/avatar/5e463ee4-433b-4052-b8ca-b21a4d479635.jpg',
        3865,
        1,
        '2026-06-28 21:05:11'
    ),
    (
        2071218974200934402,
        'docker.jpg',
        '/uploads/avatar/c42a8514-6a64-4711-9018-01ac308580c6.jpg',
        26018,
        2,
        '2026-06-28 21:07:39'
    ),
    (
        2071222277374717954,
        '美食.jpg',
        '/uploads/activity/956a7132-2f15-4343-a6b2-c9efe20ccf30.jpg',
        38110,
        0,
        '2026-06-28 21:20:47'
    ),
    (
        2071222864812158978,
        '羽毛球.png',
        '/uploads/activity/3856a26a-e7c9-4193-9ede-d8c326862eb8.png',
        6120,
        0,
        '2026-06-28 21:23:07'
    ),
    (
        2071223709406572545,
        '图书.jpg',
        '/uploads/activity/23ebfeb2-c7e6-492c-87e2-a2befe5fdbea.jpg',
        63685,
        0,
        '2026-06-28 21:26:28'
    ),
    (
        2071225626186723329,
        '宿舍.jpg',
        '/uploads/activity/25da6a0c-809a-4ea1-a353-e3ce493af682.jpg',
        36758,
        0,
        '2026-06-28 21:34:05'
    );
