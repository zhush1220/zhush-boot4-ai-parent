# kkFileView 在线文档预览整合方案

> 模块归属：`zhush-spring-boot-starter-file`（新增 `com.zhush.file.preview` 预览门面）、`zhush-module-system`（管理端接入）、`vue-vben-admin`（前端接入）
> 外部依赖：kkFileView（开源文档在线预览服务，官方仓库 https://github.com/kekingcn/kkFileView ）
> 现有能力：复用 `FileStorage` / `FileClient` / `infra_file` / `AdminFileController`
> 文档状态：**待确认**（确认后再进入功能编码）

## 一、需求概述

当前系统只具备「上传 + 解析」能力：

- **上传**：`FileClient` 落盘 + `infra_file` 落库，返回可访问 URL（`/api/files/yyyy/MM/dd/uuid.ext`）。
- **解析**：`POST /admin/system/file/parse-doc`，用 Tika 把 Word/PDF **提取成纯文本 + 富文本 HTML**，用于回填富文本编辑器。

两者都**不会原样呈现文档排版**。本方案要补齐「在线预览」：用户在文件管理、通知公告附件、合同/报表等场景下，能**不下载**直接在浏览器里查看 Word / Excel / PPT / PDF 等文档。

| 功能点 | 说明 |
|---|---|
| 统一预览入口 | 由 kkFileView 承担格式转换与渲染，业务系统只负责生成预览地址 |
| 按文件 ID 预览 | 后端新增接口，入参 `id`，出参「可预览 + 预览地址」，前端无需关心编码细节 |
| 前端通用预览 | 新窗口 / 弹窗 iframe 两种方式，封装为通用工具与组件，各页面按需复用 |
| 不可预览降级 | 不在白名单内的格式返回 `previewable=false`，前端引导下载 |
| 安全可控 | 关闭 kkFileView 上传与删除能力、开启来源白名单、水印、预览接口鉴权 + 审计 |

**非目标（本期不做）**：在线编辑、协同批注、OCR、文档版本管理。

## 二、复用已有能力

本模块尽量**不重复造轮子**，直接复用：

1. **文件元信息**：`infra_file` 表（`id` / `name` / `path` / `url` / `ext` / `content_type` / `size` / `storage_type`）——生成预览地址所需的一切字段已具备，**无需新增表、无需改库**。
2. **文件访问链路**：`LocalFileStorage.buildUrl = domain + path`，配合 `FileAutoConfiguration` 注册的静态资源映射（`zhush.file.local.url-path=/files/**` → `file:{base-path}`），文件已可通过 URL 直接下载。
3. **业务接口规范**：`AdminFileController`（`/admin/system/file`，权限 `system:file:query|create|delete`，`@Idempotent` / `@OperateLog`），新接口沿用同一套注解与返回结构。
4. **部署基座**：`deploy/backend` 使用 `network_mode: host` + nginx `listen 8086`，新容器可复用同一网络模型与统一入口。
5. **Tika 解析能力**：与预览**互补**，不是替代关系——「解析 = 取内容进富文本编辑器」，「预览 = 原样呈现排版」。

> 结论：kkFileView 是**旁路服务**，业务系统**不改动现有上传/解析链路**，只在 `AdminFileController` 上加一个"生成预览地址"的接口。

## 三、方案总览

### 3.1 架构

```
┌──────────────┐   ①GET /admin/system/file/{id}/preview      ┌────────────────────┐
│  前端（Vben） │ ──────────────────────────────────────────▶ │  zhush-server:9199 │
│  浏览器/WebView│ ◀──── ③{ previewable, previewUrl } ─────── │  PreviewService    │
└──────┬───────┘                                             └─────────┬──────────┘
       │ ④打开 previewUrl（iframe/新窗口）                              │ ②查 infra_file
       │                                        ┌────────────────────▼──────────┐
       │                                        │ infra_file（name/url/path/ext）│
       ▼                                        └───────────────────────────────┘
┌──────────────────┐   ⑤GET /onlinePreview?url=<base64>&fullfilename=xxx.docx
│ nginx:8086       │ ──────────────────────────────────────────▶ ┌─────────────────┐
│ location /preview│                                             │ kkFileView:8012 │
└──────────────────┘                                             └────────┬────────┘
                                                                          │ ⑥回源拉取源文件
                                                                          ▼
                                                       GET {sourceBaseUrl}/{path}
                                                       （如 http://172.17.0.1:9199/api/files/...）
```

### 3.2 预览时序

1. 前端调用 `GET /admin/system/file/{id}/preview`（携带 Sa-Token，权限校验、归属校验、操作日志）。
2. 后端 `PreviewService` 按 `id` 查 `infra_file`，判断 `ext` 是否在预览白名单内。
3. **重建 kkFileView 视角可达的「绝对源文件地址」**（见 3.3，这是本方案的关键点）。
4. 对源地址做 `Base64 → URLEncode` 双编码，拼成 `/onlinePreview?url=...&fullfilename=...`，返回给前端。
5. 前端新窗口 / 弹窗 iframe 打开该地址（指向 nginx `/preview` → kkFileView `8012`）。
6. kkFileView 反向拉取源文件地址（走 `/api/files/**`）→ 转换 → 在浏览器内渲染。

### 3.3 关键设计决策

**决策一：源文件地址必须"绝对可达"，不能直接用 `infra_file.url`。**

`infra_file.url` 在 docker 环境由 `FILE_DOMAIN=/api/files/` 拼接而来，是一个**相对前缀**（`/api/files/2026/...`），kkFileView 容器无法解析。因此新增配置 `zhush.preview.source-base-url`，规则：

- 若 `infra_file.url` 已是完整 `http(s)://` 地址（dev/local 环境）→ 直接使用；
- 否则 → `source-base-url` + `path` 重建。

建议取值（按部署形态）：

| 部署形态 | `source-base-url` | 说明 |
|---|---|---|
| docker（推荐） | `http://172.17.0.1:9199/api/files/` | 容器 host 网络，直连后端，绕过 nginx，链路最短 |
| docker + 统一入口 | `http://47.115.56.72:8086/files/` | 与前端同源，便于审计与限流；需 nginx `/files` 已配置 |
| 本地开发 | 留空，直接用 `infra_file.url` | `domain=http://localhost:9166/api/files/` 已是绝对地址 |

**决策二：预览地址由后端生成，而非前端拼接。**

`onlinePreview` 的双编码规则、`fullfilename`、水印参数属实现细节，集中在后端便于统一调整与灰度；前端只消费一个 `previewUrl`。

**决策三：不动鉴权白名单，接受"直链免登录"现状。**

四个环境的 Sa-Token `ignore-paths` 均含 `/api/files/**`，kkFileView 回源**无需 token**，集成成本最低；代价是直链可被转发（见「七、安全设计」的二期增强方案）。

## 四、配置与部署

### 4.1 后端配置（新增 `zhush.preview.*`）

```yaml
zhush:
  preview:
    enabled: true                                    # 预览总开关，关闭时接口返回 previewable=false
    base-url: ${PREVIEW_BASE_URL:/preview}           # 对外预览前缀（经 nginx 反代到 kkFileView）
    source-base-url: ${PREVIEW_SOURCE_BASE_URL:}     # 源文件绝对前缀；为空时回落到 infra_file.url
    watermark: ${PREVIEW_WATERMARK:}                 # 水印文案，为空则不加
    office-preview-type: pdf                         # 与 kkFileView 保持一致：pdf / image / html
    open-new-window: true                            # 前端默认打开方式（新窗口 / 弹窗）
    allowed-extensions:                              # 可预览格式白名单（与 kkFileView 支持格式对齐）
      - pdf
      - doc
      - docx
      - xls
      - xlsx
      - ppt
      - pptx
      - csv
      - txt
      - md
      - ofd
      - wps
      - et
      - dps
```

> `source-base-url` 在 docker 环境由 `deploy/backend/docker-compose.yaml` 注入：
> `PREVIEW_SOURCE_BASE_URL: http://172.17.0.1:9199/api/files/`

### 4.2 新增 kkFileView 容器 `deploy/kkfileview/docker-compose.yaml`

```yaml
services:
  kkfileview:
    image: keking/kkfileview:4.4.0        # 4.4.0：JDK8 系、稳定；5.x 强制 JDK21（见风险项）
    container_name: kkfileview
    restart: always
    network_mode: "host"                  # 与 zhush-server / nginx 保持一致的网络模型
    environment:
      TZ: Asia/Shanghai
      # ---- 服务地址：必须是对外可达地址，否则预览页内部资源链接会指向容器内网 ----
      KK_BASE_URL: http://47.115.56.72:8086/preview/
      # ---- 安全：来源白名单（4.4.0+ 默认拒绝未配置来源，防 SSRF）----
      KK_TRUST_HOST: 172.17.0.1,47.115.56.72,localhost,127.0.0.1
      # ---- 安全：关闭上传与删除，禁止脚本执行 ----
      KK_FILE_UPLOAD_DISABLE: "true"
      KK_SCRIPTJS: "false"
      # ---- 预览行为 ----
      KK_OFFICE_PREVIEW_TYPE: pdf
      KK_FORCE_UPDATED_CACHE: "true"
      # ---- 水印（可选，与 zhush.preview.watermark 保持一致）----
      KK_WATERMARK_TXT: ""
      # ---- 缓存目录与清理 ----
      KK_CACHE_CLEAN_CRON: "0 0 3 * * ?"
    volumes:
      - ./file:/opt/kkFileView/file          # 转换缓存与临时文件（持久化，避免撑爆容器层）
      - /usr/share/fonts:/usr/share/fonts    # 中文字体（缺字体会导致 Word/PPT 转 PDF 乱码）
```

> 端口：kkFileView 默认 `8012`，host 网络下直接监听宿主机 `8012`，**不建议对公网开放**，仅经 nginx 暴露 `/preview`。

### 4.3 nginx 新增预览反代（`deploy/nginx/default.conf`）

```nginx
    # 在线文档预览：kkFileView（仅放行 GET/HEAD，防被当作上传/删除入口）
    location /preview {
        limit_except GET HEAD { deny all; }

        proxy_pass http://172.17.0.1:8012/;
        proxy_http_version 1.1;
        proxy_set_header Host $http_host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_set_header Connection '';

        # 大文档转换耗时较长，放宽超时
        proxy_read_timeout 300s;
        proxy_send_timeout 300s;
    }
```

> **注意**：`/preview` 与现有 `/api`、`/files` 前缀无冲突。若采用 `KK_BASE_URL` 带 `/preview/` 后缀的写法，需保持 `proxy_pass` 尾部斜杠一致，避免拼出双斜杠。

### 4.4 中文字体

kkFileView 容器内置 LibreOffice，需确保中文字体齐全，否则 Word/PPT 转 PDF 会乱码。与 `zhush-server` 一致挂载宿主机字体目录 `.ttf/.ttc`；容器内首次使用需可执行 `fc-cache -fv`（或换用已内置中文字体的镜像）。

## 五、后端改造点

### 5.1 新增预览门面（`zhush-spring-boot-starter-file`）

新增包 `com.zhush.file.preview`：

| 类 | 职责 |
|---|---|
| `PreviewProperties` | 绑定 `zhush.preview.*`（`@ConfigurationProperties(prefix = "zhush.preview")`） |
| `PreviewService` | 生成预览地址：白名单判断 → 源地址重建 → Base64 + URLEncode → 拼 `onlinePreview` |
| `PreviewResult` | `{ previewable, previewUrl, reason }` |
| `FilePreviewVO` | 对外返回：`{ id, name, ext, size, previewable, previewUrl, downloadUrl }` |

核心逻辑（伪代码，编码规则需重点校验）：

```java
// 1) 源文件绝对地址重建
String sourceUrl = StrUtil.startWithAny(file.getUrl(), "http://", "https://")
        ? file.getUrl()
        : StrUtil.addSuffixIfNot(properties.getSourceBaseUrl(), "/") + file.getPath();

// 2) kkFileView 要求：url 参数 = Base64(源地址) 后再 URLEncode
String encoded = URLEncoder.encode(
        Base64.getEncoder().encodeToString(sourceUrl.getBytes(StandardCharsets.UTF_8)),
        StandardCharsets.UTF_8);

// 3) 拼接预览地址（fullfilename 用于让 kkFileView 判定格式，需 URLEncode 原始文件名）
String previewUrl = properties.getBaseUrl() + "/onlinePreview?url=" + encoded
        + "&fullfilename=" + URLEncoder.encode(file.getName(), StandardCharsets.UTF_8)
        + (StrUtil.isNotBlank(properties.getWatermark())
            ? "&watermarkTxt=" + URLEncoder.encode(properties.getWatermark(), StandardCharsets.UTF_8) : "");
```

### 5.2 接口新增（`AdminFileController` / `AppFileController`）

```
GET /admin/system/file/{id}/preview     权限 system:file:query
GET /member/file/{id}/preview           会员端：仅本人文件（复用 FileService.checkOwnership）
```

- 返回 `Result<FilePreviewVO>`；
- 复用 `SystemErrorCode.FILE_NOT_FOUND`；新增 `FILE_PREVIEW_UNSUPPORTED`（格式不支持时用 `previewable=false` 表达，不抛异常）；
- 管理端加 `@OperateLog(module = "文件管理", type = OperateTypeEnum.OTHER)` 记录预览行为。

### 5.3 依赖注入

`PreviewService` 由 `FileAutoConfiguration` 条件装配（`zhush.preview.enabled=true`），`zhush-module-system` 通过构造函数注入即可，无需新增 Maven 依赖。

## 六、前端改造点（vue-vben-admin）

| 文件 | 改动 |
|---|---|
| `src/api/system/file.ts`（新增或扩展） | `getFilePreview(id)` → `GET /admin/system/file/{id}/preview` |
| `src/utils/file-preview.ts`（新增） | 通用 `previewFile(id, option?)`：调接口 → 不可预览则 `message.warning` + 触发下载 → 可预览则 `window.open` 或打开弹窗 |
| `src/components/FilePreviewModal.vue`（新增） | 基于 `iframe` 的全屏/弹窗预览组件，支持 loading、失败重试、下载兜底 |
| 文件管理 / 通知公告附件 / 用户头像等入口 | 表格操作列增加「预览」按钮，调用 `previewFile(row.id)` |

要点：

1. **iframe 与 `X-Frame-Options`**：kkFileView 经 nginx 同源反代（`/preview`）后，iframe 可正常嵌入；若未来换独立域名需确认响应头未设置 `X-Frame-Options: DENY`。
2. **原始 URL vs 预览 URL**：现有 `resolveFileUrl()`（`src/views/system/common.ts`）只解决「图片/静态资源直链」；文档预览应统一走新的 `previewFile()`，两者职责不同、并存。
3. **移动端 WebView**：`zhush-admin-uni` 若需接入，iframe 兼容性差，建议改用 `window.open` 或新页面 `web-view` 承载预览地址（本期先确认是否需要）。
4. **雪球效应控制**：预览地址由后端返回，前端**不要**自行拼接 `onlinePreview`，避免双编码规则散落多处。

## 七、安全设计

| 风险 | 缓解措施 |
|---|---|
| **SSRF**（kkFileView 被诱导拉取内网资源） | `KK_TRUST_HOST` 白名单（通配/CIDR）；`not.trust.host` 黑名单优先；4.4.0+ 默认拒绝未配置来源 |
| **被当作上传/删除入口** | `file.upload.disable=true`；`/deleteFile` 保持默认禁用（**切勿**设置 `KK_DELETE_PASSWORD`）；nginx `limit_except GET HEAD` |
| **脚本注入** | `kk.scriptjs=false`（关闭用户脚本执行） |
| **内容外泄** | `watermark.txt` 水印（含操作人/时间可选）；`/preview` 及 `8012` 仅内网可达 |
| **直链可被转发** | **一期**：路径含 UUID 不可枚举 + 后端预览接口鉴权（生成地址时校验权限/归属）；**二期**：改签名直链或一次性 token（Redis 短时效），并在 `/api/files/**` 前加校验 |
| **越权预览** | 会员端复用 `FileService.checkOwnership`，仅本人文件可预览 |
| **审计缺失** | 预览接口 `@OperateLog` 落操作日志，可统计"谁预览了哪个文件" |
| **资源耗尽** | kkFileView 独立容器限 CPU/内存；缓存定期清理；必要时限制单文档转换页数/超时 |

## 八、格式支持与降级

kkFileView 支持 50+ 格式：Office（doc/docx/xls/xlsx/ppt/pptx）、WPS（wps/et/dps）、PDF、OFD、CAD、3D、压缩包、音视频、图片、邮件（eml/msg）、EPUB 等。

**降级策略**：

- `ext` 不在 `zhush.preview.allowed-extensions` → 返回 `previewable=false`，前端提示"该格式不支持在线预览，请下载后查看"并触发下载。
- 图片 / 纯文本 / PDF 可考虑**前端原生渲染**（`<img>` / `<iframe>`）优先，减少 LibreOffice 转换开销（二期可选优化）。
- kkFileView 转换失败 → 前端 `iframe` 内错误页 + 「下载原文件」兜底按钮。

## 九、性能与容量

- **转换开销**：Office → PDF 由容器内 LibreOffice 完成，属 CPU/内存密集操作，单次转换数百毫秒至数秒；建议给 kkFileView 容器独立配额，避免与 `zhush-server` 争抢（注意 `network_mode: host` 下仍可做 `deploy.resources.limits`）。
- **缓存**：`file.dir` 持久化转换结果，`forceUpdatedCache=true` 保证源文件变更后刷新；`cache.clean.cron` 定时清理（示例 `0 0 3 * * ?`）。
- **可选 Redis 缓存**：流量大时启用 `cache.type=redis` + `spring.redisson.address` 指向现有 Redis（`deploy/redis`）。
- **超时**：nginx `proxy_read_timeout 300s`；超大文档建议在前端给出加载提示。

## 十、分期实施计划与验收标准

### 一期（核心可用）

1. `deploy/kkfileview`：新增容器并跑通本机 `http://127.0.0.1:8012/onlinePreview?url=...` 自测。
2. `deploy/nginx`：新增 `/preview` 反代。
3. 后端：新增 `PreviewProperties` / `PreviewService` / `FilePreviewVO`，`AdminFileController` 新增 `GET /{id}/preview`。
4. 前端：`api` 方法 + `previewFile()` 工具 + `FilePreviewModal.vue`，接入文件管理与通知公告附件入口。

**验收标准**：

- [ ] 上传 docx / xlsx / pptx / pdf 后，管理端可在线预览，排版无明显错乱，中文不乱码。
- [ ] 预览接口未登录访问返回 401；无权限返回 403；不存在返回 `FILE_NOT_FOUND`。
- [ ] 不支持格式返回 `previewable=false`，前端引导下载。
- [ ] kkFileView 的 `/fileUpload`、`/deleteFile` 不可访问。
- [ ] 非白名单来源的 `onlinePreview` 请求被拒绝。
- [ ] 预览操作写入操作日志。

### 二期（安全与治理）

- 签名直链 / 一次性 token；水印动态化（操作人/时间）；Redis 缓存；格式白名单精调；预览次数统计。

### 三期（可选）

- 移动端 WebView 适配；图片/PDF 原生直显减负；预览埋点与容量监控。

## 十一、风险与待确认项

| # | 待确认项 | 建议 |
|---|---|---|
| 1 | kkFileView 版本 | **4.4.0**（JDK8 系、稳定、文档成熟）；5.x 强制 JDK21 且 Office 默认 PDF 模式，需评估运维成本 |
| 2 | 部署资源 | kkFileView 需独立 CPU/内存配额，确认服务器余量 |
| 3 | 是否对公网暴露 | 建议**仅内网**，统一经 nginx `/preview` 暴露 |
| 4 | 是否接受"直链免登录" | 一期接受（UUID 不可枚举 + 接口鉴权）；二期做签名直链 |
| 5 | 会员端（`zhush-admin-uni`）是否要预览 | 若不接入可只做 `AdminFileController` |
| 6 | 水印文案 | 是否需要"操作人 + 时间"，影响 `watermark.txt` 与后端传参 |
| 7 | 中文字体 | 确认宿主机 `/usr/share/fonts` 已含常用中文字体 |
| 8 | 前端入口范围 | 一期先做「文件管理 + 通知公告附件」，其余按需扩展 |

---

> 下一步：待上表确认项明确后，按「十、分期实施计划」的一期清单进入编码（新增容器/nginx/后端接口/前端入口四部分）。
