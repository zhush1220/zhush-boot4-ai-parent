-- ============================================================
-- 通知公告模块（MySQL 8.0+）
-- 依赖：01_admin_rbac.sql（sys_menu / sys_role / sys_role_menu / sys_admin_user）
--      06_infra_file.sql（可选，通知公告本身不依赖，但上传解析复用文件能力）
--
-- 表：
--   sys_notice        通知公告表（标题、类型、状态、富文本内容）
--   sys_notice_record 公告查看记录（每用户每公告一条，重复查看刷新 view_time）
--
-- 菜单 ID 规划（沿用 12_system_menu.sql 风格）：
--   106  通知公告（系统管理目录下）
--   160  通知公告查询   161  通知公告新增   162  通知公告修改   163  通知公告删除
-- ============================================================

SET NAMES utf8mb4;

-- ------------------------------------------------------------
-- 通知公告表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `sys_notice`;
CREATE TABLE `sys_notice` (
  `id`            BIGINT       NOT NULL COMMENT '雪花ID主键',
  `title`         VARCHAR(200) NOT NULL COMMENT '公告标题',
  `type`          TINYINT      NOT NULL COMMENT '公告类型：1-通知 2-公告 3-警示',
  `status`        TINYINT      NOT NULL DEFAULT 0 COMMENT '状态：0-草稿 1-已发布 2-已撤回',
  `content`       LONGTEXT     NOT NULL COMMENT '富文本HTML内容',
  `publish_time`  DATETIME     NULL COMMENT '发布时间（status=1 时写入）',
  -- 审计列必须可空：MyBatis-Flex insert 会显式写入全部列，而 GlobalInsertListener 仅在插入时回填
  -- create_by/create_time，update_by/update_time 由更新时回填；写成 NOT NULL 会导致新增直接报
  -- "Column 'update_by' cannot be null"（与 01_admin_rbac.sql 等其余表保持一致）
  `create_by`     BIGINT       NULL COMMENT '创建人ID',
  `create_time`   DATETIME     NULL COMMENT '创建时间',
  `update_by`     BIGINT       NULL COMMENT '更新人ID',
  `update_time`   DATETIME     NULL COMMENT '更新时间',
  `is_delete`     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-否 1-是',
  PRIMARY KEY (`id`),
  KEY `idx_notice_title` (`title`),
  KEY `idx_notice_status` (`status`),
  KEY `idx_notice_type` (`type`),
  KEY `idx_notice_create_time` (`create_time`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '通知公告表';

-- ------------------------------------------------------------
-- 公告查看记录表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `sys_notice_record`;
CREATE TABLE `sys_notice_record` (
  `id`          BIGINT      NOT NULL COMMENT '雪花ID主键',
  `notice_id`   BIGINT      NOT NULL COMMENT '公告ID',
  `user_id`     BIGINT      NOT NULL COMMENT '查看人ID',
  `user_name`   VARCHAR(50) NULL     COMMENT '查看人姓名',
  `view_time`   DATETIME    NOT NULL COMMENT '最近查看时间',
  `create_time` DATETIME    NOT NULL COMMENT '首次查看时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_notice_user` (`notice_id`, `user_id`),
  KEY `idx_record_notice` (`notice_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '公告查看记录表';

-- ------------------------------------------------------------
-- 菜单与按钮权限点（父目录为系统管理 id=100，见 12_system_menu.sql）
-- ------------------------------------------------------------
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `path`, `component`, `icon`, `sort`, `visible`, `status`)
VALUES
    (109, 100, '通知公告', 2, '', 'notice', '/system/notice/index', 'lucide:bell', 6, 1, 1);

INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `path`, `component`, `icon`, `sort`, `visible`, `status`)
VALUES
    (190, 109, '通知公告查询', 3, 'system:notice:query',  '', '', '', 1, 1, 1),
    (191, 109, '通知公告新增', 3, 'system:notice:create', '', '', '', 2, 1, 1),
    (192, 109, '通知公告修改', 3, 'system:notice:update', '', '', '', 3, 1, 1),
    (193, 109, '通知公告删除', 3, 'system:notice:delete', '', '', '', 4, 1, 1);

-- 超级管理员（role_id=1）关联通知公告菜单与权限点
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`)
VALUES
    (109, 1, 109),
    (190, 1, 190), (191, 1, 191), (192, 1, 192), (193, 1, 193);
