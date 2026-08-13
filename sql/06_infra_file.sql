-- ============================================================
-- 文件表 DDL（MySQL 8.0+）
-- 字符集：utf8mb4    存储引擎：InnoDB
-- 主键：雪花算法（应用层生成，BIGINT）
-- 公共字段：create_by / create_time / update_by / update_time / is_delete
--          与 com.zhush.common.entity.BaseEntity 对应
-- 写入方：com.zhush.system.service.FileService（上传成功后落库）
-- 存储实现：zhush-spring-boot-starter-file（本地/阿里云OSS/七牛云/华为云OBS）
-- ============================================================

SET NAMES utf8mb4;

-- ------------------------------------------------------------
-- 文件表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `infra_file`;
CREATE TABLE `infra_file` (
    `id`           BIGINT        NOT NULL COMMENT '主键ID（雪花算法）',
    `name`         VARCHAR(256)  NOT NULL DEFAULT '' COMMENT '原始文件名',
    `path`         VARCHAR(512)  NOT NULL DEFAULT '' COMMENT '相对存储路径（如 2026/08/12/xxx.jpg）',
    `url`          VARCHAR(1024) NOT NULL DEFAULT '' COMMENT '文件访问URL',
    `ext`          VARCHAR(32)   NOT NULL DEFAULT '' COMMENT '文件扩展名（不含点号）',
    `content_type` VARCHAR(128)  NOT NULL DEFAULT '' COMMENT 'MIME类型（如 image/png）',
    `size`         BIGINT        NOT NULL DEFAULT 0 COMMENT '文件大小（字节）',
    `storage_type` VARCHAR(32)   NOT NULL DEFAULT '' COMMENT '存储类型：local-本地 aliyun-oss-阿里云OSS qiniu-七牛云 huawei-obs-华为云OBS',
    `user_type`    VARCHAR(16)   NOT NULL DEFAULT '' COMMENT '上传人类型：admin-管理端 member-会员端',
    `user_id`      BIGINT            NULL COMMENT '上传人ID（未登录为 NULL）',
    `create_by`    BIGINT            NULL COMMENT '创建人ID',
    `create_time`  DATETIME          NULL COMMENT '创建时间',
    `update_by`    BIGINT            NULL COMMENT '更新人ID',
    `update_time`  DATETIME          NULL COMMENT '更新时间',
    `is_delete`    TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-正常 1-已删除',
    PRIMARY KEY (`id`),
    KEY `idx_user` (`user_type`, `user_id`),
    KEY `idx_storage_type` (`storage_type`),
    KEY `idx_create_time` (`create_time`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '文件表';

-- ------------------------------------------------------------
-- 菜单（文件管理权限，挂在系统管理目录下按需调整 parent_id）
-- ------------------------------------------------------------
-- INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `path`, `sort`)
-- VALUES (企业自行分配ID, 系统管理目录ID, '文件管理', 2, 'system:file:query', '/system/file', 99);
