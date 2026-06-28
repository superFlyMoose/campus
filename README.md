# 校园活动管理系统

一个面向校园场景的活动管理系统，支持游客浏览、用户注册登录、活动查看与报名、管理员活动管理与用户管理。

## 项目简介

本项目基于 Spring Boot 构建，提供面向使用者的校园活动管理能力，适合用于课程设计、毕业设计演示或基础业务系统练习。

## 技术栈

- Java 17
- Spring Boot 3.3.12
- Spring MVC
- Spring Security
- Thymeleaf
- MyBatis-Plus
- MySQL
- Redis
- RabbitMQ
- Maven

可参考 [`pom.xml`](pom.xml) 查看依赖配置。

## 运行前准备

启动项目之前，请先准备以下环境：

- JDK 17
- Maven
- MySQL 8.x
- Redis 6.x
- RabbitMQ 4.3.2（Erlang版本为27.x）

当前默认端口为 [`8080`](src/main/resources/application.yml:19)。

## 配置说明

默认启用的是 [`dev`](src/main/resources/application.yml:6) 环境，主要配置文件如下：

- [`src/main/resources/application.yml`](src/main/resources/application.yml)
- [`src/main/resources/application-dev.yml`](src/main/resources/application-dev.yml)

关键配置包括：

- 数据库地址：[`jdbc:mysql://localhost:3306/campus_activity_management`](src/main/resources/application-dev.yml:3)
- 数据库用户名：[`root`](src/main/resources/application-dev.yml:4)
- 数据库密码：[`123456`](src/main/resources/application-dev.yml:5)
- Redis 地址：[`192.168.64.135:6379`](src/main/resources/application-dev.yml:10)
- Redis 密码：[`123456`](src/main/resources/application-dev.yml:12)
- RabbitMQ 地址：[`localhost:5672`](src/main/resources/application-dev.yml:17)

如果你的本地环境与上述配置不一致，请先修改 [`src/main/resources/application-dev.yml`](src/main/resources/application-dev.yml) 后再启动。

## 数据库初始化

1. 在 MySQL 中创建数据库：`campus_activity_management`
2. 执行初始化脚本 [`campus_activity_management_db.sql`](campus_activity_management_db.sql)
3. 确认表与初始数据导入成功

初始化脚本中已写入默认测试账号，见 [`campus_activity_management_db.sql`](campus_activity_management_db.sql:49)。

## 启动方式

### 方式一：使用 Maven Wrapper

在项目根目录执行：

```bash
mvnw.cmd spring-boot:run
```

### 方式二：使用本地 Maven

```bash
mvn spring-boot:run
```

### 方式三：在 IDE 中启动

直接运行启动类 [`CampusActivityManagementApplication`](src/main/java/com/campus/management/CampusActivityManagementApplication.java:1)。

启动成功后，访问：

- 首页：`http://localhost:8080/`
- 登录页：`http://localhost:8080/login`
- 注册页：`http://localhost:8080/register`

## 登录说明

系统使用 Spring Security 表单登录，登录入口见 [`/login`](src/main/java/com/campus/management/config/SecurityConfig.java:33)。

### 默认测试账号

这是本项目最需要明确说明的信息：

- 管理员账号：`admin`
- 普通用户账号：`user`
- **admin 的明文密码是：`123456`**
- **user 的明文密码是：`123456`**

说明依据：

- 默认密码配置见 [`app.default-password: 123456`](src/main/resources/application.yml:38)
- 初始化账号见 [`admin`](campus_activity_management_db.sql:51) 与 [`user`](campus_activity_management_db.sql:52)

## 使用流程

### 普通使用者

1. 打开首页 [`/`](src/main/java/com/campus/management/controller/HomeController.java:30)
2. 进入登录页 [`/login`](src/main/java/com/campus/management/controller/HomeController.java:46)
3. 使用测试账号 [`user`](campus_activity_management_db.sql:52) 登录，密码为 `123456`
4. 浏览活动列表 [`/activities`](src/main/java/com/campus/management/controller/ActivityController.java:44)
5. 查看活动详情 [`/activities/{id}`](src/main/java/com/campus/management/controller/ActivityController.java:62)
6. 提交活动报名 [`/registrations/activity/{activityId}`](src/main/java/com/campus/management/controller/RegistrationController.java:25)
7. 在个人中心 [`/user/profile`](src/main/java/com/campus/management/controller/UserController.java:27) 查看个人信息

### 管理员

1. 进入登录页 [`/login`](src/main/java/com/campus/management/controller/HomeController.java:46)
2. 使用管理员账号 [`admin`](campus_activity_management_db.sql:51) 登录，密码为 `123456`
3. 进入管理控制台 [`/admin/dashboard`](src/main/java/com/campus/management/controller/AdminController.java:40)
4. 查看与管理活动、报名与统计信息
5. 进入用户管理页 [`/admin/users`](src/main/java/com/campus/management/controller/AdminController.java:62)
6. 进行新增、编辑、删除用户等操作

## 主要页面说明

### 公共页面

- 首页：[`src/main/resources/templates/index.html`](src/main/resources/templates/index.html)
- 登录页：[`src/main/resources/templates/login.html`](src/main/resources/templates/login.html)
- 注册页：[`src/main/resources/templates/register.html`](src/main/resources/templates/register.html)

### 活动相关页面

- 活动列表：[`src/main/resources/templates/activity/list.html`](src/main/resources/templates/activity/list.html)
- 活动详情：[`src/main/resources/templates/activity/detail.html`](src/main/resources/templates/activity/detail.html)
- 活动表单：[`src/main/resources/templates/activity/form.html`](src/main/resources/templates/activity/form.html)

### 管理端页面

- 管理控制台：[`src/main/resources/templates/admin/dashboard.html`](src/main/resources/templates/admin/dashboard.html)
- 用户表单：[`src/main/resources/templates/admin/user-form.html`](src/main/resources/templates/admin/user-form.html)

### 用户页面

- 个人中心：[`src/main/resources/templates/user/profile.html`](src/main/resources/templates/user/profile.html)

## 权限说明

权限控制定义见 [`SecurityConfig`](src/main/java/com/campus/management/config/SecurityConfig.java:15)：

- 游客可访问：首页、登录、注册、静态资源
- 管理员可访问：[`/admin/**`](src/main/java/com/campus/management/config/SecurityConfig.java:28) 与 [`/activities/admin/**`](src/main/java/com/campus/management/config/SecurityConfig.java:28)
- 普通用户与管理员可访问：[`/user/**`](src/main/java/com/campus/management/config/SecurityConfig.java:29)、[`/activities/**`](src/main/java/com/campus/management/config/SecurityConfig.java:29)、[`/registrations/**`](src/main/java/com/campus/management/config/SecurityConfig.java:29)

## 常见问题

### 1. 无法启动项目

请重点检查：

- MySQL 是否已启动
- Redis 是否可连接
- RabbitMQ 是否已启动
- [`src/main/resources/application-dev.yml`](src/main/resources/application-dev.yml) 中的地址、账号、密码是否正确

### 2. 登录失败

请确认：

- 已执行 [`campus_activity_management_db.sql`](campus_activity_management_db.sql)
- 使用的账号是否为 [`admin`](campus_activity_management_db.sql:51) 或 [`user`](campus_activity_management_db.sql:52)
- **两个账号的明文密码是否都输入为 `123456`**
- 若依旧无法登录，则在[`CampusActivityManagementApplicationTests`](src/test/java/com/campus/management/CampusActivityManagementApplicationTests.java)测试类中运行测试方法，获取控制台中的密文手动替换数据库中的密码，再尝试登录
### 3. 页面能打开但部分功能不可用

可能原因包括：

- 当前账号权限不足
- Redis 或 RabbitMQ 未正常启动
- 数据库中缺少初始化数据

## 目录说明

- [`src/main/java`](src/main/java)：后端 Java 代码
- [`src/main/resources/templates`](src/main/resources/templates)：页面模板
- [`src/main/resources/application.yml`](src/main/resources/application.yml)：公共配置
- [`src/main/resources/application-dev.yml`](src/main/resources/application-dev.yml)：开发环境配置
- [`campus_activity_management_db.sql`](campus_activity_management_db.sql)：数据库初始化脚本
- [`uploads`](uploads)：上传文件目录

## 特别说明

为方便其他人快速体验系统，请务必记住：

- **admin 的明文密码是 `123456`**
- **user 的明文密码是 `123456`**

以上两项信息已经在初始化数据与默认密码配置中得到对应体现，分别见 [`campus_activity_management_db.sql`](campus_activity_management_db.sql:51) 、[`campus_activity_management_db.sql`](campus_activity_management_db.sql:52) 与 [`src/main/resources/application.yml`](src/main/resources/application.yml:38)。
