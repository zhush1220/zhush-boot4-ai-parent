/*
 Navicat Premium Data Transfer

 Source Server         : localhost
 Source Server Type    : MySQL
 Source Server Version : 80028
 Source Host           : localhost:3306
 Source Schema         : zhush_union

 Target Server Type    : MySQL
 Target Server Version : 80028
 File Encoding         : 65001

 Date: 07/08/2026 16:05:23
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for member_social_user
-- ----------------------------
DROP TABLE IF EXISTS `member_social_user`;
CREATE TABLE `member_social_user`  (
  `id` bigint NOT NULL COMMENT '主键ID（雪花算法）',
  `user_id` bigint NOT NULL COMMENT '会员用户ID',
  `social_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '第三方类型：WECHAT_MINI-微信小程序 WECHAT_MP-微信公众号 WECHAT_OPEN-开放平台 ALIPAY APPLE ...',
  `open_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '第三方用户唯一标识（openid）',
  `union_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '第三方 UnionId（同主体多应用打通）',
  `session_key` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '微信 session_key（小程序）',
  `nickname` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '第三方昵称',
  `avatar` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '第三方头像',
  `raw_info` varchar(2048) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '第三方原始信息（JSON）',
  `create_by` bigint NULL DEFAULT NULL COMMENT '创建人ID',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint NULL DEFAULT NULL COMMENT '更新人ID',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `is_delete` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-正常 1-已删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_type_openid`(`social_type` ASC, `open_id` ASC, `is_delete` ASC) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_union_id`(`union_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '会员第三方授权表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of member_social_user
-- ----------------------------

-- ----------------------------
-- Table structure for member_user
-- ----------------------------
DROP TABLE IF EXISTS `member_user`;
CREATE TABLE `member_user`  (
  `id` bigint NOT NULL COMMENT '主键ID（雪花算法）',
  `mobile` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '手机号',
  `password` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '登录密码（BCrypt，未设置为空）',
  `nickname` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '昵称',
  `avatar` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '头像地址',
  `gender` tinyint NOT NULL DEFAULT 0 COMMENT '性别：0-未知 1-男 2-女',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态：0-禁用 1-启用',
  `register_ip` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '注册IP',
  `register_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '注册来源：WECHAT_MINI-小程序 PASSWORD-账号密码',
  `login_ip` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '最后登录IP',
  `login_date` datetime NULL DEFAULT NULL COMMENT '最后登录时间',
  `create_by` bigint NULL DEFAULT NULL COMMENT '创建人ID',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint NULL DEFAULT NULL COMMENT '更新人ID',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `is_delete` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-正常 1-已删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_mobile`(`mobile` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '会员用户表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of member_user
-- ----------------------------

-- ----------------------------
-- Table structure for sys_admin_user
-- ----------------------------
DROP TABLE IF EXISTS `sys_admin_user`;
CREATE TABLE `sys_admin_user`  (
  `id` bigint NOT NULL COMMENT '主键ID（雪花算法）',
  `username` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '登录账号',
  `password` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '登录密码（BCrypt 加密）',
  `nickname` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '昵称',
  `real_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '真实姓名',
  `avatar` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '头像地址',
  `mobile` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '手机号',
  `email` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '邮箱',
  `dept_id` bigint NULL DEFAULT NULL COMMENT '部门ID（预留）',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态：0-禁用 1-启用',
  `login_ip` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '最后登录IP',
  `login_date` datetime NULL DEFAULT NULL COMMENT '最后登录时间',
  `remark` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '备注',
  `create_by` bigint NULL DEFAULT NULL COMMENT '创建人ID',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint NULL DEFAULT NULL COMMENT '更新人ID',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `is_delete` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-正常 1-已删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_username`(`username` ASC, `is_delete` ASC) USING BTREE,
  INDEX `idx_mobile`(`mobile` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '管理员用户表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_admin_user
-- ----------------------------
INSERT INTO `sys_admin_user` VALUES (1, 'admin', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '超级管理员', '超级管理员', '', '', '', NULL, 1, '', NULL, '内置超级管理员', NULL, NULL, NULL, NULL, 0);

-- ----------------------------
-- Table structure for sys_admin_user_role
-- ----------------------------
DROP TABLE IF EXISTS `sys_admin_user_role`;
CREATE TABLE `sys_admin_user_role`  (
  `id` bigint NOT NULL COMMENT '主键ID（雪花算法）',
  `user_id` bigint NOT NULL COMMENT '管理员用户ID',
  `role_id` bigint NOT NULL COMMENT '角色ID',
  `create_by` bigint NULL DEFAULT NULL COMMENT '创建人ID',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint NULL DEFAULT NULL COMMENT '更新人ID',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `is_delete` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-正常 1-已删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_role_id`(`role_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '管理员角色关联表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_admin_user_role
-- ----------------------------
INSERT INTO `sys_admin_user_role` VALUES (1, 1, 1, NULL, NULL, NULL, NULL, 0);

-- ----------------------------
-- Table structure for sys_dept
-- ----------------------------
DROP TABLE IF EXISTS `sys_dept`;
CREATE TABLE `sys_dept`  (
  `id` bigint NOT NULL COMMENT '主键ID（雪花算法）',
  `parent_id` bigint NOT NULL DEFAULT 0 COMMENT '父部门ID（根为0）',
  `name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '部门名称',
  `sort` int NOT NULL DEFAULT 0 COMMENT '显示顺序',
  `leader_user_id` bigint NULL DEFAULT NULL COMMENT '负责人用户ID',
  `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '联系电话',
  `email` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '邮箱',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态：0-禁用 1-启用',
  `create_by` bigint NULL DEFAULT NULL COMMENT '创建人ID',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint NULL DEFAULT NULL COMMENT '更新人ID',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `is_delete` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-正常 1-已删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_parent_id`(`parent_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '部门表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_dept
-- ----------------------------
INSERT INTO `sys_dept` VALUES (1, 0, '总公司', 0, NULL, '', '', 1, NULL, '2026-08-07 16:04:58', NULL, NULL, 0);

-- ----------------------------
-- Table structure for sys_menu
-- ----------------------------
DROP TABLE IF EXISTS `sys_menu`;
CREATE TABLE `sys_menu`  (
  `id` bigint NOT NULL COMMENT '主键ID（雪花算法）',
  `parent_id` bigint NOT NULL DEFAULT 0 COMMENT '父菜单ID（根为0）',
  `name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '菜单名称',
  `type` tinyint NOT NULL COMMENT '类型：1-目录 2-菜单 3-按钮',
  `permission` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '权限标识（如 system:user:add）',
  `path` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '路由地址',
  `component` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '前端组件路径',
  `icon` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '菜单图标',
  `sort` int NOT NULL DEFAULT 0 COMMENT '显示顺序',
  `visible` tinyint NOT NULL DEFAULT 1 COMMENT '是否可见：0-隐藏 1-显示',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态：0-禁用 1-启用',
  `create_by` bigint NULL DEFAULT NULL COMMENT '创建人ID',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint NULL DEFAULT NULL COMMENT '更新人ID',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `is_delete` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-正常 1-已删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_parent_id`(`parent_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '菜单权限表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_menu
-- ----------------------------

-- ----------------------------
-- Table structure for sys_oauth2_client
-- ----------------------------
DROP TABLE IF EXISTS `sys_oauth2_client`;
CREATE TABLE `sys_oauth2_client`  (
  `id` bigint NOT NULL COMMENT '主键ID（雪花算法）',
  `client_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '客户端标识',
  `client_secret` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '客户端密钥（BCrypt 存储，仅创建时明文回显一次）',
  `client_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '应用名称（授权确认页展示）',
  `logo` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '应用图标URL（授权确认页展示）',
  `grant_types` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '允许的授权类型，逗号分隔（authorization_code,refresh_token,client_credentials）',
  `scopes` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '允许申请的 scope，逗号分隔（openid,userinfo,roles,...）',
  `redirect_uris` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '回调地址白名单，逗号分隔，精确匹配',
  `access_token_timeout` bigint NULL DEFAULT NULL COMMENT 'access_token 有效期（秒），NULL 用全局默认',
  `refresh_token_timeout` bigint NULL DEFAULT NULL COMMENT 'refresh_token 有效期（秒），NULL 用全局默认',
  `qps_limit` int NULL DEFAULT NULL COMMENT '开放 API 限流 QPS，NULL 用全局默认',
  `daily_quota` int NULL DEFAULT NULL COMMENT '开放 API 日调用配额，NULL 用全局默认',
  `notify_url` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '回调通知地址（预留，事件推送用）',
  `auto_confirm` tinyint NOT NULL DEFAULT 0 COMMENT '是否免授权确认页：0-否 1-是（自有信任应用）',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态：0-停用 1-启用（停用后存量令牌立即失效）',
  `remark` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '备注',
  `create_by` bigint NULL DEFAULT NULL COMMENT '创建人ID',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint NULL DEFAULT NULL COMMENT '更新人ID',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `is_delete` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-正常 1-已删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_client_id`(`client_id` ASC, `is_delete` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'OAuth2 客户端表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_oauth2_client
-- ----------------------------

-- ----------------------------
-- Table structure for sys_open_api_log
-- ----------------------------
DROP TABLE IF EXISTS `sys_open_api_log`;
CREATE TABLE `sys_open_api_log`  (
  `id` bigint NOT NULL COMMENT '主键ID（雪花算法）',
  `client_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '客户端标识（鉴权失败无法解析时为空）',
  `login_id` bigint NULL DEFAULT NULL COMMENT '授权用户ID（应用级调用为 NULL）',
  `request_method` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '请求方式（GET/POST/PUT/DELETE）',
  `request_uri` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '请求URI',
  `status` int NOT NULL DEFAULT 0 COMMENT 'HTTP 响应状态码',
  `error_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT 'OAuth2 协议错误码（invalid_token/insufficient_scope...，成功为空）',
  `cost_millis` bigint NOT NULL DEFAULT 0 COMMENT '执行耗时（毫秒）',
  `client_ip` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '调用方IP',
  `call_time` datetime NULL DEFAULT NULL COMMENT '调用时间',
  `create_by` bigint NULL DEFAULT NULL COMMENT '创建人ID',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint NULL DEFAULT NULL COMMENT '更新人ID',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `is_delete` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-正常 1-已删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_client_id`(`client_id` ASC) USING BTREE,
  INDEX `idx_call_time`(`call_time` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '开放 API 调用日志表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_open_api_log
-- ----------------------------

-- ----------------------------
-- Table structure for sys_operate_log
-- ----------------------------
DROP TABLE IF EXISTS `sys_operate_log`;
CREATE TABLE `sys_operate_log`  (
  `id` bigint NOT NULL COMMENT '主键ID（雪花算法）',
  `trace_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '链路追踪ID（预留）',
  `user_id` bigint NULL DEFAULT NULL COMMENT '操作人ID（未登录为 NULL）',
  `user_type` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '操作人类型：admin-管理端 member-会员端',
  `module` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '所属模块（如 用户管理）',
  `name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '操作名称（如 新增管理员）',
  `type` tinyint NOT NULL DEFAULT 0 COMMENT '操作类型：0-其它 1-查询 2-新增 3-修改 4-删除 5-导出 6-导入 7-授权',
  `request_method` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '请求方式（GET/POST/PUT/DELETE）',
  `request_url` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '请求地址',
  `user_ip` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '操作人IP',
  `user_agent` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '浏览器UA',
  `java_method` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '目标Java方法（类全名.方法名）',
  `java_method_args` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '方法入参（JSON，超长截断）',
  `start_time` datetime NULL DEFAULT NULL COMMENT '操作开始时间',
  `duration` int NOT NULL DEFAULT 0 COMMENT '执行耗时（毫秒）',
  `result_code` int NOT NULL DEFAULT 0 COMMENT '结果码：0-成功，其余为错误码',
  `result_msg` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '结果提示信息（失败时为异常信息）',
  `result_data` varchar(4000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '返回结果（JSON，超长截断）',
  `create_by` bigint NULL DEFAULT NULL COMMENT '创建人ID',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint NULL DEFAULT NULL COMMENT '更新人ID',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `is_delete` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-正常 1-已删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user`(`user_type` ASC, `user_id` ASC) USING BTREE,
  INDEX `idx_module`(`module` ASC) USING BTREE,
  INDEX `idx_start_time`(`start_time` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '操作日志表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_operate_log
-- ----------------------------

-- ----------------------------
-- Table structure for sys_role
-- ----------------------------
DROP TABLE IF EXISTS `sys_role`;
CREATE TABLE `sys_role`  (
  `id` bigint NOT NULL COMMENT '主键ID（雪花算法）',
  `name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '角色名称',
  `code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '角色编码（如 admin、ops）',
  `sort` int NOT NULL DEFAULT 0 COMMENT '显示顺序',
  `data_scope` tinyint NOT NULL DEFAULT 1 COMMENT '数据权限：1-全部 2-本部门 3-本部门及以下 4-仅本人 5-自定义（预留）',
  `data_scope_dept_ids` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '自定义数据范围的部门ID集合（逗号分隔，data_scope=5 时生效）',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态：0-禁用 1-启用',
  `remark` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '备注',
  `create_by` bigint NULL DEFAULT NULL COMMENT '创建人ID',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint NULL DEFAULT NULL COMMENT '更新人ID',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `is_delete` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-正常 1-已删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_code`(`code` ASC, `is_delete` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '角色表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_role
-- ----------------------------
INSERT INTO `sys_role` VALUES (1, '超级管理员', 'super_admin', 1, 1, '', 1, '拥有全部权限', NULL, NULL, NULL, NULL, 0);

-- ----------------------------
-- Table structure for sys_role_menu
-- ----------------------------
DROP TABLE IF EXISTS `sys_role_menu`;
CREATE TABLE `sys_role_menu`  (
  `id` bigint NOT NULL COMMENT '主键ID（雪花算法）',
  `role_id` bigint NOT NULL COMMENT '角色ID',
  `menu_id` bigint NOT NULL COMMENT '菜单ID',
  `create_by` bigint NULL DEFAULT NULL COMMENT '创建人ID',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint NULL DEFAULT NULL COMMENT '更新人ID',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `is_delete` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-正常 1-已删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_role_id`(`role_id` ASC) USING BTREE,
  INDEX `idx_menu_id`(`menu_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '角色菜单关联表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_role_menu
-- ----------------------------

-- ----------------------------
-- Table structure for sys_social_user
-- ----------------------------
DROP TABLE IF EXISTS `sys_social_user`;
CREATE TABLE `sys_social_user`  (
  `id` bigint NOT NULL COMMENT '主键ID（雪花算法）',
  `user_id` bigint NOT NULL COMMENT '管理员用户ID',
  `social_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '第三方类型：WECOM-企业微信 DINGTALK-钉钉 ...',
  `open_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '第三方用户唯一标识',
  `union_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '第三方 UnionId',
  `nickname` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '第三方昵称',
  `avatar` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '第三方头像',
  `raw_info` varchar(2048) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '第三方原始信息（JSON）',
  `create_by` bigint NULL DEFAULT NULL COMMENT '创建人ID',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint NULL DEFAULT NULL COMMENT '更新人ID',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `is_delete` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-正常 1-已删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_type_openid`(`social_type` ASC, `open_id` ASC, `is_delete` ASC) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '管理端第三方登录绑定表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_social_user
-- ----------------------------

-- ----------------------------
-- Table structure for union_account_transaction
-- ----------------------------
DROP TABLE IF EXISTS `union_account_transaction`;
CREATE TABLE `union_account_transaction`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `member_id` bigint NOT NULL COMMENT '推广者ID',
  `transaction_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '流水号（唯一）',
  `transaction_type` tinyint NOT NULL COMMENT '类型：1-佣金入账 2-提现扣减 3-系统调整 4-退款回滚 5-税费扣除',
  `amount` decimal(12, 2) NOT NULL COMMENT '变动金额（正数增加，负数减少）',
  `balance_before` decimal(12, 2) NOT NULL COMMENT '变动前余额',
  `balance_after` decimal(12, 2) NOT NULL COMMENT '变动后余额',
  `source_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '来源ID（如订单ID、提现申请ID）',
  `remark` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `status` tinyint NULL DEFAULT 1 COMMENT '1-成功 2-失败 3-处理中',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_transaction_no`(`transaction_no` ASC) USING BTREE,
  INDEX `idx_member_id`(`member_id` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '账户资金流水表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of union_account_transaction
-- ----------------------------

-- ----------------------------
-- Table structure for union_api_call_log
-- ----------------------------
DROP TABLE IF EXISTS `union_api_call_log`;
CREATE TABLE `union_api_call_log`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `platform` tinyint NOT NULL COMMENT '平台',
  `api_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'API名称（如：taobao.tbk.item.get）',
  `request_params` json NULL COMMENT '请求参数（脱敏）',
  `response_code` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '响应码',
  `response_msg` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '响应消息',
  `is_success` tinyint(1) NULL DEFAULT 1 COMMENT '是否成功',
  `duration` int NULL DEFAULT 0 COMMENT '耗时（毫秒）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_platform_api`(`platform` ASC, `api_name` ASC) USING BTREE,
  INDEX `idx_is_success`(`is_success` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '平台API调用日志表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of union_api_call_log
-- ----------------------------

-- ----------------------------
-- Table structure for union_auto_audit_config
-- ----------------------------
DROP TABLE IF EXISTS `union_auto_audit_config`;
CREATE TABLE `union_auto_audit_config`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `config_key` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '配置键',
  `config_value` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '配置值',
  `description` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '描述',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_config_key`(`config_key` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '自动审核配置表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of union_auto_audit_config
-- ----------------------------

-- ----------------------------
-- Table structure for union_category
-- ----------------------------
DROP TABLE IF EXISTS `union_category`;
CREATE TABLE `union_category`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `parent_id` bigint NULL DEFAULT 0 COMMENT '父级ID（0表示顶级）',
  `name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '分类名称',
  `icon` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '分类图标URL',
  `sort_order` int NULL DEFAULT 0,
  `status` tinyint NULL DEFAULT 1 COMMENT '1-启用 0-禁用',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_parent`(`parent_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '商品分类表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of union_category
-- ----------------------------

-- ----------------------------
-- Table structure for union_click_tracking
-- ----------------------------
DROP TABLE IF EXISTS `union_click_tracking`;
CREATE TABLE `union_click_tracking`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `short_code` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '短链码',
  `promotion_link_id` bigint NOT NULL COMMENT '推广链接ID',
  `member_id` bigint NOT NULL COMMENT '推广者ID',
  `platform` tinyint NOT NULL COMMENT '平台',
  `product_id` bigint NULL DEFAULT NULL COMMENT '商品ID',
  `user_id` bigint NULL DEFAULT NULL COMMENT '登录用户ID（如果已登录）',
  `visitor_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '访客ID（设备指纹/Cookie）',
  `click_ip` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '点击IP',
  `user_agent` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '用户代理',
  `referer` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '来源页面',
  `province` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '省份',
  `city` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '城市',
  `is_converted` tinyint(1) NULL DEFAULT 0 COMMENT '是否产生订单：0-否 1-是',
  `order_id` bigint NULL DEFAULT NULL COMMENT '关联订单ID（归因后填充）',
  `order_amount` decimal(12, 2) NULL DEFAULT NULL COMMENT '订单金额',
  `click_time` datetime NOT NULL COMMENT '点击时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_short_code`(`short_code` ASC) USING BTREE,
  INDEX `idx_promotion_link_id`(`promotion_link_id` ASC) USING BTREE,
  INDEX `idx_member_id`(`member_id` ASC) USING BTREE,
  INDEX `idx_visitor_id`(`visitor_id` ASC) USING BTREE,
  INDEX `idx_click_time`(`click_time` ASC) USING BTREE,
  INDEX `idx_is_converted`(`is_converted` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '推广链接点击明细表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of union_click_tracking
-- ----------------------------

-- ----------------------------
-- Table structure for union_commission_adjustment
-- ----------------------------
DROP TABLE IF EXISTS `union_commission_adjustment`;
CREATE TABLE `union_commission_adjustment`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `member_id` bigint NOT NULL COMMENT '推广者ID',
  `adjust_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '调整单号',
  `adjust_type` tinyint NOT NULL COMMENT '调整类型：1-补偿 2-扣减 3-修正',
  `adjust_amount` decimal(12, 2) NOT NULL COMMENT '调整金额（正数增加，负数减少）',
  `before_balance` decimal(12, 2) NOT NULL COMMENT '调整前余额',
  `after_balance` decimal(12, 2) NOT NULL COMMENT '调整后余额',
  `order_id` bigint NULL DEFAULT NULL COMMENT '关联订单ID（如订单佣金修正）',
  `reason` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '调整原因',
  `attachments` json NULL COMMENT '附件（凭证图片）',
  `operator_id` bigint NOT NULL COMMENT '操作人ID',
  `operator_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '操作人姓名',
  `status` tinyint NULL DEFAULT 1 COMMENT '1-生效 2-已撤销',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_adjust_no`(`adjust_no` ASC) USING BTREE,
  INDEX `idx_member_id`(`member_id` ASC) USING BTREE,
  INDEX `idx_order_id`(`order_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '佣金调整记录表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of union_commission_adjustment
-- ----------------------------

-- ----------------------------
-- Table structure for union_commission_distribution
-- ----------------------------
DROP TABLE IF EXISTS `union_commission_distribution`;
CREATE TABLE `union_commission_distribution`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `order_id` bigint NOT NULL COMMENT '订单ID',
  `commission_record_id` bigint NOT NULL COMMENT '佣金记录ID',
  `receiver_member_id` bigint NOT NULL COMMENT '接收佣金的推广者ID',
  `receiver_level` int NOT NULL COMMENT '接收人的层级（1=本人，2=上级，...）',
  `amount` decimal(12, 2) NOT NULL COMMENT '实际分配金额',
  `rate` decimal(5, 4) NOT NULL COMMENT '分配比例（占可分配池）',
  `distribution_type` tinyint NOT NULL COMMENT '类型：1-直接佣金 2-层级奖励 3-平台返利',
  `status` tinyint NULL DEFAULT 1 COMMENT '1-待入账 2-已入账 3-已失效',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_order_id`(`order_id` ASC) USING BTREE,
  INDEX `idx_member_id`(`receiver_member_id` ASC) USING BTREE,
  INDEX `idx_record_id`(`commission_record_id` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '佣金分配明细表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of union_commission_distribution
-- ----------------------------

-- ----------------------------
-- Table structure for union_commission_record
-- ----------------------------
DROP TABLE IF EXISTS `union_commission_record`;
CREATE TABLE `union_commission_record`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `order_id` bigint NOT NULL COMMENT '关联union_order.id',
  `member_id` bigint NOT NULL COMMENT '推广者ID',
  `actual_promoter_id` bigint NULL DEFAULT NULL COMMENT '实际推广者ID（可能是外部推广者）',
  `actual_promoter_type` tinyint NULL DEFAULT 1 COMMENT '实际推广者类型：1-系统会员 2-外部推广者',
  `external_promoter_id` bigint NULL DEFAULT NULL COMMENT '外部推广者ID',
  `settle_target_type` tinyint NULL DEFAULT 1 COMMENT '结算目标类型：1-系统会员 2-团长代收',
  `product_id` bigint NULL DEFAULT NULL COMMENT '商品ID',
  `order_amount` decimal(12, 2) NOT NULL COMMENT '订单金额',
  `commission_rate` decimal(8, 2) NOT NULL COMMENT '佣金比率',
  `commission_amount` decimal(12, 2) NOT NULL COMMENT '佣金金额（含税）',
  `tax_amount` decimal(12, 2) NULL DEFAULT 0.00 COMMENT '税金（如适用）',
  `actual_amount` decimal(12, 2) NOT NULL COMMENT '实际入账金额（扣除税费后）',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态：1-待计算 2-待入账 3-已入账 4-入账失败 5-已退款 6-已取消',
  `confirm_status` tinyint NULL DEFAULT 0 COMMENT '确认状态：0-待确认 1-已确认 2-待修正 3-已驳回',
  `confirm_time` datetime NULL DEFAULT NULL COMMENT '确认时间',
  `settle_batch_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '结算批次号',
  `settle_time` datetime NULL DEFAULT NULL COMMENT '入账时间',
  `transaction_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '关联资金流水ID',
  `remark` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `retry_count` int NULL DEFAULT 0 COMMENT '重试次数',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_order_id`(`order_id` ASC) USING BTREE,
  INDEX `idx_member_id`(`member_id` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE,
  INDEX `idx_settle_batch`(`settle_batch_no` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '佣金记录表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of union_commission_record
-- ----------------------------

-- ----------------------------
-- Table structure for union_commission_rule
-- ----------------------------
DROP TABLE IF EXISTS `union_commission_rule`;
CREATE TABLE `union_commission_rule`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `rule_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '规则名称',
  `rule_type` tinyint NOT NULL COMMENT '规则类型：1-全局默认 2-按商品类目 3-按推广者等级 4-按商品平台',
  `platform` tinyint NULL DEFAULT NULL COMMENT '平台：1-淘宝 2-京东 3-拼多多（NULL表示全部）',
  `category_id` bigint NULL DEFAULT NULL COMMENT '类目ID（NULL表示全部）',
  `member_level` int NULL DEFAULT NULL COMMENT '推广者等级（NULL表示全部）',
  `platform_service_rate` decimal(5, 4) NULL DEFAULT 0.0000 COMMENT '平台服务费率（如淘宝10%）',
  `system_cost_rate` decimal(5, 4) NOT NULL COMMENT '系统运营成本率（如15%）',
  `distributable_rate` decimal(5, 4) NOT NULL COMMENT '可分配佣金率 = 1 - platform_service_rate - system_cost_rate',
  `direct_commission_rate` decimal(5, 4) NOT NULL COMMENT '推广者直接佣金率（占可分配池）',
  `external_promoter_rate` decimal(5, 4) NULL DEFAULT 0.0000 COMMENT '外部推广者佣金比例',
  `level_1_rate` decimal(5, 4) NULL DEFAULT 0.0000 COMMENT '一级上级奖励率（占可分配池）',
  `level_2_rate` decimal(5, 4) NULL DEFAULT 0.0000 COMMENT '二级上级奖励率（占可分配池）',
  `level_3_rate` decimal(5, 4) NULL DEFAULT 0.0000 COMMENT '三级上级奖励率（占可分配池）',
  `level_4_rate` decimal(5, 4) NULL DEFAULT 0.0000 COMMENT '四级上级奖励率',
  `level_5_rate` decimal(5, 4) NULL DEFAULT 0.0000 COMMENT '五级上级奖励率',
  `priority` int NULL DEFAULT 0 COMMENT '优先级（数字越小越优先匹配）',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '1-启用 0-禁用',
  `start_time` datetime NULL DEFAULT NULL COMMENT '生效开始时间',
  `end_time` datetime NULL DEFAULT NULL COMMENT '生效结束时间',
  `remark` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_platform_category`(`platform` ASC, `category_id` ASC) USING BTREE,
  INDEX `idx_member_level`(`member_level` ASC) USING BTREE,
  INDEX `idx_priority`(`priority` ASC) USING BTREE,
  INDEX `idx_status_time`(`status` ASC, `start_time` ASC, `end_time` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '佣金分配规则配置表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of union_commission_rule
-- ----------------------------

-- ----------------------------
-- Table structure for union_external_account
-- ----------------------------
DROP TABLE IF EXISTS `union_external_account`;
CREATE TABLE `union_external_account`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `external_promoter_id` bigint NOT NULL COMMENT '外部推广者ID',
  `master_member_id` bigint NOT NULL COMMENT '所属团长ID',
  `total_commission` decimal(12, 2) NULL DEFAULT 0.00 COMMENT '累计佣金总额',
  `available_balance` decimal(12, 2) NULL DEFAULT 0.00 COMMENT '可提现余额',
  `frozen_balance` decimal(12, 2) NULL DEFAULT 0.00 COMMENT '冻结余额（提现中）',
  `total_withdrawn` decimal(12, 2) NULL DEFAULT 0.00 COMMENT '累计已提现',
  `pending_settle` decimal(12, 2) NULL DEFAULT 0.00 COMMENT '待结算佣金（未到结算周期）',
  `version` int NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_external_promoter`(`external_promoter_id` ASC) USING BTREE,
  INDEX `idx_master_member`(`master_member_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '外部推广者账户表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of union_external_account
-- ----------------------------

-- ----------------------------
-- Table structure for union_external_commission_detail
-- ----------------------------
DROP TABLE IF EXISTS `union_external_commission_detail`;
CREATE TABLE `union_external_commission_detail`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `external_promoter_id` bigint NOT NULL COMMENT '外部推广者ID',
  `master_member_id` bigint NOT NULL COMMENT '所属团长ID',
  `order_id` bigint NOT NULL COMMENT '关联订单ID',
  `order_amount` decimal(12, 2) NOT NULL COMMENT '订单金额',
  `commission_rate` decimal(8, 2) NOT NULL COMMENT '佣金比率',
  `commission_amount` decimal(12, 2) NOT NULL COMMENT '佣金金额',
  `settle_status` tinyint NOT NULL DEFAULT 0 COMMENT '结算状态：0-待结算 1-已结算 2-已退款 3-结算失败',
  `settle_time` datetime NULL DEFAULT NULL COMMENT '结算时间',
  `settle_batch_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '结算批次号',
  `transaction_id` bigint NULL DEFAULT NULL COMMENT '关联资金流水ID',
  `remark` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_order_external`(`order_id` ASC, `external_promoter_id` ASC) USING BTREE,
  INDEX `idx_external_promoter`(`external_promoter_id` ASC) USING BTREE,
  INDEX `idx_master_member`(`master_member_id` ASC) USING BTREE,
  INDEX `idx_settle_status`(`settle_status` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '外部推广者佣金明细表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of union_external_commission_detail
-- ----------------------------

-- ----------------------------
-- Table structure for union_external_commission_pending
-- ----------------------------
DROP TABLE IF EXISTS `union_external_commission_pending`;
CREATE TABLE `union_external_commission_pending`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `external_promoter_id` bigint NOT NULL COMMENT '外部推广者ID',
  `master_member_id` bigint NOT NULL COMMENT '团长ID',
  `order_id` bigint NOT NULL COMMENT '订单ID',
  `commission_amount` decimal(12, 2) NOT NULL COMMENT '待分配佣金',
  `settle_status` tinyint NULL DEFAULT 0 COMMENT '结算状态：0-待结算 1-已结算 2-已取消',
  `settle_time` datetime NULL DEFAULT NULL COMMENT '结算时间',
  `remark` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_external_promoter`(`external_promoter_id` ASC) USING BTREE,
  INDEX `idx_master_member`(`master_member_id` ASC) USING BTREE,
  INDEX `idx_settle_status`(`settle_status` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '外部推广者待结算佣金表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of union_external_commission_pending
-- ----------------------------

-- ----------------------------
-- Table structure for union_external_promoter
-- ----------------------------
DROP TABLE IF EXISTS `union_external_promoter`;
CREATE TABLE `union_external_promoter`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `promoter_code` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '推广者唯一编码（业务生成）',
  `name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '推广者姓名/昵称',
  `mobile` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '手机号（可选）',
  `master_member_id` bigint NOT NULL COMMENT '所属团长/上级推广者ID（有系统账号）',
  `bind_type` tinyint NULL DEFAULT 1 COMMENT '绑定类型：1-临时绑定 2-永久绑定 3-活动绑定',
  `commission_settle_type` tinyint NOT NULL DEFAULT 1 COMMENT '佣金结算方式：1-由团长手动结算 2-系统自动结算到团长账户',
  `status` tinyint NULL DEFAULT 1 COMMENT '状态：1-正常 2-已禁用 3-已过期',
  `bind_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '绑定时间',
  `expire_time` datetime NULL DEFAULT NULL COMMENT '过期时间（临时绑定用）',
  `unbind_time` datetime NULL DEFAULT NULL COMMENT '解绑时间',
  `remark` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_promoter_code`(`promoter_code` ASC) USING BTREE,
  INDEX `idx_master_member_id`(`master_member_id` ASC) USING BTREE,
  INDEX `idx_mobile`(`mobile` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '外部推广者/影子推广者表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of union_external_promoter
-- ----------------------------

-- ----------------------------
-- Table structure for union_external_transaction
-- ----------------------------
DROP TABLE IF EXISTS `union_external_transaction`;
CREATE TABLE `union_external_transaction`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `external_promoter_id` bigint NOT NULL COMMENT '外部推广者ID',
  `master_member_id` bigint NOT NULL COMMENT '所属团长ID',
  `transaction_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '流水号（唯一）',
  `transaction_type` tinyint NOT NULL COMMENT '类型：1-佣金入账 2-提现扣减 3-系统调整 4-退款回滚',
  `amount` decimal(12, 2) NOT NULL COMMENT '变动金额（正数增加，负数减少）',
  `balance_before` decimal(12, 2) NOT NULL COMMENT '变动前余额',
  `balance_after` decimal(12, 2) NOT NULL COMMENT '变动后余额',
  `source_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '来源ID（如订单ID、提现申请ID）',
  `remark` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `status` tinyint NULL DEFAULT 1 COMMENT '1-成功 2-失败 3-处理中',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_transaction_no`(`transaction_no` ASC) USING BTREE,
  INDEX `idx_external_promoter`(`external_promoter_id` ASC) USING BTREE,
  INDEX `idx_master_member`(`master_member_id` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '外部推广者资金流水表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of union_external_transaction
-- ----------------------------

-- ----------------------------
-- Table structure for union_external_withdraw
-- ----------------------------
DROP TABLE IF EXISTS `union_external_withdraw`;
CREATE TABLE `union_external_withdraw`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `external_promoter_id` bigint NOT NULL,
  `master_member_id` bigint NOT NULL,
  `apply_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '申请单号',
  `apply_amount` decimal(12, 2) NOT NULL,
  `actual_amount` decimal(12, 2) NULL DEFAULT NULL,
  `fee_amount` decimal(12, 2) NULL DEFAULT 0.00,
  `pay_account` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '收款账号',
  `pay_account_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '收款人姓名',
  `status` tinyint NULL DEFAULT 1 COMMENT '1-待处理 2-已打款 3-已驳回',
  `pay_time` datetime NULL DEFAULT NULL,
  `transaction_id` bigint NULL DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_apply_no`(`apply_no` ASC) USING BTREE,
  INDEX `idx_external_promoter`(`external_promoter_id` ASC) USING BTREE,
  INDEX `idx_master_member`(`master_member_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '外部推广者提现申请表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of union_external_withdraw
-- ----------------------------

-- ----------------------------
-- Table structure for union_level_config
-- ----------------------------
DROP TABLE IF EXISTS `union_level_config`;
CREATE TABLE `union_level_config`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `level` int NOT NULL COMMENT '层级（1/2/3/...）',
  `level_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '层级名称（如：合伙人/总监/经理）',
  `commission_ratio` decimal(5, 4) NOT NULL COMMENT '该层级的佣金系数（如0.1表示10%）',
  `min_team_size` int NULL DEFAULT 0 COMMENT '晋升该层级所需最小团队人数',
  `min_team_commission` decimal(12, 2) NULL DEFAULT 0.00 COMMENT '晋升该层级所需最小团队佣金',
  `is_active` tinyint(1) NULL DEFAULT 1 COMMENT '是否启用',
  `sort_order` int NULL DEFAULT 0 COMMENT '排序',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_level`(`level` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '层级佣金配置表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of union_level_config
-- ----------------------------

-- ----------------------------
-- Table structure for union_link_config
-- ----------------------------
DROP TABLE IF EXISTS `union_link_config`;
CREATE TABLE `union_link_config`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `config_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '配置名称',
  `member_level` int NULL DEFAULT NULL COMMENT '推广者等级（NULL表示全部）',
  `platform` tinyint NULL DEFAULT NULL COMMENT '平台（NULL表示全部）',
  `category_id` bigint NULL DEFAULT NULL COMMENT '类目ID（NULL表示全部）',
  `enable_short_url` tinyint(1) NULL DEFAULT 1 COMMENT '是否生成短链接',
  `short_url_expire_days` int NULL DEFAULT 30 COMMENT '短链接有效期（天数）',
  `enable_promotion_code` tinyint(1) NULL DEFAULT 1 COMMENT '是否生成口令/二维码',
  `enable_poster` tinyint(1) NULL DEFAULT 0 COMMENT '是否生成海报',
  `auto_save_product` tinyint(1) NULL DEFAULT 1 COMMENT '转链时自动保存商品信息到union_product',
  `click_tracking_enabled` tinyint(1) NULL DEFAULT 1 COMMENT '是否启用点击追踪',
  `smart_redirect` tinyint(1) NULL DEFAULT 0 COMMENT '是否启用智能跳转（根据设备跳转不同端）',
  `expire_strategy` json NULL COMMENT '过期策略：{\"activity\":7, \"product\":30, \"shop\":60, \"default\":30}',
  `rate_limit_per_minute` int NULL DEFAULT 0 COMMENT '每分钟限流次数（0表示不限）',
  `rate_limit_per_hour` int NULL DEFAULT 0 COMMENT '每小时限流次数（0表示不限）',
  `priority` int NULL DEFAULT 0 COMMENT '优先级',
  `status` tinyint NULL DEFAULT 1 COMMENT '1-启用 0-禁用',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_member_level`(`member_level` ASC) USING BTREE,
  INDEX `idx_platform_category`(`platform` ASC, `category_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '转链配置表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of union_link_config
-- ----------------------------

-- ----------------------------
-- Table structure for union_link_request_log
-- ----------------------------
DROP TABLE IF EXISTS `union_link_request_log`;
CREATE TABLE `union_link_request_log`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `member_id` bigint NOT NULL COMMENT '推广者ID',
  `platform` tinyint NOT NULL COMMENT '平台',
  `source_type` tinyint NOT NULL COMMENT '来源类型：1-商品链接 2-商品ID 3-淘口令 4-搜索关键词',
  `source_input` varchar(2048) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '用户输入的原始内容（脱敏后）',
  `outer_product_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '解析出的第三方商品ID',
  `product_id` bigint NULL DEFAULT NULL COMMENT '关联union_product.id（可能为NULL，首次转链时商品未入库）',
  `link_type` tinyint NOT NULL COMMENT '链接类型：1-单品 2-店铺 3-活动 4-通用',
  `request_ip` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '请求IP',
  `user_agent` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '用户代理',
  `result_status` tinyint NOT NULL DEFAULT 1 COMMENT '转链结果：1-成功 2-失败 3-部分成功',
  `promotion_link_id` bigint NULL DEFAULT NULL COMMENT '生成的推广链接ID（成功时）',
  `promotion_url` varchar(2048) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '生成的推广链接',
  `promotion_code` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '淘口令/口令码',
  `short_url` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '短链接',
  `error_code` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '错误码',
  `error_msg` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '错误信息',
  `api_call_duration` int NULL DEFAULT 0 COMMENT '调用第三方API耗时（毫秒）',
  `total_duration` int NULL DEFAULT 0 COMMENT '总耗时（毫秒）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_member_id`(`member_id` ASC) USING BTREE,
  INDEX `idx_platform`(`platform` ASC) USING BTREE,
  INDEX `idx_product_id`(`product_id` ASC) USING BTREE,
  INDEX `idx_result_status`(`result_status` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '转链请求日志表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of union_link_request_log
-- ----------------------------

-- ----------------------------
-- Table structure for union_member_account
-- ----------------------------
DROP TABLE IF EXISTS `union_member_account`;
CREATE TABLE `union_member_account`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `member_id` bigint NOT NULL COMMENT '推广者ID',
  `total_commission` decimal(12, 2) NULL DEFAULT 0.00 COMMENT '累计佣金总额',
  `available_balance` decimal(12, 2) NULL DEFAULT 0.00 COMMENT '可提现余额',
  `frozen_balance` decimal(12, 2) NULL DEFAULT 0.00 COMMENT '冻结余额（提现中）',
  `total_withdrawn` decimal(12, 2) NULL DEFAULT 0.00 COMMENT '累计已提现',
  `pending_settle` decimal(12, 2) NULL DEFAULT 0.00 COMMENT '待结算佣金（未到结算周期）',
  `version` int NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_member_id`(`member_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '推广者账户表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of union_member_account
-- ----------------------------

-- ----------------------------
-- Table structure for union_member_device
-- ----------------------------
DROP TABLE IF EXISTS `union_member_device`;
CREATE TABLE `union_member_device`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `member_id` bigint NOT NULL COMMENT '推广者ID',
  `device_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '设备唯一标识（UUID/IMEI/IDFA）',
  `device_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '设备类型：IOS/ANDROID/PC/H5',
  `device_model` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '设备型号',
  `os_version` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '操作系统版本',
  `app_version` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT 'APP版本',
  `last_login_ip` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '最后登录IP',
  `last_login_time` datetime NULL DEFAULT NULL COMMENT '最后登录时间',
  `login_count` int NULL DEFAULT 0 COMMENT '登录次数',
  `status` tinyint NULL DEFAULT 1 COMMENT '1-正常 2-异常 3-已禁用',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_member_device`(`member_id` ASC, `device_id` ASC) USING BTREE,
  INDEX `idx_device_id`(`device_id` ASC) USING BTREE,
  INDEX `idx_member_id`(`member_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '推广者设备信息表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of union_member_device
-- ----------------------------

-- ----------------------------
-- Table structure for union_member_level
-- ----------------------------
DROP TABLE IF EXISTS `union_member_level`;
CREATE TABLE `union_member_level`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `level_code` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '等级编码：L1/L2/L3',
  `level_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '等级名称：见习/正式/黄金/钻石/合伙人',
  `min_team_size` int NULL DEFAULT 0 COMMENT '晋升所需最小团队人数',
  `min_team_commission` decimal(12, 2) NULL DEFAULT 0.00 COMMENT '晋升所需最小团队佣金',
  `commission_multiplier` decimal(5, 4) NULL DEFAULT 1.0000 COMMENT '佣金系数（等级越高，分配比例越高）',
  `status` tinyint NULL DEFAULT 1,
  `sort_order` int NULL DEFAULT 0,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_level_code`(`level_code` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '推广者等级配置表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of union_member_level
-- ----------------------------

-- ----------------------------
-- Table structure for union_member_relation
-- ----------------------------
DROP TABLE IF EXISTS `union_member_relation`;
CREATE TABLE `union_member_relation`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `member_id` bigint NOT NULL COMMENT '推广者ID（本人）',
  `parent_id` bigint NULL DEFAULT NULL COMMENT '上级推广者ID（直接邀请人）',
  `root_id` bigint NULL DEFAULT NULL COMMENT '顶级推广者ID（根节点，用于快速归属）',
  `path` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '层级路径：例如 /1/23/45/ ，从根节点到本人的ID路径',
  `level` int NULL DEFAULT 1 COMMENT '当前层级：1表示顶级，2表示二级，以此类推',
  `max_depth` int NULL DEFAULT 1 COMMENT '该节点以下的最大深度（子树深度，便于缓存）',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态：1-正常 2-已解除 3-已冻结',
  `bind_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '绑定时间（成为下级的时间）',
  `unbind_time` datetime NULL DEFAULT NULL COMMENT '解绑时间',
  `total_children` int NULL DEFAULT 0 COMMENT '直属下级数量',
  `total_team` int NULL DEFAULT 0 COMMENT '整个团队人数（所有层级）',
  `cache_version` int NULL DEFAULT 0 COMMENT '缓存版本号（变更时自增）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_member_id`(`member_id` ASC) USING BTREE,
  INDEX `idx_parent_id`(`parent_id` ASC) USING BTREE,
  INDEX `idx_root_id`(`root_id` ASC) USING BTREE,
  INDEX `idx_path`(`path`(255) ASC) USING BTREE,
  INDEX `idx_level`(`level` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '推广者关系表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of union_member_relation
-- ----------------------------

-- ----------------------------
-- Table structure for union_member_team_snapshot
-- ----------------------------
DROP TABLE IF EXISTS `union_member_team_snapshot`;
CREATE TABLE `union_member_team_snapshot`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `member_id` bigint NOT NULL COMMENT '推广者ID',
  `level_1_count` int NULL DEFAULT 0 COMMENT '一级下级数',
  `level_2_count` int NULL DEFAULT 0 COMMENT '二级下级数',
  `level_3_count` int NULL DEFAULT 0 COMMENT '三级下级数',
  `total_team_count` int NULL DEFAULT 0 COMMENT '团队总人数',
  `team_total_commission` decimal(12, 2) NULL DEFAULT 0.00 COMMENT '团队累计贡献佣金',
  `team_month_commission` decimal(12, 2) NULL DEFAULT 0.00 COMMENT '本月团队贡献佣金',
  `snapshot_date` date NOT NULL COMMENT '快照日期',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_member_date`(`member_id` ASC, `snapshot_date` ASC) USING BTREE,
  INDEX `idx_member_id`(`member_id` ASC) USING BTREE,
  INDEX `idx_snapshot_date`(`snapshot_date` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '推广者团队快照表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of union_member_team_snapshot
-- ----------------------------

-- ----------------------------
-- Table structure for union_notification
-- ----------------------------
DROP TABLE IF EXISTS `union_notification`;
CREATE TABLE `union_notification`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `member_id` bigint NOT NULL COMMENT '接收人ID',
  `notify_type` tinyint NOT NULL COMMENT '类型：1-提现审核结果 2-佣金到账 3-订单归因 4-系统公告 5-活动推送',
  `title` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '通知标题',
  `content` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '通知内容',
  `link_url` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '跳转链接',
  `extra_data` json NULL COMMENT '扩展数据（如订单ID、提现ID）',
  `is_read` tinyint(1) NULL DEFAULT 0 COMMENT '是否已读',
  `read_time` datetime NULL DEFAULT NULL COMMENT '阅读时间',
  `status` tinyint NULL DEFAULT 1 COMMENT '1-正常 2-已撤回',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_member_id`(`member_id` ASC) USING BTREE,
  INDEX `idx_is_read`(`is_read` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '消息通知表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of union_notification
-- ----------------------------

-- ----------------------------
-- Table structure for union_order
-- ----------------------------
DROP TABLE IF EXISTS `union_order`;
CREATE TABLE `union_order`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `sharding_key` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '分表键（格式：YYYYMM）',
  `platform` tinyint NOT NULL COMMENT '平台：1-淘宝 2-京东 3-拼多多',
  `order_source` tinyint NULL DEFAULT 1 COMMENT '订单来源：1-平台推送 2-定时轮询 3-手动录入',
  `outer_order_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '第三方平台订单号',
  `parent_order_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '父订单号（合并支付时）',
  `member_id` bigint NULL DEFAULT NULL COMMENT '推广者ID（归因后填充）',
  `promoter_type` tinyint NULL DEFAULT 1 COMMENT '推广者类型：1-系统会员 2-外部推广者',
  `external_promoter_id` bigint NULL DEFAULT NULL COMMENT '外部推广者ID',
  `master_member_id` bigint NULL DEFAULT NULL COMMENT '所属团长ID',
  `promotion_link_id` bigint NULL DEFAULT NULL COMMENT '关联推广链接记录ID',
  `product_id` bigint NULL DEFAULT NULL COMMENT '关联union_product.id',
  `product_title` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '商品标题（冗余）',
  `product_image` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '商品主图（冗余）',
  `outer_product_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '第三方商品ID（冗余）',
  `shop_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '店铺名称（冗余）',
  `order_amount` decimal(12, 2) NOT NULL COMMENT '订单金额（实际支付）',
  `commission_rate` decimal(8, 2) NULL DEFAULT NULL COMMENT '该笔订单的佣金比率（快照）',
  `commission_amount` decimal(12, 2) NULL DEFAULT NULL COMMENT '预估佣金金额',
  `actual_commission` decimal(12, 2) NULL DEFAULT NULL COMMENT '最终结算佣金（平台结算后确定）',
  `coupon_amount` decimal(12, 2) NULL DEFAULT 0.00 COMMENT '优惠券抵扣金额',
  `order_status` tinyint NOT NULL DEFAULT 1 COMMENT '订单状态：1-待支付 2-已支付 3-已收货 4-已结算 5-无效/退款 6-已关闭',
  `platform_order_status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '平台原始订单状态码',
  `settle_status` tinyint NULL DEFAULT 0 COMMENT '结算状态：0-未结算 1-已结算 2-结算失败',
  `settle_time` datetime NULL DEFAULT NULL COMMENT '实际结算时间',
  `order_time` datetime NOT NULL COMMENT '下单时间（第三方）',
  `pay_time` datetime NULL DEFAULT NULL COMMENT '支付时间',
  `confirm_time` datetime NULL DEFAULT NULL COMMENT '确认收货时间',
  `sync_time` datetime NULL DEFAULT NULL COMMENT '同步到本系统的时间',
  `extra_json` json NULL COMMENT '平台扩展数据（JSON）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_platform_outer_order`(`platform` ASC, `outer_order_id` ASC) USING BTREE,
  INDEX `idx_member_id`(`member_id` ASC) USING BTREE,
  INDEX `idx_promotion_link`(`promotion_link_id` ASC) USING BTREE,
  INDEX `idx_order_time`(`order_time` ASC) USING BTREE,
  INDEX `idx_order_status`(`order_status` ASC) USING BTREE,
  INDEX `idx_settle_status`(`settle_status` ASC) USING BTREE,
  INDEX `idx_sharding_key`(`sharding_key` ASC) USING BTREE,
  INDEX `idx_external_promoter_order`(`external_promoter_id` ASC) USING BTREE,
  INDEX `idx_master_member_order`(`master_member_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '联盟订单主表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of union_order
-- ----------------------------

-- ----------------------------
-- Table structure for union_order_attribution_log
-- ----------------------------
DROP TABLE IF EXISTS `union_order_attribution_log`;
CREATE TABLE `union_order_attribution_log`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `order_id` bigint NOT NULL COMMENT '订单ID',
  `platform` tinyint NOT NULL,
  `outer_order_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `attributed_member_id` bigint NULL DEFAULT NULL COMMENT '归因到的推广者ID',
  `attribution_method` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '归因方式：cookie/链接/设备ID',
  `confidence_score` decimal(5, 2) NULL DEFAULT NULL COMMENT '置信度分数',
  `process_status` tinyint NULL DEFAULT 0 COMMENT '0-待处理 1-成功 2-失败',
  `error_msg` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '失败原因',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_order_id`(`order_id` ASC) USING BTREE,
  INDEX `idx_outer_order`(`platform` ASC, `outer_order_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '订单归因日志表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of union_order_attribution_log
-- ----------------------------

-- ----------------------------
-- Table structure for union_platform_category_map
-- ----------------------------
DROP TABLE IF EXISTS `union_platform_category_map`;
CREATE TABLE `union_platform_category_map`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `platform` tinyint NOT NULL COMMENT '平台',
  `platform_category_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '平台类目ID',
  `platform_category_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '平台类目名称',
  `platform_parent_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '平台父级类目ID',
  `union_category_id` bigint NULL DEFAULT NULL COMMENT '映射到union_category.id',
  `status` tinyint NULL DEFAULT 1 COMMENT '1-启用 0-禁用',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_platform_category`(`platform` ASC, `platform_category_id` ASC) USING BTREE,
  INDEX `idx_union_category`(`union_category_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '平台类目映射表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of union_platform_category_map
-- ----------------------------

-- ----------------------------
-- Table structure for union_product
-- ----------------------------
DROP TABLE IF EXISTS `union_product`;
CREATE TABLE `union_product`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `platform` tinyint NOT NULL COMMENT '平台：1-淘宝 2-京东 3-拼多多 4-抖音',
  `outer_product_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '第三方平台原始商品ID（京东为字符串）',
  `title` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '商品标题',
  `sub_title` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '副标题/卖点',
  `image_url` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '主图URL',
  `image_list` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '商品轮播图列表，JSON数组',
  `market_price` decimal(12, 2) NULL DEFAULT NULL COMMENT '市场价（划掉的价格）',
  `sale_price` decimal(12, 2) NOT NULL COMMENT '当前售价',
  `commission_rate` decimal(8, 2) NOT NULL COMMENT '佣金比率（平台原始值，淘宝为千分比需转换）',
  `commission_amount` decimal(12, 2) NULL DEFAULT NULL COMMENT '预估佣金金额（预计算字段）',
  `shop_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '店铺名称',
  `shop_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '店铺ID',
  `category_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '类目ID',
  `category_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '类目名称',
  `sales_volume` int NULL DEFAULT 0 COMMENT '月销量',
  `good_comments` int NULL DEFAULT 0 COMMENT '好评数',
  `coupon_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '优惠券ID',
  `coupon_amount` decimal(12, 2) NULL DEFAULT NULL COMMENT '优惠券面额',
  `coupon_start_time` datetime NULL DEFAULT NULL COMMENT '优惠券开始时间',
  `coupon_end_time` datetime NULL DEFAULT NULL COMMENT '优惠券结束时间',
  `is_recommend` tinyint(1) NULL DEFAULT 0 COMMENT '是否推荐：0-否 1-是',
  `status` tinyint NULL DEFAULT 1 COMMENT '状态：1-上架 2-下架 3-审核中',
  `sync_status` tinyint NULL DEFAULT 0 COMMENT '同步状态：0-未同步 1-已同步 2-同步失败',
  `last_sync_time` datetime NULL DEFAULT NULL COMMENT '最后同步时间',
  `next_sync_time` datetime NULL DEFAULT NULL COMMENT '下次同步时间（用于增量同步）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_platform_outer`(`platform` ASC, `outer_product_id` ASC) USING BTREE,
  INDEX `idx_status_sync`(`status` ASC, `sync_status` ASC) USING BTREE,
  INDEX `idx_commission_rate`(`commission_rate` ASC) USING BTREE,
  INDEX `idx_update_time`(`update_time` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '联盟商品主表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of union_product
-- ----------------------------

-- ----------------------------
-- Table structure for union_product_category
-- ----------------------------
DROP TABLE IF EXISTS `union_product_category`;
CREATE TABLE `union_product_category`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `product_id` bigint NOT NULL,
  `category_id` bigint NOT NULL COMMENT '分类ID',
  `sort_order` int NULL DEFAULT 0 COMMENT '排序',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_product_category`(`product_id` ASC, `category_id` ASC) USING BTREE,
  INDEX `idx_category`(`category_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '商品分类关联表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of union_product_category
-- ----------------------------

-- ----------------------------
-- Table structure for union_product_extra
-- ----------------------------
DROP TABLE IF EXISTS `union_product_extra`;
CREATE TABLE `union_product_extra`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `product_id` bigint NOT NULL COMMENT '关联union_product.id',
  `detail_url` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '商品详情页URL',
  `desc` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '商品描述（富文本）',
  `brand_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '品牌名称',
  `brand_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '品牌ID',
  `province` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '发货地省份',
  `shipping_fee` decimal(10, 2) NULL DEFAULT 0.00 COMMENT '运费',
  `after_sale_service` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '售后服务',
  `extra_json` json NULL COMMENT '其他扩展字段（JSON）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_product_id`(`product_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '商品扩展信息表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of union_product_extra
-- ----------------------------

-- ----------------------------
-- Table structure for union_product_status_log
-- ----------------------------
DROP TABLE IF EXISTS `union_product_status_log`;
CREATE TABLE `union_product_status_log`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `product_id` bigint NOT NULL COMMENT '商品ID',
  `field_name` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '变更字段：status/is_recommend',
  `old_value` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '旧值',
  `new_value` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '新值',
  `operator_id` bigint NULL DEFAULT NULL COMMENT '操作人ID',
  `operator_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '操作人姓名',
  `remark` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '变更原因',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_product_id`(`product_id` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '商品状态变更日志表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of union_product_status_log
-- ----------------------------

-- ----------------------------
-- Table structure for union_product_sync_log
-- ----------------------------
DROP TABLE IF EXISTS `union_product_sync_log`;
CREATE TABLE `union_product_sync_log`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `platform` tinyint NOT NULL COMMENT '平台',
  `sync_type` tinyint NOT NULL COMMENT '同步类型：1-全量 2-增量 3-手动',
  `total_count` int NULL DEFAULT 0 COMMENT '同步总数',
  `success_count` int NULL DEFAULT 0 COMMENT '成功数',
  `fail_count` int NULL DEFAULT 0 COMMENT '失败数',
  `error_msg` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '错误信息',
  `start_time` datetime NOT NULL,
  `end_time` datetime NULL DEFAULT NULL,
  `status` tinyint NULL DEFAULT 0 COMMENT '0-进行中 1-成功 2-失败',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_platform_status`(`platform` ASC, `status` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '商品同步日志表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of union_product_sync_log
-- ----------------------------

-- ----------------------------
-- Table structure for union_promotion_link
-- ----------------------------
DROP TABLE IF EXISTS `union_promotion_link`;
CREATE TABLE `union_promotion_link`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `member_id` bigint NOT NULL COMMENT '推广者ID',
  `promoter_type` tinyint NOT NULL DEFAULT 1 COMMENT '推广者类型：1-系统会员 2-外部推广者',
  `external_promoter_id` bigint NULL DEFAULT NULL COMMENT '外部推广者ID（关联union_external_promoter.id）',
  `master_member_id` bigint NULL DEFAULT NULL COMMENT '所属团长ID（冗余，便于快速查询）',
  `platform` tinyint NOT NULL COMMENT '平台',
  `platform_adzone_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '平台广告位ID（淘宝adzone_id）',
  `platform_site_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '平台站点ID（淘宝site_id）',
  `product_id` bigint NULL DEFAULT NULL COMMENT '商品ID（单品推广）',
  `shop_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '店铺ID（店铺推广）',
  `link_type` tinyint NOT NULL COMMENT '链接类型：1-单品 2-店铺 3-活动 4-通用',
  `promotion_url` varchar(2048) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '推广长链接',
  `short_url` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '短链接',
  `promotion_code` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '淘口令/口令码',
  `expire_time` datetime NULL DEFAULT NULL COMMENT '有效期（NULL表示永久）',
  `click_count` int NULL DEFAULT 0 COMMENT '点击量',
  `order_count` int NULL DEFAULT 0 COMMENT '转化订单数',
  `status` tinyint NULL DEFAULT 1 COMMENT '1-正常 2-已失效',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_member_id`(`member_id` ASC) USING BTREE,
  INDEX `idx_product_id`(`product_id` ASC) USING BTREE,
  INDEX `idx_platform`(`platform` ASC) USING BTREE,
  INDEX `idx_external_promoter`(`external_promoter_id` ASC) USING BTREE,
  INDEX `idx_master_member`(`master_member_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '推广链接表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of union_promotion_link
-- ----------------------------

-- ----------------------------
-- Table structure for union_promotion_template
-- ----------------------------
DROP TABLE IF EXISTS `union_promotion_template`;
CREATE TABLE `union_promotion_template`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `template_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '模板名称',
  `platform` tinyint NULL DEFAULT NULL COMMENT '平台（NULL表示通用）',
  `category_id` bigint NULL DEFAULT NULL COMMENT '类目ID（NULL表示通用）',
  `template_type` tinyint NOT NULL COMMENT '模板类型：1-文案 2-海报 3-视频',
  `title_template` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '标题模板（支持变量：{title}、{price}、{commission}）',
  `desc_template` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '描述模板',
  `image_url` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '海报底图URL',
  `image_config` json NULL COMMENT '海报配置（文字位置、颜色、字体大小等）',
  `priority` int NULL DEFAULT 0 COMMENT '优先级',
  `status` tinyint NULL DEFAULT 1 COMMENT '1-启用 0-禁用',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_platform_category`(`platform` ASC, `category_id` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '推广文案模板表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of union_promotion_template
-- ----------------------------

-- ----------------------------
-- Table structure for union_risk_blacklist
-- ----------------------------
DROP TABLE IF EXISTS `union_risk_blacklist`;
CREATE TABLE `union_risk_blacklist`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `black_type` tinyint NOT NULL COMMENT '黑名单类型：1-IP 2-设备指纹 3-用户ID 4-手机号 5-身份证号',
  `black_value` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '黑名单值',
  `reason` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '加入原因',
  `operator_id` bigint NULL DEFAULT NULL COMMENT '操作人ID',
  `expire_time` datetime NULL DEFAULT NULL COMMENT '过期时间（NULL表示永久）',
  `status` tinyint NULL DEFAULT 1 COMMENT '1-生效 0-已失效',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_type_value`(`black_type` ASC, `black_value` ASC) USING BTREE,
  INDEX `idx_status_expire`(`status` ASC, `expire_time` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '风控黑名单表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of union_risk_blacklist
-- ----------------------------

-- ----------------------------
-- Table structure for union_risk_execution_log
-- ----------------------------
DROP TABLE IF EXISTS `union_risk_execution_log`;
CREATE TABLE `union_risk_execution_log`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `apply_id` bigint NOT NULL COMMENT '提现申请ID',
  `member_id` bigint NOT NULL COMMENT '推广者ID',
  `context_data` json NOT NULL COMMENT '风控上下文数据（所有检查字段的当前值）',
  `matched_rules` json NULL COMMENT '命中的规则列表：[{\"rule_code\":\"R001\",\"rule_name\":\"单日提现次数超限\",\"risk_score\":20}]',
  `total_risk_score` int NULL DEFAULT 0 COMMENT '总风险分值',
  `decision` tinyint NOT NULL COMMENT '决策：1-自动通过 2-转人工审核 3-拒绝 4-降额处理',
  `decision_reason` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '决策原因',
  `original_amount` decimal(12, 2) NULL DEFAULT NULL COMMENT '原始申请金额',
  `final_amount` decimal(12, 2) NULL DEFAULT NULL COMMENT '最终核定金额（降额后）',
  `execution_duration` int NULL DEFAULT 0 COMMENT '执行耗时（毫秒）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_apply_id`(`apply_id` ASC) USING BTREE,
  INDEX `idx_member_id`(`member_id` ASC) USING BTREE,
  INDEX `idx_decision`(`decision` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '风控执行记录表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of union_risk_execution_log
-- ----------------------------

-- ----------------------------
-- Table structure for union_risk_rule_config
-- ----------------------------
DROP TABLE IF EXISTS `union_risk_rule_config`;
CREATE TABLE `union_risk_rule_config`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `rule_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '规则编码（唯一标识）',
  `rule_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '规则名称',
  `rule_category` tinyint NOT NULL COMMENT '规则分类：1-用户行为 2-账户安全 3-交易异常 4-设备环境 5-业务合规',
  `rule_type` tinyint NOT NULL COMMENT '规则类型：1-硬拒绝（直接拒绝） 2-软拒绝（转人工） 3-降级（限制额度） 4-仅记录（不打分）',
  `condition_expression` json NOT NULL COMMENT '条件表达式：{\"field\":\"apply_amount\",\"operator\":\">\",\"value\":50000}',
  `condition_sql` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '条件SQL片段（预编译，用于数据库过滤）',
  `action_type` tinyint NOT NULL COMMENT '动作类型：1-拒绝 2-转人工审核 3-限制提现额度 4-冻结账户 5-发送告警',
  `action_params` json NULL COMMENT '动作参数：{\"limit_amount\":1000, \"freeze_days\":7}',
  `risk_score` int NULL DEFAULT 10 COMMENT '命中的风险分值（累加）',
  `auto_audit_threshold` int NULL DEFAULT 0 COMMENT '自动审核通过阈值（低于此值自动通过）',
  `priority` int NULL DEFAULT 0 COMMENT '执行优先级（数字越小越先执行）',
  `status` tinyint NULL DEFAULT 1 COMMENT '1-启用 0-禁用',
  `start_time` datetime NULL DEFAULT NULL COMMENT '生效开始时间',
  `end_time` datetime NULL DEFAULT NULL COMMENT '生效结束时间',
  `description` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '规则描述',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_rule_code`(`rule_code` ASC) USING BTREE,
  INDEX `idx_category_type`(`rule_category` ASC, `rule_type` ASC) USING BTREE,
  INDEX `idx_priority`(`priority` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '风控规则配置表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of union_risk_rule_config
-- ----------------------------

-- ----------------------------
-- Table structure for union_settle_batch
-- ----------------------------
DROP TABLE IF EXISTS `union_settle_batch`;
CREATE TABLE `union_settle_batch`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `batch_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '批次号（业务唯一）',
  `total_amount` decimal(12, 2) NOT NULL COMMENT '批次总金额',
  `record_count` int NOT NULL COMMENT '包含记录数',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态：1-待审核 2-审核通过 3-已结算 4-已驳回',
  `operator_id` bigint NULL DEFAULT NULL COMMENT '操作员ID',
  `audit_time` datetime NULL DEFAULT NULL COMMENT '审核时间',
  `settle_time` datetime NULL DEFAULT NULL COMMENT '结算完成时间',
  `remark` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_batch_no`(`batch_no` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '结算批次表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of union_settle_batch
-- ----------------------------

-- ----------------------------
-- Table structure for union_short_url_map
-- ----------------------------
DROP TABLE IF EXISTS `union_short_url_map`;
CREATE TABLE `union_short_url_map`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `short_code` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '短链码（如：abc123）',
  `promotion_link_id` bigint NOT NULL COMMENT '关联推广链接ID',
  `original_url` varchar(2048) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '原始推广链接',
  `target_url` varchar(2048) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '最终跳转URL（可做A/B测试或渠道跟踪）',
  `expire_time` datetime NULL DEFAULT NULL COMMENT '过期时间（NULL表示永久）',
  `total_clicks` int NULL DEFAULT 0 COMMENT '总点击量',
  `unique_clicks` int NULL DEFAULT 0 COMMENT '独立访客点击量（去重）',
  `last_click_time` datetime NULL DEFAULT NULL COMMENT '最后点击时间',
  `status` tinyint NULL DEFAULT 1 COMMENT '状态：1-正常 2-已禁用 3-已过期',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_short_code`(`short_code` ASC) USING BTREE,
  INDEX `idx_promotion_link_id`(`promotion_link_id` ASC) USING BTREE,
  INDEX `idx_status_expire`(`status` ASC, `expire_time` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '短链接映射表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of union_short_url_map
-- ----------------------------

-- ----------------------------
-- Table structure for union_withdraw_apply
-- ----------------------------
DROP TABLE IF EXISTS `union_withdraw_apply`;
CREATE TABLE `union_withdraw_apply`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `apply_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '申请单号（业务唯一，格式：WD + 日期 + 序列号）',
  `member_id` bigint NOT NULL COMMENT '推广者ID',
  `member_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '推广者姓名（冗余，便于展示）',
  `member_mobile` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '推广者手机号（冗余，便于联系）',
  `apply_amount` decimal(12, 2) NOT NULL COMMENT '申请提现金额',
  `actual_amount` decimal(12, 2) NULL DEFAULT NULL COMMENT '实际到账金额（扣除税费/手续费后）',
  `fee_amount` decimal(12, 2) NULL DEFAULT 0.00 COMMENT '手续费',
  `tax_amount` decimal(12, 2) NULL DEFAULT 0.00 COMMENT '代扣税费',
  `tax_rate` decimal(5, 4) NULL DEFAULT 0.0000 COMMENT '税费率（如0.05表示5%）',
  `withdraw_type` tinyint NOT NULL COMMENT '提现方式：1-银行卡 2-支付宝 3-微信支付',
  `pay_account` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '收款账号（脱敏存储）',
  `pay_account_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '收款人姓名',
  `pay_bank_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '开户银行名称（银行卡时必填）',
  `pay_bank_branch` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '开户支行名称',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态：1-待审核 2-审核通过 3-已打款 4-已驳回 5-已取消 6-打款失败',
  `auditor_id` bigint NULL DEFAULT NULL COMMENT '审核人ID',
  `auditor_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '审核人姓名（冗余）',
  `audit_time` datetime NULL DEFAULT NULL COMMENT '审核时间',
  `audit_remark` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '审核备注（驳回原因填这里）',
  `pay_operator_id` bigint NULL DEFAULT NULL COMMENT '打款操作人ID',
  `pay_operator_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '打款操作人姓名',
  `pay_time` datetime NULL DEFAULT NULL COMMENT '打款时间',
  `pay_channel_order_no` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '支付渠道订单号（如支付宝/微信流水号）',
  `pay_fail_reason` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '打款失败原因',
  `retry_count` int NULL DEFAULT 0 COMMENT '打款重试次数',
  `transaction_id` bigint NULL DEFAULT NULL COMMENT '关联资金流水ID（union_account_transaction.id）',
  `transaction_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '关联资金流水号（冗余）',
  `cancel_time` datetime NULL DEFAULT NULL COMMENT '用户取消时间',
  `cancel_reason` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '取消原因',
  `remark` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `extra_json` json NULL COMMENT '扩展JSON（如：{\"voucher_url\": \"xxx\"}）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_apply_no`(`apply_no` ASC) USING BTREE,
  INDEX `idx_member_id`(`member_id` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE,
  INDEX `idx_pay_channel_order`(`pay_channel_order_no` ASC) USING BTREE,
  INDEX `idx_transaction_id`(`transaction_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '提现申请表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of union_withdraw_apply
-- ----------------------------

-- ----------------------------
-- Table structure for union_withdraw_audit_log
-- ----------------------------
DROP TABLE IF EXISTS `union_withdraw_audit_log`;
CREATE TABLE `union_withdraw_audit_log`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `apply_id` bigint NOT NULL COMMENT '提现申请ID',
  `apply_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '申请单号',
  `operator_id` bigint NOT NULL COMMENT '操作人ID',
  `operator_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '操作人姓名',
  `operation_type` tinyint NOT NULL COMMENT '操作类型：1-提交申请 2-审核通过 3-审核驳回 4-打款 5-打款失败 6-用户取消 7-系统自动处理',
  `before_status` tinyint NULL DEFAULT NULL COMMENT '操作前状态',
  `after_status` tinyint NULL DEFAULT NULL COMMENT '操作后状态',
  `remark` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '操作备注',
  `extra_json` json NULL COMMENT '扩展信息（如：{\"auto_audit\": true, \"risk_score\": 85}）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_apply_id`(`apply_id` ASC) USING BTREE,
  INDEX `idx_apply_no`(`apply_no` ASC) USING BTREE,
  INDEX `idx_operator_id`(`operator_id` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '提现审核日志表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of union_withdraw_audit_log
-- ----------------------------

-- ----------------------------
-- Table structure for union_withdraw_config
-- ----------------------------
DROP TABLE IF EXISTS `union_withdraw_config`;
CREATE TABLE `union_withdraw_config`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `config_key` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '配置键',
  `config_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '配置名称',
  `config_value` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '配置值',
  `config_type` tinyint NOT NULL COMMENT '配置类型：1-数字 2-字符串 3-百分比 4-JSON',
  `description` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '配置说明',
  `is_active` tinyint(1) NULL DEFAULT 1 COMMENT '是否启用',
  `sort_order` int NULL DEFAULT 0 COMMENT '排序',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_config_key`(`config_key` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '提现配置表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of union_withdraw_config
-- ----------------------------

-- ----------------------------
-- Table structure for union_withdraw_risk_record
-- ----------------------------
DROP TABLE IF EXISTS `union_withdraw_risk_record`;
CREATE TABLE `union_withdraw_risk_record`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `apply_id` bigint NOT NULL COMMENT '提现申请ID',
  `member_id` bigint NOT NULL COMMENT '推广者ID',
  `risk_score` int NULL DEFAULT 0 COMMENT '风险评分（0-100，越高越危险）',
  `risk_level` tinyint NULL DEFAULT 1 COMMENT '风险等级：1-低 2-中 3-高 4-极高',
  `check_items` json NULL COMMENT '检查项明细：{\"same_ip_orders\": 10, \"abnormal_amount\": false, \"new_device\": true}',
  `hit_rules` json NULL COMMENT '命中的风控规则：[\"高频提现\", \"异常IP\"]',
  `suggestion` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '建议：PASS-通过 MANUAL-人工审核 REJECT-拒绝',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_apply_id`(`apply_id` ASC) USING BTREE,
  INDEX `idx_member_id`(`member_id` ASC) USING BTREE,
  INDEX `idx_risk_score`(`risk_score` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '提现风控记录表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of union_withdraw_risk_record
-- ----------------------------

-- ----------------------------
-- Table structure for union_withdraw_whitelist
-- ----------------------------
DROP TABLE IF EXISTS `union_withdraw_whitelist`;
CREATE TABLE `union_withdraw_whitelist`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `member_id` bigint NOT NULL COMMENT '推广者ID',
  `whitelist_type` tinyint NOT NULL COMMENT '类型：1-全部自动通过 2-大额豁免 3-高频豁免 4-免风控',
  `limit_amount` decimal(12, 2) NULL DEFAULT NULL COMMENT '额度上限（NULL表示不限制）',
  `reason` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '加入原因',
  `operator_id` bigint NULL DEFAULT NULL COMMENT '操作人ID',
  `expire_time` datetime NULL DEFAULT NULL COMMENT '过期时间（NULL表示永久）',
  `status` tinyint NULL DEFAULT 1 COMMENT '1-生效 0-已失效',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_member_type`(`member_id` ASC, `whitelist_type` ASC) USING BTREE,
  INDEX `idx_status_expire`(`status` ASC, `expire_time` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '提现白名单表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of union_withdraw_whitelist
-- ----------------------------

SET FOREIGN_KEY_CHECKS = 1;
