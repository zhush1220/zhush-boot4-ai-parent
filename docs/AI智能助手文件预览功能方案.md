# AI 智能助手文件预览功能方案

> 模块归属：`vue-vben-admin/apps/web-antdv-next`（AI 助手会话页，主战场）、`zhush-module-ai`（产物元信息接口）、`zhush-spring-boot-starter-file`（文件访问与预览门面）、`zhush-module-system`（文件记录与鉴权）
> 覆盖格式：图片、视频、HTML、Markdown、PDF、Word（docx/doc）、Excel（xlsx/xls/csv）、PPT（pptx，次级）
> 现有能力：产物已落 `infra_file` 并回链 `fileId`；图片/视频已内联渲染；其余格式**只有下载**
> 外部依赖：默认路线零新增服务（纯前端库）；资源充足时按门禁升级为 LibreOffice 内嵌或 kkFileView 独立服务
> 文档状态：**方案待确认，本期不写实现代码**（确认后按第十章分期清单开工）

## 〇、结论速览

1. **先测服务器余量，再选路线**。本方案是**双路线**设计：服务端能力用 `zhush.preview.office-converter` 一个开关切换，**前端契约（`previewMode` 分派）在两条路线下一字不改**。资源门禁档位见 §0.1。
2. **路线乙｜资源紧张（默认，也是任何情况下的兜底）：纯前端渲染，零新增服务**。AI 会话里的产物天然小（文档受 `AgentDocumentService` 限制、图像/视频受 `zhush.file.max-size=50MB` 限制），浏览器侧渲染栈（pdf.js + docx-preview + exceljs）足以覆盖 `png/jpg/mp4/html/md/pdf/docx/xlsx/csv` 九类，占实际产物的绝大多数。代价是 `.doc`/`.xls`/`.ppt` 这类二进制旧格式**明确只下载、不做假预览**。
3. **路线甲｜资源充足：在服务端补一层格式转换，把浏览器渲染不了的旧格式统一收敛成 PDF**（不是 HTML——HTML 直渲会多开一个可嵌入的服务面，见第八章 S10）。两种实现按档位选：
   - **甲-1 LibreOffice headless 内嵌**（`jodconverter-local:5.1.1` 或裸 `ProcessBuilder`）：无第二容器，+400～700MB 镜像、单次转换 300～500MB 峰值内存，产物仍是 PDF → **前端仍用同一个 pdf.js 组件渲染**，改动最小。**推荐作为「服务器资源充足」的第一选择**。
   - **甲-2 kkFileView 5.0.3 独立服务**：JDK21 + 内置 LibreOffice，空载 RSS ≈1.5GB、官方建议 2～4GB；换来的是 50+ 格式广度（`ofd`/CAD/压缩包/邮件）。仅当业务确实需要这层广度、且能给独立 CPU/内存配额时才上。
4. **OnlyOffice / Collabora 两条路线都排除**：官方系统要求 4GB 内存起（OnlyOffice Docs CE 10.0 Docker 最低 2GHz/4GB/40GB + 自带 Postgres/Redis/RabbitMQ），与 `zhush-server` 争抢同一台小云主机不现实；且 OnlyOffice 是 AGPLv3。
5. **本方案带三条必须先补的安全前置项**（第八章）：预览把「下载后在本地打开」变成了「在我们自己的源里执行」，同源 HTML/SVG 存储型 XSS、免鉴权公开直链、扩展名白名单声明未生效这三件事，在预览上线前必须收口。**注意 S1 在路线甲-2 下更严重**（kkFileView 会直接把 Office 文档转成 HTML/PDF 对外渲染，多出一个可被 iframe 嵌入的服务面）。
6. **既有的《kkFileView 在线预览整合方案.md》至今未落地**（全仓无 `com.zhush.file.preview` 包、无 `zhush.preview.*` 配置、nginx 无 `/preview` location），本方案把它从「主方案」调整为「路线甲-2 的实现蓝本」，并复用其接口形状（`{previewable, previewUrl}`）与安全清单。

### 0.1 资源门禁（决定走哪条路线，开工前唯一要做的判断题）

| 档位 | 判定条件（宿主机实测） | 采用路线 | 新增成本 | 换来什么 |
|---|---|---|---|---|
| **C 紧张** | 空闲内存 < 1.5GB **或** `zhush-server` 常驻内存已 > 主机 60% | **路线乙**（纯前端） | 0 服务端资源，仅前端懒加载 chunk | doc/xls/ppt 只能下载 |
| **B 中等** | 空闲内存 1.5～3GB，≥2 vCPU，磁盘富余 ≥3GB | **路线乙 + 甲-1**（LibreOffice 内嵌） | +400～700MB 镜像；转换进程峰值 300～500MB，需串行化 | doc/xls/ppt 转 PDF 后可预览 |
| **A 充足** | 空闲内存 ≥4GB 且 ≥4 vCPU，可给独立容器配额 | **路线乙 + 甲-2**（kkFileView 独立服务） | 独立容器 ≈1.5GB RSS + 0.7～1.5GB 镜像 + JDK21 运维口径 | 50+ 格式广度，前端零依赖 |

**实测口径**（在部署机上执行，结果决定档位，别凭印象）：

```bash
free -m                      # 看 available 列，不是 free 列（cache 可回收）
nproc                        # 可用核数
df -h /data/upload /var/lib/docker   # 转换缓存与镜像需要磁盘
docker stats --no-stream     # zhush-server / nginx / redis / mysql 实际占用
```

**门禁原则**：

1. **路线乙永远是基线**。即使走路线甲，前端渲染栈（图片/视频/md/html/pdf）**照旧保留**——服务端转换只用来补 `.doc/.xls/.ppt` 和低保真格式，不用它渲染图片视频（否则给一次预览凭空加一次网络往返与转换耗时）。
2. **降级不报错**。`office-converter` 关闭或转换服务不可达时，`previewMode` 回落 `unsupported` + 文案「该格式需下载查看」，会话功能不受影响；**不能因为预览服务挂了就阻断对话**。
3. **随时可升降档**。因为前端只认 `previewMode`，从 C 档升到 B/A 档属于**纯运维变更（改配置 + 起容器），不需要前端发版**。这是本方案把格式判断权收到后端的主要原因。
4. **转换必须限流**。路线甲任一实现都要给转换加进程/队列上限（LibreOffice 单进程串行或固定 2 实例；kkFileView 给 `deploy.resources.limits`），否则一份大 PPT 转换能把 `zhush-server` 一起拖死——这是本方案唯一的自伤型风险。

---

## 一、需求与范围

### 1.1 功能诉求

| 编号 | 诉求 | 说明 |
|---|---|---|
| R1 | 会话内产物点击即预览 | 图片灯箱、视频播放、文档内嵌渲染，不打新窗口也不下载 |
| R2 | 统一预览入口 | 一个组件/一个工具函数按格式分派，各业务页可复用，不在页面里散落 `window.open` |
| R3 | 不可预览格式明确降级 | 返回 `previewable=false` + 原因文案，引导下载，不给白屏 |
| R4 | 历史消息同样可预览 | 不能只有本轮流式产物能预览，刷新页面回显的历史必须一致 |
| R5 | 预览行为可审计、可鉴权 | 谁预览了哪个文件；越权预览被拒 |

### 1.2 预览对象盘点（格式 × 来源 × 现状）

| 格式 | 来源 | 现状 | 路线乙（默认，纯前端） | 路线甲（资源充足时增益） |
|---|---|---|---|---|
| png / jpg / gif / webp | `generateImage` 工具（异步任务转存） | **已内联 `<img>`** | 补灯箱放大、失败重试 | 同乙（**不走服务端**，见 §0.1 门禁原则 1） |
| mp4 | `generateVideo` 工具 | **已内联 `<video controls>`** | 校验 Range 流式、补全屏/倍速 | 同乙 |
| pdf | `generateDocument(format=pdf)`、用户上传 | 仅下载卡片 | **前端 pdf.js 渲染**（注意 5.5 CJK 坑） | 同乙 |
| docx | `generateDocument(format=docx)`、用户上传 | 仅下载卡片 | **前端 docx-preview 渲染** | 可选改 LO→PDF，保真度更高（V3 不达标时启用） |
| md | 用户上传（`accept` 含 `.md`） | 仅下载/作为 prompt 文本 | 前端 marked 渲染 | 同乙 |
| html | 用户上传（`accept` 含 `.html`）、后续网页类产物 | 无 | **sandbox iframe**（安全重点） | 同乙（**禁止**交给 kkFileView 直渲，见 S1） |
| xlsx / csv | 用户上传 | 无 | 前端 exceljs 表格渲染 | 大表/复杂样式可改 LO→PDF |
| **doc / xls / ppt** | 用户上传 | 无 | **仅下载**（`previewable=false`） | ✅ **转 PDF 后预览**（甲-1 或甲-2，这是路线甲的主要动机） |
| pptx | 用户上传 | 无 | 次级，视保真度决定是否收录（D4） | ✅ LO/kkFileView 转 PDF，保真度明显优于 `@vue-office/pptx` |
| ofd / wps / 压缩包 / CAD | — | 无 | 仅下载 | 仅 **甲-2（kkFileView）** 支持，甲-1 不覆盖 |
| svg | 未在 `accept` 内，但可被 `generateImage` 之外的链路产出 | 无 | **禁止内联执行**（第八章 S1） | 同样禁止内联，一律下载 |

### 1.3 非目标（本期不做）

- 在线编辑、批注、协同（OnlyOffice/Collabora 的核心卖点，也是其体量来源）。
- 移动端 `zhush-admin-uni` 预览（该端已有产物提取逻辑，但小程序 `openDocument` 对 office 支持受限，需等服务端 PDF 化后再适配）。
- 大文件分片/断点续传、CDN 分发。
- 预览版本的水印（列为阶段三，取决于第十一章待确认项 D5）。

---

## 二、现状取证（代码事实）

以下均为本次实测结论，直接决定方案形状。

### 2.1 产物链路已具备预览所需的元数据

- `FileArtifact`（[zhush-module-ai/.../generation/FileArtifact.java:20-45](file:///D:/zhush/project/back-boot/zhush-boot4-parent/zhush-module-ai/src/main/java/com/zhush/ai/generation/FileArtifact.java#L20-L45)）字段为 `type`(file/image/video) / `format` / `name` / `url` / `size` / `fileId`。**`fileId` 即 `infra_file.id`**，是「按 ID 预览」的接口前提。
- 文档产物：`AgentDocumentService.generate()` 经 `FileRecordStore.upload(...)` 落库并回填 `fileId`（[AgentDocumentService.java:106](file:///D:/zhush/project/back-boot/zhush-boot4-parent/zhush-module-ai/src/main/java/com/zhush/ai/generation/AgentDocumentService.java#L106)）；**格式白名单硬编码为 `docx`/`pdf`**（同文件 :88），md/html/xlsx 目前不会由 AI 产出。
- 图像/视频：`AiGenerationTaskService.transferAndComplete()` 从供应商下载后转存文件服务并回填 `fileId`（[AiGenerationTaskService.java:278](file:///D:/zhush/project/back-boot/zhush-boot4-parent/zhush-module-ai/src/main/java/com/zhush/ai/service/AiGenerationTaskService.java#L278)），产物体积受 `zhush.file.max-size` 约束（[RemoteFileFetcher.java:28](file:///D:/zhush/project/back-boot/zhush-boot4-parent/zhush-module-ai/src/main/java/com/zhush/ai/generation/RemoteFileFetcher.java#L28)、:170）。
- 落库结构：`ai_chat_message.attachments` 是 **JSON 列**（[sql/25_agent_capability.sql:20](file:///D:/zhush/project/back-boot/zhush-boot4-parent/sql/25_agent_capability.sql#L20)），由 `AgentChatService` 从工具结果事件提取后写入。**给附件加字段不需要 DDL 变更**，这是本方案成本低的关键原因之一。
- 注意：`FileClient` 本身不写库，落库靠 `FileRecordStore` SPI；若部署形态未引入 module-system，`NoopFileRecordStore` 会返回 `id=null` → 预览接口对这类产物不可用，必须回落到「按 URL 预览」。

### 2.2 前端渲染现状：只有下载卡片

[message-bubble.vue:276-322](file:///D:/zhush/project/back-boot/vue-vben-admin/apps/web-antdv-next/src/views/ai/assistant/modules/message-bubble.vue#L276-L322) 的附件块共四个分支：生成中占位、失败占位、`<img>`、`<video>`，最后 `v-else` 是一条 `<a>` 下载卡片（图标映射 `attachmentIcon()` :91-99、大小 `formatSize()` :77-88、下载逻辑 `downloadWithFallback()` :113-129）。**预览的插入点就是这条 `v-else` 分支**——把它从「一个下载链接」升级为「预览 + 下载两个动作」。

- 附件模型 `ChatAttachment`（[use-chat-stream.ts:28-46](file:///D:/zhush/project/back-boot/vue-vben-admin/apps/web-antdv-next/src/hooks/use-chat-stream.ts#L28-L46)）当前**不含 `fileId`**，虽然后端 `FileArtifactVO` 已有（[api/ai/types.ts:283-290](file:///D:/zhush/project/back-boot/vue-vben-admin/apps/web-antdv-next/src/api/ai/types.ts#L283-L290)）。→ 阶段一需补齐前端类型透传。
- `requestClient.download<Blob>(url)` 默认返回 Blob（[downloader.ts:25-34](file:///D:/zhush/project/back-boot/vue-vben-admin/packages/effects/request/src/request-client/modules/downloader.ts#L25-L34)），**已具备带鉴权头取字节流的能力**，前端渲染（pdf.js/exceljs/marked）拿数据不需要新接口。
- Markdown 渲染栈（marked 18 + highlight.js 11 + DOMPurify 3.4）已在同文件 :58-74 就绪，`.md` 预览可直接复用，零新增依赖。
- 图片灯箱能力已存在但藏在上传适配器里：`previewImage()`（[adapter/component/index.ts:253-329](file:///D:/zhush/project/back-boot/vue-vben-admin/apps/web-antdv-next/src/adapter/component/index.ts#L253-L329)）动态挂载 antdv-next `Image` + `PreviewGroup`，非图片回退 `window.open`。→ 提取为公共工具即可复用，**不必引入 photoswipe/medium-zoom**。
- 全仓**没有任何 office/pdf 预览依赖**（无 vue-office / pdfjs-dist / xlsx / mammoth），AI 页面**没有任何 `<iframe>`**。

### 2.3 文件访问链路（决定预览可行性与安全性）

| 事实 | 出处 | 对预览的影响 |
|---|---|---|
| `/api/files/**` 在 Sa-Token 免登录白名单内 | [application-dev.yaml:130](file:///D:/zhush/project/back-boot/zhush-boot4-parent/zhush-server/src/main/resources/application-dev.yaml#L130)（四个环境一致） | `<img>`/`<video>`/`<iframe>` 可**直链加载**，不必先换 token；代价是直链即凭证（S2） |
| dev：`domain: http://localhost:9166/api/files/` 为**绝对地址** | application-dev.yaml:171 | 前端（5666）跨域取字节，依赖 CORS |
| docker：`FILE_DOMAIN=/api/files/` 为**相对前缀** | [application-docker.yaml:186](file:///D:/zhush/project/back-boot/zhush-boot4-parent/zhush-server/src/main/resources/application-docker.yaml#L186)、[deploy/backend/docker-compose.yaml:32](file:///D:/zhush/project/back-boot/zhush-boot4-parent/deploy/backend/docker-compose.yaml#L32) | 生产同源加载，无 CORS 问题；但**服务端转换器拿不到绝对地址**（kkFileView 方案「决策一」同源问题） |
| CORS `allowed-origin-patterns: "*"` + `allowCredentials: true` | application-dev.yaml:207-215、docker.yaml:223-231 | 前端 Blob 拉取当前可用；生产建议收敛（S4） |
| nginx 站点：`/`（前端 dist）与 `/api`、`/files`（后端）**同一源** | [deploy/nginx/default.conf:5-30](file:///D:/zhush/project/back-boot/zhush-boot4-parent/deploy/nginx/default.conf#L5-L30) | **同源 HTML/SVG = 存储型 XSS 直达管理端**（S1，本方案最高风险项） |
| nginx **未设置任何 `add_header`**（实测 `add_header` 命中 0 次） | 同上 | 无 CSP、无 `X-Frame-Options` → iframe 预览可嵌入（好），但也意味着 S1 无兜底 |
| 本地文件由 `ResourceHttpRequestHandler` 静态映射提供 | [FileAutoConfiguration.java:104-107](file:///D:/zhush/project/back-boot/zhush-boot4-parent/zhush-framework/zhush-spring-boot-starter-file/src/main/java/com/zhush/file/config/FileAutoConfiguration.java#L104-L107) | 该 handler 支持 `Range/206` 与 `Last-Modified`，视频与 pdf.js 渐进加载理论可行；**需实测经 nginx 反代后 206 是否保留**（第九章 V1） |
| 扩展名白名单**已生效，只是四个环境都没配** | `FileStorageProperties.allowedExtensions`（[:34](file:///D:/zhush/project/back-boot/zhush-boot4-parent/zhush-framework/zhush-spring-boot-starter-file/src/main/java/com/zhush/file/config/FileStorageProperties.java#L34)）由 `FileClient.validateSizeAndExtension` 实际校验，四个环境 yaml 该项均为注释 ⇒ 当前等价于「不限制」 | 缺的是**配置**而非校验代码；预览侧另设 `zhush.preview.allowed-extensions`，两个口径不共用（S3 据此修正） |

### 2.4 已识别缺口

| 编号 | 缺口 | 归属 |
|---|---|---|
| G1 | 附件只有 `url` 无 `contentType`；前端类型未透传 `fileId` | 前端 + DTO |
| G2 | 无预览元信息接口，无「是否可预览」判定 | 后端 |
| G3 | 无统一预览组件与格式分派 | 前端 |
| G4 | 用户上传附件只以 `【附件】name: url` 文本行注入 prompt（[assistant/index.vue:180-186](file:///D:/zhush/project/back-boot/vue-vben-admin/apps/web-antdv-next/src/views/ai/assistant/index.vue#L180-L186)），**user 消息不落 `attachments`**，历史里退化成一行裸文本、无法预览 | 前后端 |
| G5 | 同源可执行内容无隔离（S1） | 安全 |
| G6 | `system:file:query` 对 admin **不做归属校验**（`checkOwnership` 仅约束 member，[FileService.java:182-189](file:///D:/zhush/project/back-boot/zhush-boot4-parent/zhush-module-system/src/main/java/com/zhush/system/service/FileService.java#L182-L189)），任何管理员可预览任意文件 | 安全 |
| G7 | 既有 kkFileView 方案未落地，预览门面缺位 | 后端 |

---

## 三、方案选型对比

### 3.1 约束（不是结论）

单机云主机（Docker host 网络 + nginx 统一入口，`zhush-server` 已在内），文件当前为**本地盘**存储（`zhush.file.primary=local`，`FILE_BASE_PATH=/data/upload`）。**常驻服务要与后端争内存，所以「能不能上服务端方案」必须由实测余量决定，而不是由偏好决定**——这就是 §0.1 门禁存在的原因。本方案刻意把两条路线都设计成可运行形态：主机余量变化时（扩容、加内存、拆分服务）预览能力可以随之升级，代码不用改。

### 3.2 四条路线

| 路线 | 代表 | 版本（2026-10 实测） | 许可证 | 部署代价 | 保真度 | 结论 |
|---|---|---|---|---|---|---|
| **A 纯前端渲染** | pdf.js + docx-preview + exceljs | `vue-pdf-embed@2.1.7`（依赖 `pdfjs-dist@6.4.299`）、`@vue-office/docx@1.6.3`、`@vue-office/excel@1.7.14` | MIT / Apache-2.0 | **0 服务**，仅 bundle 增量（全懒加载） | pdf 高 / docx 中 / xlsx 中 | ✅ **两档路线的共同基线**（C 档独占） |
| **B LibreOffice 内嵌转换** | `jodconverter-local:5.1.1` 或裸 `ProcessBuilder` 调 `soffice --headless --convert-to pdf`，产物用 pdf.js 渲染 | 5.1.1（Java 17+） | Apache-2.0（LO 为 MPL） | +400～700MB 镜像，**无第二容器**，一进程一转换（需串行/限流） | 高（LO 引擎） | ✅ **路线甲-1**：B 档（空闲 1.5～3GB）首选，作为 A 的**补集**而非替代 |
| **C kkFileView 独立服务** | keking/kkFileView | **5.0.3（2026-10-08）**；5.0.0 起要求 **JDK21+**、Office 默认 PDF 模式、Redis 可选 | Apache-2.0 | 镜像约 0.7～1.5GB，空载 RSS ≈1.5GB，官方建议 2～4GB | 广（50+ 格式：ofd/CAD/压缩包/邮件） | ✅ **路线甲-2**：A 档（空闲 ≥4GB、≥4 vCPU）且需要格式广度时选；须给 `deploy.resources.limits` |
| **D OnlyOffice / Collabora** | Docs CE 10.0.0 / CODE 25.04 | — | **AGPLv3** / MPL-2.0 | OnlyOffice 官方最低 **2GHz/4GB/40GB/4GB swap** 且捆绑 PG+Redis+RabbitMQ；Collabora 自带「不建议用于生产」 | 最高 | ❌ **两档都排除**（体量与许可双重不合；A 档也不选，因为它的增益是「可编辑」，非本需求） |
| 云 Office 预览 | 阿里云 IMM/WebOffice | — | 商业 | 依赖 OSS（当前本地盘，改造成本） | 高 | ❌ 不选，仅作成本对照（预览 ¥10/千次、转换 ¥80/千次） |

### 3.3 路线甲与路线乙的取舍边界（不是「要不要上 kkFileView」）

既有《kkFileView 在线预览整合方案.md》把 kkFileView 定位成**唯一门面**——这一点本方案不认同，但把它定位成**A 档的可选实现**完全成立。三条边界划清楚即可：

1. **不要用服务端渲染图片/视频/md**。这三类前端零成本、零延迟、零转换开销；走 kkFileView 反而多一次回源 + 一次转换排队。**服务端只负责浏览器做不了的那部分**：`.doc/.xls/.ppt`（以及可选的 pptx/复杂 docx 高保真）。
2. **kkFileView 的不可替代性只在「格式广度」**：`ofd`/CAD/压缩包/邮件。若业务没有这些，**甲-1（LibreOffice 内嵌）严格优于甲-2**：少一个容器、少一次 `source-base-url` 绝对地址重建（docker 环境 `FILE_DOMAIN=/api/files/` 是相对前缀，kkFileView 回源必须靠 `zhush.preview.source-base-url`，这是它独有的集成摩擦）、少一套 JDK21 运维口径，而且产物仍是 PDF → 前端复用同一个 pdf.js 组件，**不引入第三方渲染面**。
3. **安全面随路线递增**。甲-2 多暴露一个 `/preview` 服务（须 `KK_FILE_UPLOAD_DISABLE`、`KK_TRUST_HOST`、nginx `limit_except GET HEAD`）且会把文档转成可对外渲染的 HTML/PDF；甲-1 不新增网络面。**这是「资源充足也不该无脑上 kkFileView」的第二个理由。**

→ 结论：**基线恒为 A；B 档加甲-1；A 档且确有广度需求才上甲-2。** 三者对前端都是同一个 `previewMode` 契约。

---

## 四、推荐架构：统一预览门面 + 格式路由

### 4.1 分层（双路线共用同一契约）

```
                    ┌── 附件卡片（message-bubble.vue 现有 v-else 分支升级）
                    │      预览 / 下载 两个动作
                    ▼
会话层  AttachmentPreview.vue（新增，defineAsyncComponent 懒加载）
                    │  输入：ChatAttachment{fileId,url,format,contentType,size}
                    ▼
路由层  后端 previewMode（唯一裁决权）
        inline-image | inline-video | inline-text | iframe-sandbox
        | client-pdf | client-docx | client-sheet
        | server-pdf（甲-1 LibreOffice 出 PDF）
        | server-iframe（仅甲-2：kkFileView 自带渲染页）
        | unsupported
          ↑ 前端本地 previewKind(att) 仅作「后端不可达时」的降级推断
                    ▼
渲染层  Image.PreviewGroup │ <video> │ marked+hljs │ sandbox iframe │ pdf.js │ docx-preview │ exceljs 表格
        （server-pdf 复用同一个 pdf.js 组件；server-iframe 复用同一个 sandbox iframe 组件）
                    ▼
数据层  二进制：requestClient.download(url) → Blob → ObjectURL / ArrayBuffer
        文本：  GET /admin/ai/agent/file/{id}/preview-meta（+ 可选 /content）
```

要点：
- **裁决权在后端，前端只分派**：路线从乙升到甲 = 后端把 `.doc` 的 `previewMode` 从 `unsupported` 改成 `server-pdf`，前端**一行代码不用改、不用发版**。这是本方案对「先按现有资源上线、以后资源宽裕了再增强」这一现实节奏的唯一解法。
- **单一入口**：所有页面（AI 会话、文件管理、通知公告附件）后续都调同一个 `previewFile(attachment|fileId)`，格式判断、降级、审计都在一处，避免「双编码/拼接规则散落多处」的老问题。
- **全部懒加载**：pdf.js、docx-preview、exceljs 都是数百 KB～1MB 级依赖，必须 `defineAsyncComponent` 按 `previewMode` 触发；A 档若走 `server-iframe`，这三个依赖对旧格式路径根本不加载。
- **版本进 catalog**：按仓库 BOM 口径写入 `pnpm-workspace.yaml` catalog（现状仅 `antdv-next: ^1.4.5` 等在 catalog），应用侧写 `"@vue-office/docx": "catalog:"`，**并锁精确版本**（见 5.6 关于 vue-office 维护停滞的说明）。

### 4.2 格式路由表

| 判定 | 取数方式 | 渲染 | 路线甲覆盖 | 失败降级 |
|---|---|---|---|---|
| image (png/jpg/gif/webp) | 直链 `<img>` | antdv-next `Image` + `PreviewGroup` 灯箱 | 不覆盖（保持前端） | 下载 |
| video (mp4/webm) | 直链 `<video>` | 原生播放器 + `preload="metadata"` | 不覆盖 | 新窗口直链 |
| markdown (md) | `download()` 取文本 | 复用 marked + hljs + DOMPurify | 不覆盖 | 纯文本 `<pre>` |
| html | `download()` 取文本 | `<iframe sandbox="allow-scripts" :srcdoc>` | **仍不覆盖**（禁交 kkFileView 直渲，S1） | 纯文本视图 + 下载 |
| pdf | 直链或 Blob | pdf.js（见 5.5 CJK 坑） | 不覆盖 | 新窗口打开直链（浏览器内置查看器） |
| docx | Blob | `@vue-office/docx` | 甲-1/甲-2 可改 `server-pdf`（V3 不达标时启用） | mammoth 语义 HTML → 再降级下载 |
| xlsx / csv | Blob | `@vue-office/excel`；csv 走轻解析 | 甲-1/甲-2 可改 `server-pdf`（复杂样式/大表） | 下载 |
| **doc / xls / ppt** | — | 乙：`previewable=false`，文案「旧版格式请下载后用 Office 打开」 | ✅ **甲-1：`server-pdf`（LO 转 PDF）**；甲-2：`server-iframe`（它给不出 PDF 字节，见 §15.1） | 下载 |
| pptx | Blob | `@vue-office/pptx@1.0.1`，**默认不收录**（保真度最差，D4） | ✅ 甲路线转 PDF 后保真度明显更好，建议随甲启用 | 下载 |
| ofd / wps / 压缩包 / CAD | — | 乙：下载 | ✅ **仅甲-2（kkFileView）**支持 | 下载 |
| svg | — | **不内联**（S1），两档路线一律 unsupported | 不覆盖 | 下载 |

### 4.3 交互形态

1. **图片/视频**：会话气泡内保持现有内联缩略图，点击进灯箱（多图共享 `PreviewGroup`，支持左右切换）。
2. **文档类**：气泡卡片加「预览」按钮 → 打开右侧**大尺寸弹窗**（`useVbenModal`，全屏/最大化；注意已知约束：**`useVbenModal` 不支持 `width` 属性**，需用 `class`/样式或 `fullscreen` 控制尺寸）。
3. **弹窗工具条**：文件名 / 大小 / 格式徽标 / 下载 / 新窗口打开 / 关闭。
4. **加载态与兜底**：转换或渲染超过阈值给骨架；失败必须露出「下载原文件」，不给空白 iframe。
5. 可选增强（阶段三）：会话页右侧常驻预览分栏，边看对话边翻产物。

---

## 五、各格式实现要点与坑（先读这章再开工）

### 5.1 图片

复用现成 `previewImage()` 模式即可，**不引入新依赖**。注意 `message-bubble.vue` 的 scoped 样式已有 `.message-markdown :deep(img)` 规则（同文件 :474），AI 正文里内嵌的图也应接灯箱，属同一套。

### 5.2 视频

内联 `<video>` 已可用，阶段一只需两件事：确认 Range 流式（V1）、加 `playsinline` 与错误重试。**50MB 上限**（`zhush.file.max-size`）意味着拖进度条必须有 206，否则整段重下。若后续要 HLS/自适应码率再上 `xgplayer@3.0.26`（MIT，字节跳动出品），当前无需。

### 5.3 Markdown

零新增依赖。取 Blob → `text()` → 现有渲染链。两个必须的处理：**先 DOMPurify**（AI/用户上传的 md 允许内嵌原始 HTML，当前会话正文已这么做，产物预览不能例外）；**大文件截断**（如 >1MB 只渲染前若干块并提示「内容过长，建议下载」），避免长表格/代码块把主线程打满。

### 5.4 HTML（本方案最需要小心的格式）

`accept` 里已经允许 `.html` 上传，预览等于「在我们的源里跑别人的 HTML/JS」。规矩只有一条：

- **iframe 只给 `sandbox="allow-scripts"`，绝不与 `allow-same-origin` 并存**（同时给等于沙箱失效，脚本可拿 `parent`/同源 storage）。
- 内容用 `srcdoc`（或 Blob URL）注入而非直链导航；Blob URL 需同步放行 CSP（当前无 CSP，见 S1 的响应头方案）。
- 不要用 DOMPurify 洗过后直接 `v-html`：洗掉 `<style>`/布局后「HTML 报告」就没有意义了，沙箱 iframe 才是正解。
- 配套服务端加固（同一目标、纵深防御）：对 `/api/files/**` 的 `.html`/`.svg` 响应追加 `Content-Security-Policy: sandbox allow-scripts`、`X-Content-Type-Options: nosniff`、`Content-Disposition: inline`；这样**用户手动粘贴直链到地址栏也不会中招**，而 iframe 内的 srcdoc 渲染不受影响。

### 5.5 PDF —— 本项目特有的中文字体坑（必读）

`AgentDocumentService` 生成 PDF 用的是 `BaseFont.createFont("STSongStd-Light","UniGB-UCS2-H", BaseFont.NOT_EMBEDDED)`（[AgentDocumentService.java:210](file:///D:/zhush/project/back-boot/zhush-boot4-parent/zhush-module-ai/src/main/java/com/zhush/ai/generation/AgentDocumentService.java#L210)，:39 的类注释也明确写了「非嵌入，阅读器需自带宋体渲染」）。**字体没有嵌进 PDF 文件**，浏览器内置查看器靠系统字体能出字，但 pdf.js 是自带渲染器的：

- 必须让 pdf.js 拿到 CJK cmaps，否则**中文直接空白或豆腐块**，而这正是 AI 文档产物的主体内容。
- **V2 已验证并落地为自建组件**（不引 `vue-pdf-embed`）：`pdfjs-dist@6.4.299` 已删除 `CMapReaderFactory`，
  改用 `getDocument({ BinaryDataFactory, cMapPacked: true, data })`——自定义工厂实现
  `fetch({ kind, filename })` 返回 `Uint8Array` 即可（`kind ∈ cMapUrl / standardFontDataUrl / wasmUrl`）；
  传自定义工厂时 `useWorkerFetch` 自动为 false，取数回到主线程。
- cmap 交付方式：**不拷 `cmaps/` 整个目录**（1.5MB/169 文件进 git 不值），只把生成端实际用到的
  `UniGB-UCS2-H/V`、`UniGB-UTF16-H` 三个 `.bcmap` 用 `?url` 导入交给工厂映射。Vite 会把它们
  打成 `dist/bcmap/*.bcmap` 资源（小于内联阈值的自动变成 data URL，`fetch()` 两种都能读）。
  代价：预览**非本系统生成**的、用了其它 CMap 的 PDF 时中文可能缺字（会打 warn 后继续渲染），
  要全覆盖就改回「拷全目录 + `cMapUrl: '/pdfjs/cmaps/'`」。
- worker 同批处理：`import workerAsset from 'pdfjs-dist/build/pdf.worker.min.mjs?url'` →
  `GlobalWorkerOptions.workerSrc`（oxlint 的 `import/default` 会误报，需一行 disable）。
- 根因侧的另一半选择：**把生成端改成字体嵌入**，可同时解决「桌面阅读器无宋体」与「pdf.js 无 cmap」两个问题，代价是产物体积增加与需要字体文件授权。列为待确认项 **D2**。
- 版本副作用：`pdfjs-dist@6.4.299` 的 `engines` 要求 **`node >= 22.13 || >= 24`**。本地 Node 24.18.0 满足，但 **CI/构建镜像的 Node 版本必须同步核对**，否则 `pnpm install` 或直接构建会炸。

### 5.6 Word

- `docx` 走 `@vue-office/docx@1.6.3`（内部封装 `docx-preview`）。页眉页脚、文本框、分栏会漂，但我们的 docx 由 POI XWPF 生成、结构极简（标题/段落/列表/代码/表格），漂移风险低；用户上传的复杂 Word 需在文案上明确「预览仅供参考，以下载文件为准」。
- 两个工程细节：① peer 依赖含 `vue-demi@^0.14.6`，其 **postinstall 脚本决定走 Vue2 还是 Vue3 入口**；本仓库 pnpm 11.x 默认拦截构建脚本，**必须在 `allowBuilds` 里放行 `vue-demi`**，否则运行期报找不到 `setup` 一类错误（这是本仓库已知机制）。② `@vue-office/*` 把依赖打包进 dist、且 CSS 需**显式 import**。
- 上游维护节奏自 2024-12 起基本停滞 → **catalog 锁精确版本 + 走 pnpm 离线镜像缓存**，别用 `^`。
- `doc`（二进制）客户端无解：路线乙 unsupported，路线甲走 LibreOffice→PDF。`mammoth@1.13.0`（BSD-2）只作 docx 兜底——它输出语义 HTML 而非版式，长得很不像 Word，不能作为主呈现。

### 5.7 Excel

- `xlsx` 走 `@vue-office/excel@1.7.14`（内含 `exceljs@4.4.0` + x-data-spreadsheet）；**明确不支持 `.xls`**，xls 在路线乙下 unsupported，路线甲转 PDF。
- **不要装 npm 上的 `xlsx`**：registry 版本冻结在 0.18.5 已多年未更新，且带原型污染/DoS 安全公告，官方发行早已迁到自有 CDN。若需要自己解析（而非借用 vue-office），用 npm 上的镜像 **`@e965/xlsx@0.20.3`**（Apache-2.0，可进 catalog）。
- `x-data-spreadsheet`（1.1.9）已停止维护；`handsontable@18.1.2` 是**商用付费 / GPLv3 非商用**，对管理后台属许可陷阱，排除。若未来要「可编辑表格」再评估 Univer（`@univerjs/core@1.0.4`，Apache-2.0），但它是 MB 级体量，预览场景用不上。
- `csv` 用 `@e965/xlsx` 解析或 `papaparse` 类轻解析 + 现有 `Table`（antdv-next）渲染即可，注意 GBK/UTF-8 探测与行数上限截断。

### 5.8 PPT 与其他

`@vue-office/pptx@1.0.1`（2025-01）是本组里保真度最差的，且我们没有任何 PPT 产物来源（`generateDocument` 只出 docx/pdf），只服务用户上传——收益低。**路线乙下建议不做**（等 D4 确认）；**路线甲下 pptx 反而是明确增益**（转 PDF 保真度远高于该库），随甲启用即可，前端零改动。ofd/wps/压缩包/CAD 只有甲-2（kkFileView）覆盖，其余情况一律下载。

---

## 六、后端改造点

### 6.1 新增预览元信息接口

```
GET /admin/ai/agent/file/{fileId}/preview-meta     权限 ai:assistant
→ Result<FilePreviewMetaVO>{ id, name, ext, size, contentType,
                             previewable, previewMode, reason, accessUrl }
```

- `previewMode ∈ {inline-image, inline-video, inline-text, iframe-sandbox, client-pdf, client-docx, client-sheet, server-pdf, server-iframe, unsupported}`（与 §4.1 一致）：把「该用什么渲染」的判断权收到后端，前端只做分派。**这是双路线能热切换的关键**——升档时后端改枚举返回值即可，新增格式也不必发版前端。
- 接口内部按 `zhush.preview.office-converter` 决定 `doc/xls/ppt` 返回 `server-pdf` / `server-iframe` 还是 `unsupported`；**转换服务不可达时必须回落 `unsupported` 而非 500**（§0.1 门禁原则 2）。
- **鉴权不能只靠 `system:file:query`**（G6：admin 无归属校验）。此处应断言「该 `fileId` 属于当前登录管理员可访问的会话产物」：由 `fileId` 反查 `ai_chat_message.attachments` 或 `ai_generation_task.file_id`，再复用 `AgentChatController` 的会话归属口径（同文件 :164-169 的 `ai:assistant` / `ai:session:list` 模式）。
- 加 `@OperateLog(module="AI助手", type=OTHER)` 落审计（R5）。

### 6.2 文本内联读取接口（可选，推荐做）— **本期未做**

```
GET /admin/ai/agent/file/{fileId}/content?maxBytes=2097152
→ Result<{ contentType, truncated, text }>   // 仅 md / txt / html / csv 白名单
```

比让前端跨域拉 Blob 再 `text()` 更省心：统一走鉴权、统一大小截断、统一字符集探测（GBK 中文 CSV 是常见坑，仓库里已有 GBK 相关教训），并且 html 走这条路时**响应体是数据而非可导航文档**，与 5.4 的沙箱策略天然配合。

> 实施修正：前端本就有带 token 的 `requestClient.download<Blob>()`，产物又都在同一文件服务下，
> 阶段一改用「同源化直链 + Blob → `text()` + 前端截断（文本 20 万字符、HTML 100 万字符）」即可闭环，
> 因此未新增 `/content` 接口。**代价**：字符集探测落到浏览器（`Blob.text()` 按 UTF-8 解码，
> GBK 导出的 CSV 会乱码）；若后续确有非 UTF-8 产物，再按本节补接口，把解码放回后端。

### 6.3 静态资源响应头与 Range — **已落地（nosniff 范围收窄）**

在 `FileAutoConfiguration` 的资源映射处追加 `FileSecurityHeaderFilter`（`FilterRegistrationBean`，
url-pattern 由 `zhush.file.local.url-path` 推导，servlet 匹配只支持 `/files/*` 而非 `/files/**`）：
`.html/.htm/.svg` 下发 `CSP: sandbox allow-scripts` + `X-Frame-Options: SAMEORIGIN`，
文本类扩展名（html/htm/svg/txt/md/csv/json/xml/js/css）下发 `X-Content-Type-Options: nosniff`。
图片与视频**故意不给 nosniff**：Spring 对 `.webp/.mov` 等可能回落 `application/octet-stream`，
带 nosniff 会让 `<img>`/`<video>` 直接拒绝渲染。`Accept-Ranges`/206 由 `ResourceHttpRequestHandler`
保留（V1 仍需实测经 nginx 反代后 206 是否透传）。

### 6.4 路线甲：服务端转换门面（按 §0.1 档位择一实现）

**门面形状两档共用**，沿用 kkFileView 文档已设计好的 `PreviewProperties` / `PreviewService` / `PreviewResult{previewable, previewUrl, reason}`，用 `zhush.preview.office-converter` 三值切换实现，`none` 即路线乙：

**甲-1 LibreOffice 内嵌（B 档首选，无第二容器）——〔已实施，详见 §13.5〕**

- 流程：`libreoffice` → `soffice --headless --convert-to pdf` → 缓存到 `/data/upload/preview-cache/{fileId}.pdf`（或转成 `infra_file` 记录）→ `previewMode=server-pdf` → **前端复用同一个 pdf.js 组件**，不新增渲染面。
- 依赖：**不要用 `jodconverter-spring-boot-starter`**（4.4.11 基于 Spring Boot 2.7.18，5.x 面向 Boot 3.x，Boot 4 未验证）；引 `jodconverter-local:5.1.1`（Java 17+）手动声明 `LocalOfficeManager` Bean，或直接 `ProcessBuilder`。
- **必须限流**：固定 1～2 个 office 进程 + 队列上限 + 单文档超时（建议 60s）；`taskqueue.disable=false` 之类的排队语义自己实现。超限即返回 `unsupported` + 稍后重试文案。
- 镜像：在 `zhush-server` Dockerfile 加装 `libreoffice` 与中文字体（缺字体会乱码，与 kkFileView 文档 §4.4 同一问题），体积 +400～700MB。

**实施修正（四条，均已落代码）**

1. **缓存不放存储根内**：默认取存储根的**同级** `preview-cache`（docker 为 `/data/preview-cache`）。存储根经免鉴权的 `/api/files/**` 直出（S2），转换产物放进去等于绕开归属校验；改由新接口 `GET /admin/ai/agent/file/{fileId}/preview-content` 带会话归属断言下发字节。
2. **不引 jodconverter**：`ProcessBuilder` 直起 soffice。引它只为拿进程池，而排队/超时/降级语义本就要自己实现，零新增 Maven 依赖更可控。
3. **限流的具体形状**：槽位数 = `max-concurrent`，每槽一套独立 `-env:UserInstallation` profile（同一 profile 并发启动会互抢锁而失败）；等待人数 > `queue-limit` 立即报忙；等槽位超 `queue-timeout-seconds` 报忙；进程超 `timeout-seconds` 即 `destroyForcibly`。四类失败（UNAVAILABLE / BUSY / TIMEOUT / FAILED）统一回落 `unsupported`，不抛异常。
4. **镜像改动做成 build-arg**：`ARG WITH_LIBREOFFICE=false`，默认不装以保住现网镜像体积；B 档启用时装 `libreoffice-writer/calc/impress + fonts-noto-cjk`。compose 侧新增 `PREVIEW_OFFICE_CONVERTER` / `PREVIEW_CACHE_PATH` / `PREVIEW_MAX_CONCURRENT` 与 `./preview-cache` 卷。

**甲-2 kkFileView 独立服务（A 档且需要格式广度时）——〔已实施，落地形态与偏差见第十五章〕**

- 版本取 **5.0.3**（5.0.0 起需 JDK21，与既有文档里的 4.4.0/JDK8 口径需重新对齐，见 D3）。**实施修正**：kkFileView 对外只有 `/onlinePreview` 渲染页，没有「转出 PDF 字节」的稳定公开接口，因此本路线**只能返回 `server-iframe`**，拿不到 `server-pdf`（详见 15.1 与 S10 的实施修正）。
- 必须配 `zhush.preview.source-base-url`（2.3 的 docker 相对前缀问题，容器无法解析 `/api/files/...`）、`KK_FILE_UPLOAD_DISABLE=true`、`KK_TRUST_HOST`、`KK_SCRIPTJS=false`、nginx `/preview` + `limit_except GET HEAD`，并给容器 `deploy.resources.limits`。**细节以《kkFileView 在线预览整合方案.md》第四、五、七章为准**，那份文档的容器/nginx/安全清单可直接执行，只是不再作为唯一路线。
- **`.html`/`.svg` 不交给它渲染**（S1：它会把内容直接呈现在可嵌入的服务面上）。
- 门面成本：新增一个 `RemotePreviewRenderer` Bean（与甲-1 的 `OfficeFileConverter` 是两个交付物形状，不可互换），`AgentFilePreviewService` 多一个 `server-iframe` 分支，前端多一个 sandbox iframe 渲染器；缓存与 `preview-content` 下发接口这一整套甲-1 机制在甲-2 下**不参与**（产物字节不经后端）。

### 6.5 配置项（新增 `zhush.preview.*`，示意）——〔实际形状与差异见 §13.5〕

```yaml
zhush:
  preview:
    enabled: true
    office-converter: none            # none(路线乙) / libreoffice(甲-1) / kkfileview(甲-2)
    allowed-extensions: [png, jpg, jpeg, gif, webp, mp4, webm, md, markdown,
                         txt, csv, html, htm, pdf, docx, xlsx]   # 路线乙白名单；xls/doc/ppt 由 office-converter 决定是否放行
    inline-text-max-bytes: 2097152
    convert:
      timeout-seconds: 60             # 甲-1 单文档转换超时
      max-concurrent: 1               # 甲-1 office 进程数，防拖垮 zhush-server
      queue-limit: 20                 # 超出即降级 unsupported
      cache-path: /data/upload/preview-cache
    source-base-url: ${PREVIEW_SOURCE_BASE_URL:}   # 仅甲-2 需要（docker 相对前缀问题）
    base-url: ${PREVIEW_BASE_URL:/preview}         # 仅甲-2：nginx 反代前缀
    watermark: ""                     # 阶段三，取决于 D5
    sign-url:
      enabled: false                  # 阶段三打开：HMAC 签名直链 + 短时效（60~300s）
```

> 升档动作 = 改 `office-converter` 一个值（甲-2 另需起容器 + nginx `/preview`）。**前端、接口契约、数据库均不变**。

### 6.6 数据库

**无 DDL 变更**（`attachments` 为 JSON 列，加 `fileId`/`contentType` 字段天然兼容；历史数据缺字段按「按 URL 预览」降级）。若走 kkFileView 路线，仅需按其原文档补 nginx 与 compose，不动表。

---

## 七、前端改造点（文件清单）

| 文件 | 改动 | 说明 |
|---|---|---|
| `src/hooks/use-chat-stream.ts` | `ChatAttachment` 补 `fileId`/`contentType`；新增 `previewKind(att)` 纯函数 | 与后端 `FileArtifact` 对齐（G1） |
| `src/api/ai/types.ts`、`src/api/ai/agent.ts` | 新增 `getFilePreviewMeta(fileId)`、`getFileContent(fileId)` | 走 `requestClient`，带 token |
| `src/views/ai/assistant/modules/attachment-preview.vue`（新增） | 预览容器：**以后端 `previewMode` 为主分派**、本地 `previewKind` 仅兜底 + loading + 失败降级 + 下载兜底 | 子渲染器全部 `defineAsyncComponent`；`server-pdf` 复用 `preview-pdf.vue`，`server-iframe` 复用 `preview-html.vue` 的 iframe 壳 |
| `.../modules/preview-pdf.vue`（新增） | pdf.js 渲染（自建或 vue-pdf-embed），**必须配 cmaps/standardFontData** | 5.5 |
| `.../modules/preview-docx.vue`（新增） | `@vue-office/docx` | 5.6 |
| `.../modules/preview-sheet.vue`（新增） | `@vue-office/excel`；csv 走轻解析 + antdv-next Table | 5.7 |
| `.../modules/preview-markdown.vue`（新增） | 复用 marked/hljs/DOMPurify + 截断 | 5.3 |
| `.../modules/preview-html.vue`（新增） | `sandbox="allow-scripts"` iframe + `srcdoc` | 5.4，**唯一允许 iframe 的入口** |
| `src/views/ai/assistant/modules/message-bubble.vue` | 附件 `v-else` 分支改为「预览 + 下载」；图片接灯箱；正文 md 内嵌图接灯箱 | :294-322 |
| `src/utils/file-preview.ts`（新增） | `previewFile(attOrFileId, opts)`：取 meta → 不可预览则 warning+下载 → 可预览则开弹窗 | 全站统一入口，供文件管理/公告附件复用 |
| `src/adapter/component/index.ts` | 把 `previewImage()` 的图片分支抽成公共 lightbox 工具 | :253-329 |
| `src/views/ai/assistant/index.vue` | 上传后的待发送附件列表加「预览」；**发送时把附件对象随消息落库**（G4），不再只拼文本行 | :180-210、:692-697 |
| `pnpm-workspace.yaml` + `apps/web-antdv-next/package.json` | catalog 增精确版本；应用引用 `catalog:` | 见下表 |
| `nginx` / CSP | 若采用 5.4 的 Blob URL 方案，需放行 `blob:`；响应头方案落地时在 nginx 或后端补 header | 两个 `default.conf` 当前 0 个 `add_header` |

新增依赖（**均懒加载，不进首屏**；路线乙全量安装，路线甲只是让部分依赖在旧格式路径上不被触发，不需要卸载）：

| 包 | 精确版本 | 许可证 | 用途 |
|---|---|---|---|
| `pdfjs-dist` | 6.4.299 | Apache-2.0 | PDF（首选自建，含 cmap/字体数据） |
| `vue-pdf-embed` | 2.1.7 | MIT | 备选：worker 自动处理，但 cmap 透传待验证 |
| `@vue-office/docx` | 1.6.3 | MIT | docx |
| `@vue-office/excel` | 1.7.14 | MIT | xlsx/csv |
| `vue-demi` | 0.14.6 | MIT | vue-office peer，**postinstall 需 allowBuilds 放行** |
| `@e965/xlsx` | 0.20.3 | Apache-2.0 | 需要自解析时的 SheetJS 镜像 |
| （次级）`@vue-office/pptx` | 1.0.1 | MIT | 仅当 D4 确认收录 pptx |
| （兜底）`mammoth` | 1.13.0 | BSD-2 | docx 语义 HTML 兜底，非版式 |

图片（antdv-next 内置 `Image.PreviewGroup`）、视频（原生）、Markdown（现有 marked/hljs/DOMPurify）**零新增**。

---

## 八、安全设计

| 编号 | 风险 | 现状证据 | 缓解（路线 × 期次） |
|---|---|---|---|
| **S1** | **同源 HTML/SVG 存储型 XSS**：上传或产物 `.html`/`.svg` 经 `/api/files/**` 与后台同域返回，`text/html` 直开即执行 JS，可拿管理端 token | nginx `/`、`/api`、`/files` 同源（deploy/nginx/default.conf）；无任何 `add_header`；前端无 CSP | **阶段一必做（已落地）**：预览一律 `sandbox="allow-scripts"`（禁 `allow-same-origin`）+ `srcdoc`；SVG 不做内联预览（`preview-meta` 直接判 unsupported）；响应头由 [FileSecurityHeaderFilter](file:///D:/zhush/project/back-boot/zhush-boot4-parent/zhush-framework/zhush-spring-boot-starter-file/src/main/java/com/zhush/file/filter/FileSecurityHeaderFilter.java) 在 `/files/**` 映射上落地——`.html/.htm/.svg` 加 `CSP: sandbox allow-scripts` + `X-Frame-Options: SAMEORIGIN`，`nosniff` 只给文本类扩展名（图片/视频不给，避免 Spring 无 MIME 映射时 `nosniff` 直接拦掉播放） |
| **S2** | **免鉴权公开直链**：`/api/files/**` 全环境在白名单，路径含 UUID 不可枚举但可转发 | application-dev.yaml:130 等四环境 | 阶段一：预览入口收敛到鉴权接口 + `@OperateLog` 审计；**阶段三**：HMAC 签名直链或 Redis 一次性 token（60~300s），图片视频可按需豁免 |
| **S3** | **扩展名白名单形同虚设**（表述修正）：校验代码在 `FileClient.validateSizeAndExtension`（:165）里是**生效**的，缺的是配置——四个环境 yaml 该项均注释，等价于不限制 | FileStorageProperties.java:34、FileClient.java:165 | 阶段一（已落地）：预览侧独立口径 `zhush.preview.allowed-extensions`（不配则用内置默认表，与上传白名单互不影响）；上传侧真正启用白名单前仍须先按 D6 统计 `infra_file.ext` 分布，避免反噬历史功能 |
| **S4** | CORS `*` + `allowCredentials:true` 长期开放 | dev:207-215 / docker:223-231 | 生产按域名收敛（`CORS_ALLOWED_ORIGINS` 已支持 env 注入） |
| **S5** | 越权预览他人会话产物 | `checkOwnership` 只约束 member（FileService.java:182-189）；`ai:assistant` 是粗粒度权限点 | **阶段一必做**：6.1 的会话归属断言；顺带关闭《对话框多能力补全…》第四章列出的「接口权限点校验 + 会话归属断言」两项治理 |
| **S6** | SSRF：预览接口按外部 URL 预览 | 现有 `RemoteFileFetcher` 已限制允许域名 | 预览接口**只接受 `fileId`**，不接受任意 URL；如需 URL 形态，复用 `fetchFromAllowedHosts` |
| **S7** | DoS / 资源耗尽：超大 xlsx 单元格爆炸、10MB+ HTML `srcdoc`、并发转换拖垮后端 | `max-size=50MB` | 文本类 `maxBytes` 截断（6.2）；表格行列上限；pdf.js 只渲染可视页 + 分页懒渲；**路线甲必做**：转换进程数上限 + 队列上限 + 单文档超时（6.5 `convert.*`），甲-2 另需容器 `deploy.resources.limits` |
| **S8** | 内容外泄（截图/转发） | — | 阶段三：预览水印（操作人 + 时间），前端 CSS 遮罩仅作心理提示 |
| **S9** | **甲-2 专属**：`/preview` 服务面被当作上传/删除入口或被诱导拉取内网（SSRF） | kkFileView 默认开放 `/fileUpload`、`/deleteFile`，且回源由它发起 | `KK_FILE_UPLOAD_DISABLE=true`、不设 `KK_DELETE_PASSWORD`、`KK_TRUST_HOST` 白名单、`KK_SCRIPTJS=false`、nginx `limit_except GET HEAD`；8012 不对公网开放 |
| **S10** | **甲-2 专属**：kkFileView 把文档渲染成可嵌入页面，等于多一个同源可执行内容出口 | 其 HTML 预览模式直接在 `/preview` 下出 HTML | 默认 `KK_OFFICE_PREVIEW_TYPE: pdf` → 只取它当转换器（`server-pdf`），**不取 `server-iframe`**；`.html/.svg` 永不交给它。**实施修正（2026-10-22）**：kkFileView 没有返回 PDF 字节的稳定公开接口（转换产物落在它自己的缓存目录与内部静态映射里，随小版本变），「只取转换器」在它身上做不到 → 本路线实际就是 `server-iframe`。补偿控制：容器侧仍配 `office.preview.type=pdf`（它自己把 Office 转 PDF 再渲染，不出 HTML 直排）、前端 iframe 用 `sandbox="allow-scripts"` 且**绝不给 `allow-same-origin`**（见 15.2）、`/preview` 经 nginx 只放行 GET/HEAD、`.html/.svg/图片/视频/md/pdf` 在渲染器的 `supports()` 里就排除掉（不是靠运维自觉）。**要严格拿到 S10 原意，选甲-1。** |

**备案/合规提醒**：预览不新增 UGC 对外入口（管理端内部功能），与当前个人备案口径不冲突；但若把 `/api/files/**` 挪到独立子域以解耦 S1，需同步确认备案覆盖范围后再改。

---

## 九、待实测项（开工前必须验掉，别写进代码假设）

| 编号 | 验证内容 | 方法 | 影响 |
|---|---|---|---|
| **V1** | 经 nginx 反代后 `/api/files/**` 是否保留 `Range`→206 | `curl -H "Range: bytes=0-1023" -I`，分别打 9199 直连与 8086 反代 | 决定视频拖进度条、pdf.js 渐进加载是否可用；不通过则需 `ResourceRegion` 或 `X-Accel-Redirect` |
| **V2** | `vue-pdf-embed@2.1.7` 能否透传 `cMapUrl`/`standardFontDataUrl`，用**真实 AI 生成 PDF**（OpenPDF 非嵌入 CJK）出字 | 起一个最小 demo 页拿现有产物 pdf 试渲染 | 决定 5.5 走封装还是自建；失败即自建 |
| **V3** | `@vue-office/docx` 渲染 `AgentDocumentService` 产物（POI XWPF，含代码块/表格）的版式还原度 | 用会话「生成 Word」真实产物比对 Word 原样打开 | 不达标 → 提前启用路线甲（docx 也转 PDF），或维持前端渲染并在文案上注明「预览仅供参考」 |
| **V4** | `@vue-office/excel@1.7.14` 对 xlsx 合并单元格/公式/多 sheet 的表现；`.xls` 是否确实拒绝 | 造 3 个测试文件 | 决定 sheet 是否进路线乙；不达标则该格式也归入路线甲 |
| **V5** | pnpm 11 下 `vue-demi` postinstall 被拦的实际报错与 `allowBuilds` 写法 | 安装一次 | 决定 5.6 工程细节 |
| **V6** | `pdfjs-dist@6.4.299` 的 `engines(node>=22.13||>=24)` 与本机(24.18.0)/CI/构建 Dockerfile 镜像是否一致 | 查 CI 与 `Dockerfile` 基础镜像 node 版本 | 不匹配则 `pnpm install`/构建失败 |
| **V7** | **部署机实际余量 → 定档（A/B/C）** | §0.1 的四条命令（`free -m` 看 available、`nproc`、`df -h`、`docker stats --no-stream`），在**业务高峰时段**各采一次 | 决定走路线乙还是甲；也决定甲-1 vs 甲-2。**这是唯一影响整体路线的实测项，建议开工第一天就做** |

> V1/V2/V3 任一条不通，对应格式的渲染路线就要改，因此**先做验证再排期**；这三条各约 0.5 人日。V7 是纯运维动作，零成本，但**必须在立项时完成**——它是资源门禁的输入，不能等代码写完再回头评估。

---

## 十、分期实施计划与验收标准

### 阶段一｜路线乙基线（C/B/A 三档都要做，是唯一不可跳过的一段；建议 4～6 人日）**〔已实施，见第十三章〕**

范围：G1/G2/G3/G5(部分)/S1/S3/S5 + 图片灯箱、视频、md、html、pdf、docx、xlsx/csv 预览；`doc/xls/ppt` 在 `office-converter=none` 下 unsupported。
**注意后端门面从第一天就带 `office-converter` 开关与完整 `previewMode` 枚举**（即使取值恒为 `none`）——契约先稳定，后续升档才不会改前端。

1. 先跑 §0.1 的 V7 定档 + V1～V6 验证（0.5～1 人日；V7 只是敲四条命令）。
2. 后端：`preview-meta`（+可选 `content`）接口、`previewMode` 裁决与 `office-converter` 开关、会话归属断言、`@OperateLog`、扩展名白名单真正生效、`/api/files` 响应头加固。
3. 前端：依赖入 catalog（精确版本）、`utils/file-preview.ts`、`AttachmentPreview.vue` + 7 个懒加载渲染器、`message-bubble.vue` 附件卡片改造、`previewImage` 抽公共、上传附件随消息落 `attachments`（G4）。
4. 交付验证走仓库既有口径：`pnpm typecheck` + `oxlint`，并在真实会话里逐个格式点一遍（**不能只靠类型检查**）。

验收标准：

- [ ] 会话内 png/jpg/mp4/pdf/docx/md/html/xlsx/csv 均可点击预览，历史消息回显后同样可预览（R4）。
- [ ] AI 生成的中文 PDF 在 pdf.js 下**中文正常出字**（V2 通过的客观判据）。
- [ ] `office-converter=none` 时 `doc`/`xls`/`ppt`/`svg` 返回 `previewable=false` 且前端提示「请下载查看」，无空白框、无控制台报错。
- [ ] 预览一个含 `<script>` 的 `.html` 产物：脚本在沙箱内运行但**读不到 `parent`、拿不到 localStorage/token**（S1 回归用例）。
- [ ] 未登录访问 `preview-meta` → 401；无 `ai:assistant` → 403；预览他人会话产物 → 拒绝（S5）。
- [ ] 上传 `.exe`/非白名单扩展被拒（S3）。
- [ ] 预览动作写入操作日志，可查「谁在何时预览了哪个 fileId」（R5）。
- [ ] AI 助手页首屏 chunk 无 PDF/Office 相关体积（懒加载生效）。
- [ ] 视频拖动进度条走 206 分段请求（V1）。

### 阶段二｜路线甲（仅当 V7 定档为 B 或 A；建议 3～5 人日，含压测）——〔甲-1 已实施，见 §13.5；V7 定档仍是启用前提〕

| 档位 | 做什么 | 关键验收 |
|---|---|---|
| **B（空闲 1.5～3GB）** | 甲-1：LibreOffice 内嵌（6.4），`doc/xls/ppt` → `server-pdf` | 转换并发压测下 `zhush-server` 不 OOM、对话链路不受影响；超限即回落 `unsupported` |
| **A（空闲 ≥4GB、≥4 vCPU）** | 甲-2：kkFileView 5.0.3 独立服务，或先用甲-1、仅在确有 ofd/CAD/压缩包需求时再并存 | 除 B 档各项外，另验 `/fileUpload`、`/deleteFile` 不可访问、非白名单来源被拒（S9）、Office 走 PDF 模式而非 HTML 直渲（S10） |

两条档位共用的验收项（**这一条是本方案双路线设计的核心检验**）：

- [ ] **热切换**：只改 `zhush.preview.office-converter`（`none`→`libreoffice`）重启后端，**前端不发版**，`doc` 即从「仅下载」变为可预览；反向切换同样干净。
- [ ] **降级不阻断**：转换服务停机/超时，预览返回 `unsupported` 并给出下载兜底，**对话与产物下载完全不受影响**（§0.1 原则 2）。
- [ ] 转换产物有缓存且可按 `fileId` 失效，重复预览不重复转换。
- [ ] `doc` 转换后中文不乱码（宿主机 `/usr/share/fonts` 已含中文字体，与 kkFileView 文档 §4.4 同一前提）。

### 阶段三｜治理与扩展（不依赖档位）

- 签名直链 / 一次性 token（S2）+ 动态水印（S8）。
- 预览门面复用到文件管理、通知公告附件入口（与《kkFileView在线预览整合方案.md》的场景合并，一份实现两处用）。
- 会话右侧常驻预览分栏；移动端 `zhush-admin-uni`（依赖路线甲 PDF 化 + `uni.downloadFile`/`openDocument` 适配）；预览埋点与容量监控（含转换耗时、失败率、档位余量告警）。

---

## 十一、待确认项

| 编号 | 待确认 | 建议 |
|---|---|---|
| **D0** | **V7 实测结果落在哪一档**（这条不定，路线甲是否开工就是悬案） | 先跑 `free -m` / `nproc` / `docker stats`，按 §0.1 表定档。**建议无论哪档都先交付阶段一**，路线甲作为增量 |
| D1 | 阶段一是否接受「doc/xls/ppt 只能下载」 | **建议接受**，这是零新增容器换来的边界；V7 定为 B/A 后由阶段二补齐 |
| D2 | PDF 中文：预览端配 cmaps，还是**生成端改字体嵌入** | 建议两处都做（生成端嵌入更彻底，但需确认字体授权与体积容忍度）。**注意：走甲-1 时 LO 转出的 PDF 自带字体，反而绕开这个坑**——这是 B 档的一条隐含收益 |
| D3 | kkFileView（甲-2）还是 LibreOffice（甲-1） | **默认甲-1**。仅当业务明确需要 ofd/CAD/压缩包/邮件这类广度格式时才上甲-2；两者共用同一个裁决门面（`AgentFilePreviewService` + `previewMode` 契约），但**交付物是两个 SPI**（`OfficeFileConverter` 出字节 / `RemotePreviewRenderer` 出地址），先甲-1 后补甲-2 仍不构成返工，因为前端与接口一行不改 |
| D4 | pptx 是否进阶段一 | 建议否（无产物来源、保真度最差、`@vue-office/pptx` 仅 1.0.1）；**走路线甲后 pptx 自然由转 PDF 覆盖**，无需单独决策 |
| D5 | 是否需要预览水印（合规诉求？） | 若文件涉及对外资料，阶段三做动态水印（操作人+时间） |
| D6 | `FileStorageProperties.allowedExtensions` 生效是否会打断现有业务上传 | 上线前统计 `infra_file.ext` 分布再定白名单，避免 S3 修复反噬历史功能 |
| D7 | HTML 产物未来会否由 AI 生成（目前只 docx/pdf） | 若有「生成 HTML 报告/落地页」规划，5.4 沙箱 + S1 响应头**必须阶段一同步落地**，不能等阶段二/三 |
| D8 | 生产 Node/CI 版本（V6） | 若 CI 仍 <22.13，先统一版本，或改用不依赖 pdfjs-dist 的 `@vue-office/pdf@2.0.10`（内联 worker，体积更大） |
| D9 | 服务器是否有扩容/加内存计划 | 若短期会加内存，阶段一照做即可，路线甲留到扩容后按门禁启用；**不必为预览功能单独申请扩容**（旧格式只下载的代价有限） |

---

## 十二、与既有文档的关系

- **《kkFileView在线预览整合方案.md》（同目录，未落地）**：接口形状（`{previewable, previewUrl, reason}`）、安全清单（`KK_FILE_UPLOAD_DISABLE`、`KK_TRUST_HOST`、nginx `limit_except`）、docker `FILE_DOMAIN` 相对前缀的「决策一」、中文字体前提全部沿用，**可直接作为路线甲-2 的施工蓝本**；差异仅在于本文不再把它当唯一路线，而是 A 档的可选实现（B 档优先甲-1）。两份文档的预览门面必须合并为同一个 `PreviewService`，避免两套口径。
- **《对话框多能力补全与图像视频生成实现方案.md》**：其 §四.4「开工 P3/P4 前先补的四项治理」中，**接口权限点校验**与**会话归属断言**在本方案阶段一 S5 一并关闭；**配额与限流**仍归该文档管辖。
- **《Markdown文件导入解析方案.md》/《文档解析使用指南.md》**：解析（Tika 抽文本喂模型）与预览（原样呈现排版）是互补两条链路，本方案不改动解析链路；`parse-doc` 返回的富文本 HTML 也不是预览实现，勿复用。

---

## 十三、阶段一实施状态（2026-10-22）

阶段一（路线乙基线）已按本文落地，**双路线契约先稳定**：`previewMode` 全枚举 + `zhush.preview.office-converter` 开关已进代码与四个环境 yaml，取值恒为 `none`。

### 13.1 后端（`mvn -pl zhush-server -am compile` 通过）

| 文件 | 作用 |
|---|---|
| `zhush-module-ai/.../preview/PreviewMode.java` | 10 个渲染方式的枚举，`value` 为下发前端的 kebab 串（VO 里存 `String`，不押注 Jackson 3 的 `@JsonValue` 行为） |
| `.../preview/PreviewProperties.java` | `zhush.preview.{enabled, office-converter, allowed-extensions}`；`office-converter` 非 `none` 时**只告警并按 `none` 运行**（本期未实现甲路线，避免承诺没有的 `server-pdf`） |
| `.../preview/AgentFilePreviewService.java` | `fileId` → 产物定位 + 模式裁决；归属沿用 `AgentChatOwnershipGuard`（fail-closed），消息附件反查不到时回退 `ai_generation_task` |
| `.../vo/FilePreviewMetaVO.java` | `{id, name, ext, size, contentType, previewable, previewMode, reason, accessUrl}` |
| `.../controller/admin/AgentFilePreviewController.java` | `GET /admin/ai/agent/file/{fileId}/preview-meta`，权限 `ai:assistant` OR `ai:session:list`，`@OperateLog(QUERY, recordResult=false)` |
| `.../mapper/AiChatMessageMapper.java` | `selectByAttachmentFileId`（LIKE 粗筛 + Java 侧精确比对；**`fileId` 按字符串比**，全局 Long→String 序列化使 attachments 里存的是 `"fileId":"123"`） |
| `zhush-starter-file/.../filter/FileSecurityHeaderFilter.java` | 6.3 的响应头加固，随 `zhush.file.local.enabled` 装配 |

### 13.2 前端（typecheck / oxlint / `pnpm -F @vben/web-antdv-next build` 全绿）

`apps/web-antdv-next/src/views/ai/assistant/modules/` 下新增 `attachment-preview.vue`（页面级唯一弹窗实例，按后端 `previewMode` 分派）与 `preview/` 五个懒加载渲染器（text / html / pdf / docx / sheet）+ `preview-utils.ts`；`message-bubble.vue` 附件卡片改为「预览 + 下载」，图片点击进弹窗；`ChatAttachment`/`FileArtifactVO` 补 `fileId`；`api/ai/agent.ts` 加 `getFilePreviewMeta`。依赖 `pdfjs-dist@6.4.299`、`@vue-office/{docx,excel}`、`vue-demi` 入 catalog 并写进 `allowBuilds`（V5 命中：两个 vue-office 包的 postinstall 要在 v2/v3 产物间切换）。

产物体积证实懒加载生效：`preview-pdf` 480KB、`preview-docx` 172KB、`preview-sheet` 1.65MB 各自独立 chunk，首屏不含。

### 13.3 与本文的偏差（均为有意收敛，不是遗漏）

| 偏差 | 原因与影响 |
|---|---|
| 未做 6.2 的 `/content` 接口 | 前端已有带 token 的 `requestClient.download<Blob>`，同源化直链后 `blob.text()` 即可；**代价**：字符集解码交给浏览器，GBK 导出的 CSV 会乱码，届时再补后端接口 |
| 无 `utils/file-preview.ts` 全局工具 | 同源化 / 取字节 / 扩展名推断只在预览链路内用，落在 `modules/preview/preview-utils.ts`，避免过早抽象 |
| G4（用户上传附件落 `attachments`）未做 | 不属于「AI 生成文件可预览」的范围，且要动 user 消息落库结构；用户上传附件本期仍按文本行注入 |
| 未做「`previewImage` 抽公共」 | 图片改为进同一预览弹窗，不再需要独立灯箱 |
| 上传白名单（S3）仍未启用 | 校验代码本就生效，缺的是配置；按 D6 先统计 `infra_file.ext` 分布再决定，本期只落预览侧独立口径 |
| pptx 未接（D4 结论一致） | 无产物来源；`@vue-office/pptx` 仅 1.0.1；走路线甲后由转 PDF 覆盖 |

### 13.4 尚待实机验证（本文第九章清单）

V1（206 透传）、V3（docx 版式还原度）、V4（xlsx 合并单元格/公式/`.xls` 拒绝）、V7（部署机余量定档）**必须在跑起来的环境里核对**；本次交付环境无后端（MySQL/Redis/9166 未启动），只完成静态验证：编译、`vue-tsc`、`oxlint`、生产构建。第九/十章验收表里凡是「点一遍真实产物」的判据都算未勾选，尤其是这几条：

- [ ] AI 生成的中文 PDF 出字（cmap 交付路径已按 5.5 落地，但需真实产物确认）。
- [ ] 含 `<script>` 的 html 产物：脚本执行但读不到 `parent` / localStorage（S1 回归）。
- [ ] 预览他人会话产物被拒（S5，`preview-meta` 归属断言）。
- [ ] 未登录 401 / 无 `ai:assistant` 403 / 预览动作进操作日志（R5）。
- [ ] 历史消息回显后仍可预览（依赖 `attachments` 里的 `fileId`，早于 fileId 落库的历史数据走前端按扩展名的兜底）。
- **AI 模块现状缺口（见知识库「zhush-module-ai 接口归属隔离、权限校验与配额限制的现状缺口」）**：本方案的 6.1 归属断言是该缺口的第一个落地修复点。

---

## 十四、阶段二实施状态（2026-10-22，甲-1 路线）

**前置并未闭环**：V7 资源实测（`free -m` / `nproc` / `df -h` / `docker stats --no-stream`）决定的是**启不启用**，不是做不做。本次交付环境没有部署机，所以代码只做「实现完整 + 默认关闭」——四环境 `office-converter` 仍为 `none`；即便误设为 `libreoffice` 而机器没装 LibreOffice，启动探测失败后也自动按路线乙运行。

### 14.1 后端（`mvn -o -pl zhush-module-ai -am compile` 通过）

| 文件 | 作用 |
|---|---|
| `preview/OfficeFileConverter.java` | 转换门面 SPI：`name / available / supports / convertToPdf`。**「甲-2 只需再注册一个 Bean、其余一行不改」这句当时的判断不成立**：甲-2 交不出 PDF 字节，只能另立 `RemotePreviewRenderer`（见 §15.1） |
| `preview/PreviewConversionException.java` | 四类失败 Kind（UNAVAILABLE / BUSY / TIMEOUT / FAILED），供门面翻译成文案 |
| `preview/LibreOfficeFileConverter.java` | 甲-1 实现：`ProcessBuilder` 起 soffice + 槽位化 `-env:UserInstallation` profile + 排队上限 + 超时 `destroyForcibly` + 启动探测；`@ConditionalOnProperty(office-converter=libreoffice)`，非该值时不创建 |
| `preview/PreviewCacheStore.java` | `{cache}/{fileId}.pdf`：临时文件 + `ATOMIC_MOVE` 落盘，避免并发读到半截 PDF；TTL 过期按未命中处理；检测到缓存落在存储根之内会告警 |
| `preview/PreviewProperties.java` | 新增 `prefer-pdf-extensions` 与 `convert.*` 七个旋钮 |
| `preview/AgentFilePreviewService.java` | 裁决 `doc/xls/ppt/pptx/odt/ods/odp/rtf` → `server-pdf`；`docx/xlsx` **默认仍走前端**，显式列进 `prefer-pdf-extensions` 才改转 PDF；转换在 `preview-meta` 内同步完成，任一失败即 `unsupported`；新增 `convertedPdf()` 复用同一套归属断言 |
| `controller/admin/AgentFilePreviewController.java` | `GET /file/{fileId}/preview-content`：`FileSystemResource` + `application/pdf` + `nosniff` + `Cache-Control: no-store`（带归属校验的字节不做 HTTP 缓存，否则命中缓存就跳过归属断言），每次重跑会话归属校验 |
| `enums/AiErrorCode.java` | `FILE_CONVERT_FAILED(1003405)` |

### 14.2 前端（vue-tsc / oxlint / build 全绿）

`PreviewMode` 补 `server-pdf`；`attachment-preview.vue` 把它并入 pdf.js 分支，并**拆开渲染地址与下载地址**——`server-pdf` 下发的是转好的 PDF，下载必须仍取原始 `.doc` 及原文件名。`message-bubble.vue` 的预览入口**不再按扩展名预判**：否则升档后新可预览的格式要发版才显示按钮，直接违反本章「热切换」验收项。`api/ai/agent.ts` 给 `getFilePreviewMeta` 单独放宽超时到 95s（= 排队 30s + 转换 60s），默认 10s 会把首次转换的请求掐断成「不支持预览」的假象。

### 14.3 配置与部署

四环境 yaml 只写需要覆盖的键，其余由 `PreviewProperties.Convert` 兜默认。与 §6.5 示意的差异：删掉 `inline-text-max-bytes`（前端已按字符数截断，见 §13.3）；`cache-path` 默认从「存储根内」改为「存储根同级」；新增 `queue-timeout-seconds` / `cache-ttl-hours` / `max-source-bytes` / `prefer-pdf-extensions`；`source-base-url` / `base-url` / `watermark` / `sign-url` 未引入（分属甲-2 与阶段三；甲-2 落地时前两个已补上并另加 `probe-url`，见 §15.3）。Dockerfile 加 `ARG WITH_LIBREOFFICE=false`，compose 加 `PREVIEW_OFFICE_CONVERTER` / `PREVIEW_CACHE_PATH` / `PREVIEW_MAX_CONCURRENT` 与 `./preview-cache` 卷。

### 14.4 阶段二待实机验证（本地无法勾选）

- [ ] **热切换**：`office-converter: none → libreoffice` 重启后端，`.doc` 从「仅下载」变可预览；反向切换同样干净。
- [ ] **并发不 OOM**：多份大 doc 同时预览时 `zhush-server` 与主机内存稳定、对话链路不受影响；`max-concurrent=1` + `queue-limit=20` 下第 22 个请求应拿到「下载兜底」而非 500。
- [ ] **降级不阻断**：停掉 LibreOffice（或设错 `soffice-path`），预览返回 `unsupported`，对话与产物下载完全正常。
- [ ] **中文不乱码**：需 CJK 字体（compose 已挂宿主 `/usr/share/fonts`，B 档镜像内另装 `fonts-noto-cjk`）。
- [ ] **缓存命中**：第二次预览不再出现「转出 PDF」日志；`cache-ttl-hours` 过期后重转。
- [ ] **越权**：直接改 `preview-content` 的 fileId 为他人产物 → 业务错误拒绝。
- [ ] **磁盘**：`preview-cache` 只增不减（超期仅重转、不删旧条目），磁盘紧张时需运维清理或调小 TTL。

---

## 第十五章 阶段二·路线甲-2（kkFileView 独立服务）实施记录（2026-10-22）

代码与部署面已落地，默认仍是 `office-converter=none`；**本机没有 kkFileView 容器，本章所有行为都未实机跑过**（15.5 是全量待勾项）。

### 15.1 落地形态：交付物形状不同，所以是两个 SPI

| 类 | 职责 |
|---|---|
| `preview/RemotePreviewRenderer.java`（新增） | 外部渲染门面 SPI：`name / available / supports / serverOnly / renderUrl`。与甲-1 的 `OfficeFileConverter` 不可互换 |
| `preview/KkFileViewPreviewRenderer.java`（新增） | kkFileView 适配，`@ConditionalOnProperty(office-converter=kkfileview)`，与甲-1 的条件装配天然互斥 |
| `preview/AgentFilePreviewService.java` | 多一条 `serverIframe(ext)` 裁决与 `SERVER_IFRAME` 分支；甲-1 的缓存与 `preview-content` 下发在甲-2 下**完全不参与**（产物字节不经后端） |
| `generation/RemoteFileFetcher.java` | 新增公开判断方法 `isFromStorage(url)`：只判断、不取字节，让外部渲染路线复用同一份存储域名白名单 |

为什么不塞进 `OfficeFileConverter`：那个接口的契约是「返回 PDF 字节」，kkFileView 给不出。硬塞的话门面会把渲染页 HTML 写进 `preview-cache/{fileId}.pdf` 再交给 pdf.js 渲染——坏得悄无声息。

裁决边界：图片 / 视频 / md / txt / html / pdf / csv 在 `build()` 的 switch 里被无条件留在前端（甲-2 改不动它们，§3.3），只有 `doc/xls/ppt/pptx/wps/ofd/压缩包/邮件/CAD/xmind/bpmn/epub` 这类**前端渲染面覆盖不了**的格式默认改走外部服务；`docx/xlsx` 仍要显式列进 `prefer-pdf-extensions` 才让外部服务接手。广度格式即使不在 `allowed-extensions` 里也可预览——这正是甲-2 相对甲-1 的唯一增量。

### 15.2 安全实现（S9 / S10 里可执行的部分）

| 控制 | 落点 |
|---|---|
| 可携带脚本的内容永不外派 | 渲染器 `RENDERABLE` 里**没有** `html/htm/svg/png/jpg/mp4/pdf/md/txt`，靠代码保证而非运维自觉（S1/S10） |
| 只把自己的存储地址交给外部服务 | 绝对地址需命中 `RemoteFileFetcher.isFromStorage()` 或运维配的 `source-base-url` 前缀，否则拒绝生成预览地址（S9 的 SSRF 面收在「我们不交任意 URL」） |
| 不把签名参数抄进日志 | 拒绝时只打主机名，不打完整产物地址 |
| iframe 严格沙箱 | `sandbox="allow-scripts"`，**不给** `allow-same-origin`；**没有**留「放宽沙箱」的配置键——放宽等于把后台 token/cookie 交给第三方渲染页 |
| 写入口与端口 | nginx `/preview` `limit_except GET HEAD`；`KK_FILE_UPLOAD_DISABLE`、`KK_SCRIPTJS=false`、不设删除密码；compose 注释里明确 8012 需安全组关闭公网入方向（S9） |
| 不可达不白屏 | 渲染器 `available()` 为假 → 返回 null → `unsupported` + 下载文案，而不是给前端一个必然报错的 iframe（门禁原则 2）；探测失败按 60s 节流重试，容器比后端晚起不必重启 `zhush-server` |
| **遗留** | `/preview` 免鉴权，预览地址一经下发即「知道即可用」的 bearer URL。这与 S2 同源，阶段三的签名直链 / 一次性 token 一并收口（kkFileView 蓝本的「决策三」本来就接受这一条） |

### 15.3 配置与部署增量

| 文件 | 改动 |
|---|---|
| `PreviewProperties.java` | 新增扁平三键 `base-url`（默认 `/preview`）/ `source-base-url` / `probe-url`，形状与《kkFileView 整合方案》§4.1 一致；`getBaseUrl()` 手写兜空值 |
| `deploy/kkfileview/docker-compose.yaml` | **新增**：5.0.3 + host 网络 + `KK_*` 安全/行为键 + `deploy.resources.limits`（2C/2G）+ 字体与缓存目录挂载 + 三条实机核对注释 |
| `deploy/nginx/default.conf` | 新增 `location /preview`，`limit_except GET HEAD`，`proxy_read_timeout 300s` |
| `deploy/backend/docker-compose.yaml` | 注释形式给出 `PREVIEW_BASE_URL` / `PREVIEW_SOURCE_BASE_URL` / `PREVIEW_PROBE_URL`，避免未起容器时改变现网行为 |
| `application-docker.yaml` | 三个键接 env 占位（默认值不改变现网） |
| `application-{dev,test,prod}.yaml` | 注释形式给出三项 + 修正「kkfileview 未实现」的旧注释 |
| `apps/web-antdv-next/vite.config.ts` | 新增 `/preview → localhost:8012` 代理（dev 跑甲-2 才用） |

### 15.4 与两份蓝本的差异（实机前须知道）

1. 蓝本把门面放在 `zhush-spring-boot-starter-file` 的 `com.zhush.file.preview`；实际落在 `com.zhush.ai.preview`。原因：module-ai 不依赖 module-system，产物归属校验只能用 AI 自有表（`attachments` / `ai_generation_task`）。两份文档的门面合并仍未做，属阶段三「门面复用到文件管理 / 公告」。
2. 蓝本的 `PreviewResult{previewable, previewUrl, reason}` 已被 `FilePreviewMetaVO{previewable, previewMode, accessUrl, reason}` 覆盖，字段名不同、语义一一对应；`watermark` 与 `open-new-window` 未引入（阶段三，取决于 D5）。
3. 蓝本用 `infra_file.path` 重建绝对地址；本实现手里只有产物 JSON 的 `url`，故按 `/files/` 标记切出相对段再拼 `source-base-url`——效果等价，且对 `FILE_DOMAIN` 写成 `/api/files/` 还是 `/files/` 不敏感。
4. **S10 的「只取它当转换器、不取渲染页」做不到**，本路线实际是 `server-iframe`（理由见 S10 实施修正）。要严格守 S10，用甲-1。
5. 蓝本示例镜像是 4.4.0/JDK8，本实现按 D3 与 §3.2 取 **5.0.3**（JDK21）。

### 15.5 甲-2 待实机验证清单

- [ ] **V8 严格沙箱下能否渲染（本章最大的未知）**：`sandbox="allow-scripts"` 不给 `allow-same-origin` 时，文档处于 opaque origin，kkFileView 自己那页 pdf.js 取 PDF 的 XHR 会带 `Origin: null`，它若不回 CORS 头就会被拦 → iframe 空白。两种处置：① 容器侧改 `KK_OFFICE_PREVIEW_TYPE=image`（逐页出 `<img>`，图片加载不吃 CORS）；② 把 `/preview` 挪到**独立源**（另开 server 块或子域），此时 `allow-same-origin` 才谈得上安全。**不许用「去掉 sandbox」解决。**
- [ ] **回源可达**：docker 下 `PREVIEW_SOURCE_BASE_URL` 必须是 kkFileView 容器访问得到的地址，且该主机在 `KK_TRUST_HOST` 内；否则它报「文件不存在」而非转换失败，排查成本很高。
- [ ] **热切换**：`libreoffice → kkfileview` 重启后端，`.doc` 的 `previewMode` 由 `server-pdf` 变 `server-iframe`，前端不发版；`kkfileview → none` 后 `.ofd/.zip` 回到「仅下载」。
- [ ] **降级不阻断**：停掉 kkFileView（且配了 `probe-url`）→ 预览 `unsupported`，对话与下载正常；容器恢复后 60s 内自动恢复可预览。
- [ ] **不被当写入口**：`POST /preview/fileUpload`、`/preview/deleteFile` 经 nginx 应 403/405；公网直连 8012 应不通。
- [ ] **越权**：改 fileId 为他人产物 → `preview-meta` 按会话归属拒绝，iframe 地址根本不会下发。地址一经下发即 bearer 可用（见 15.2 遗留）。
- [ ] **环境变量键名**：核对所用镜像 `config/application.properties` 的 `${KK_*}` 占位与 `file.dir` 挂载路径；未识别的变量被 Spring 静默忽略——不报错，但配置没生效。
- [ ] **资源余量**：V7 定档到 A 才可上；`docker stats` 确认空载 RSS（≈1.5GB）与并发转换峰值不挤垮 `zhush-server`。

---

> 下一步：① 执行 V7 定档（四条命令）——**这是启用阶段二唯一的判断题**，两条甲路线的代码都已就绪，默认 `office-converter=none`；② 定档 B 走甲-1（按 §14.3 改配置 + `WITH_LIBREOFFICE=true` 重新构建镜像，勾 §14.4），定档 A 且确有 ofd/CAD/压缩包需求才走甲-2（起 `deploy/kkfileview` + 核对 §15.5，注意 S10 的原始口径在这条路线上做不到）；③ 阶段三（签名直链 S2、水印、门面复用到文件管理/公告）不依赖档位。

