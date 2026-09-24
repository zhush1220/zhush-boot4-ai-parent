-- ============================================================
-- 对话框多能力 P3/P4：图像/视频生成模型种子（MySQL 8.0+）
-- 关联方案：《对话框多能力补全与图像视频生成实现方案》P3、P4；依赖 sql/15（ai_model 表）
-- 背景：DashScope 通义万相文生图/文生视频模型；capability=image/video 供
--       AgentModelResolver.resolveByCapability 取凭证。api_key 走 ${ENV} 占位，与存量口径一致。
-- 口径：默认 status=0（停用）——无 DASHSCOPE_API_KEY 环境不误启用；拿到 Key 后配 zhush.agentscope.generation.enabled=true
--       并在模型管理页启用即可，无需改代码（生产禁 Mock，宁缺毋假）。id 续 15 的 93001~93003。
-- ============================================================

SET NAMES utf8mb4;

DELETE FROM `ai_model` WHERE `id` IN (93004, 93005);
INSERT INTO `ai_model`
    (`id`, `code`, `name`, `type`, `api_key`, `base_url`, `model`, `capability`, `is_default`, `priority`, `options`, `status`)
VALUES
    (93004, 'wanx2.1-t2i-turbo', '通义万相-文生图', 'dashscope', '${DASHSCOPE_API_KEY:}', '', 'wanx2.1-t2i-turbo', 'image', 0, 5, '{"size":"1024*1024","n":1}', 0),
    (93005, 'wanx2.1-t2v-turbo', '通义万相-文生视频', 'dashscope', '${DASHSCOPE_API_KEY:}', '', 'wanx2.1-t2v-turbo', 'video', 0, 5, '{"size":"1280*720","duration":5}', 0);

-- ------------------------------------------------------------
-- 校验：两条记录 capability 正确、默认停用
-- ------------------------------------------------------------
-- SELECT id, code, capability, status FROM ai_model WHERE id IN (93004, 93005);
