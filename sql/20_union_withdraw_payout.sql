-- ============================================================
-- 提现出款（统一支付底座）增量 DDL（MySQL 8.0+）
-- 适用：已部署环境升级；全新安装直接执行 09_union.sql 即可（已含本文件变更）
-- 变更：union_withdraw_apply 新增 pay_channel / pay_openid 两列
--       status 注释补充 7-打款处理中
--       注册提现出款查单定时任务（sys_job）
--       新增提现支付渠道数据字典（sys_dict_type / sys_dict_data）
-- 注意：MySQL 8.0 不支持 ADD COLUMN IF NOT EXISTS，重复执行会报 1060，属预期
-- ============================================================

SET NAMES utf8mb4;

-- ------------------------------------------------------------
-- 1. 新增支付渠道（协议）字段：与 withdraw_type 显式区分
--    withdraw_type 表达业务方式（银行卡/支付宝/微信），pay_channel 表达实际协议
--    （微信既可走 V2 企业付款到银行卡，也可走 V3 商家转账）
-- ------------------------------------------------------------
ALTER TABLE `union_withdraw_apply`
    ADD COLUMN `pay_channel` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL
        COMMENT '支付渠道（协议）：WECHAT_V2 / WECHAT_V3 / ALIPAY，与 withdraw_type 显式区分' AFTER `pay_bank_branch`,
    ADD COLUMN `pay_openid` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL
        COMMENT '微信零钱收款 openid（pay_channel=WECHAT_V3 时必填，服务端反查写入）' AFTER `pay_channel`;

-- ------------------------------------------------------------
-- 2. 渠道过滤索引
-- ------------------------------------------------------------
ALTER TABLE `union_withdraw_apply`
    ADD INDEX `idx_pay_channel`(`pay_channel` ASC) USING BTREE;

-- ------------------------------------------------------------
-- 3. 状态注释补充：7-打款处理中（渠道已受理、结果未定的中间态）
-- ------------------------------------------------------------
ALTER TABLE `union_withdraw_apply`
    MODIFY COLUMN `status` tinyint NOT NULL DEFAULT 1
        COMMENT '状态：1-待审核 2-审核通过 3-已打款 4-已驳回 5-已取消 6-打款失败 7-打款处理中';

-- ------------------------------------------------------------
-- 4. 注册「提现出款查单收敛」定时任务（表驱动：invoke_target = beanName.method()）
--    对应 Bean：com.zhush.union.job.UnionWithdrawReconcileTask#reconcile()（无参）
--    每 2 分钟扫描 status=7 的处理中单据，主动查单收敛终态（V2 无回调，必须依赖查单兜底）
--    幂等：uk_name_group 唯一，重复执行前先按 job_name 清理旧记录
-- ------------------------------------------------------------
DELETE FROM `sys_job` WHERE `job_name` = '提现出款查单' AND `job_group` = 'DEFAULT';
INSERT INTO `sys_job` (`id`, `job_name`, `job_group`, `invoke_target`, `cron_expression`, `params`, `concurrent`, `status`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `is_delete`)
VALUES
    (300000000000000010, '提现出款查单', 'DEFAULT', 'unionWithdrawReconcileTask.reconcile()', '0 0/2 * * * ?', '', 0, 1, '无参示例：每 2 分钟扫描并收敛打款处理中的提现单', 0, NOW(), 0, NOW(), 0);

-- ------------------------------------------------------------
-- 5. 数据字典：提现支付渠道（协议）
--    与 union_withdraw_apply.pay_channel 取值一一对应
--    dict_type = union_pay_channel，供字典管理页维护
--    幂等：先按 dict_type 清理旧记录再插入（uk_type / uk_type_value 唯一）
-- ------------------------------------------------------------
DELETE FROM `sys_dict_data` WHERE `dict_type` = 'union_pay_channel';
DELETE FROM `sys_dict_type` WHERE `type` = 'union_pay_channel';

INSERT INTO `sys_dict_type` (`id`, `name`, `type`, `status`, `remark`)
VALUES (3, '提现支付渠道', 'union_pay_channel', 1,
        '提现出款实际使用的支付协议；与提现方式(withdraw_type)区分，微信可走 V2 或 V3');

INSERT INTO `sys_dict_data` (`id`, `dict_type`, `label`, `value`, `sort`, `status`)
VALUES (6, 'union_pay_channel', '微信V2（企业付款到银行卡）', 'WECHAT_V2', 1, 1),
       (7, 'union_pay_channel', '微信V3（商家转账）',        'WECHAT_V3', 2, 1),
       (8, 'union_pay_channel', '支付宝',                    'ALIPAY',    3, 1);
