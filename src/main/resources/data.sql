INSERT INTO sys_user (id, username, password, real_name, avatar, role, is_deleted, create_time, update_time)
VALUES
    (1, 'admin', '$2a$10$e0NRP4sQx0mVxS1l3m5k4.e7xR4Q4mG0k2QWJkK6K0N8Vx1Y3qN5O', '系统管理员', '', 'ADMIN', 0, NOW(), NOW()),
    (2, 'user', '$2a$10$wJ7J3K6c1eE9T8sQyG3F3uR4zWnR2xVq8Q1h0vL2dM7pN5bC6aD8e', '普通用户', '', 'USER', 0, NOW(), NOW());
