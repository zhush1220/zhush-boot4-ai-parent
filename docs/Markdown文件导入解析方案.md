# Markdown 文件导入解析方案

> 模块范围：`zhush-spring-boot-starter-file`（解析能力）、`zhush-module-system`（接口与落库）、`zhush-module-ai`（可选：工具与渲染统一）
> 口径：master 分支（联盟代码已从 master 剥离，本方案不涉及 union 业务）
> 状态：方案设计，**未改动任何代码**
> 关联文档：《文档解析使用指南.md》《对话框自然语言多能力任务实现方案.md》

## 〇、结论摘要

1. 现有文档解析链（Tika）**能读 `.md`，但只当纯文本读**：结构、frontmatter、图片引用全部丢失，输出 HTML 还会把 `#`、`|` 这类语法原样吐进富文本。直接复用等于没做。
2. 推荐引入 **`org.commonmark`（CommonMark Java，Apache-2.0）** 作为结构解析唯一真源，落在 `zhush-spring-boot-starter-file` 新增 `markdown` 包；`zhush-dependencies` BOM 统一管版本（central 当前 0.24.0）。
3. 能力按三层交付：**L0 单文件解析**（`.md` → 结构化 AST + frontmatter + HTML + TOC，零新表）→ **L1 批量导入**（多文件或 zip 含图片资源搬运 + 相对链接重写）→ **L2 业务落地**（公告/博客/门户内容按需接入，导入批次表视接入方数量再建）。
4. 本期只做到 L0 + L1（同步批量），L2 由具体业务方提需求；**不引入向量/知识库切块**（当前全仓无 RAG 底座）。

---

## 一、需求界定

"Markdown 文件导入解析"有三种差别很大的含义，先框定范围：

| 场景 | 输入 | 期望产物 | 本方案 |
|---|---|---|---|
| A. 单篇转富文本 | 一个 `.md` / `.markdown` | HTML + 纯文本 + frontmatter + 目录，回显到编辑器 | ✅ P0 |
| B. 批量导入（含资源） | 多个 `.md`，或一个 zip（`docs/**.md` + `images/**`） | 每篇解析结果 + 图片转存后的可访问 URL + 失败清单 | ✅ P1 |
| C. 内容入库 | B 的产物 + 目标业务表 | 公告 / 博客文章 / 门户内容记录 | ⏸ P2，按业务方需求接线 |
| D. 知识库切块 | B 的产物 | heading-aware 分块 + 向量化 | ❌ 不在本期（无 RAG 底座） |

不在范围内：`.mdx`、AsciiDoc、reStructuredText；在线 Git 仓库抓取（GitHub/Gitee API）；Markdown **导出**（已有 `generateDocument` 走 Word/PDF）。

---

## 二、现状盘点（代码事实）

### 2.1 已有组件

| 组件 | 位置 | 现状能力 |
|---|---|---|
| `FileParseUtils` | `zhush-common` `utils/file/FileParseUtils.java:58` | Tika `AutoDetectParser` 静态工具：`extractText` / `extractImages` / `extractMetadata` / `detectContentType`；默认字符上限 `DEFAULT_MAX_CHARS = 100_000`（`:63`） |
| `DocParseService` | `starter-file` `core/DocParseService.java:38` | 文本 + 内嵌图片上传 + 拼 HTML，出 `DocParseResult{fileName,hasImage,text,html,images}` |
| `FileService.parseDoc` | `module-system` `service/FileService.java:92` | 解析 + 提取出的图片元信息落 `infra_file`，事务内 |
| `POST /admin/system/file/parse-doc` | `module-system` `controller/admin/AdminFileController.java:60` | 权限 `system:file:create`、`@Idempotent(timeout=3)`、`@OperateLog` |
| `parseDocument` Agent 工具 | `module-ai` `tools/AgentCapabilityTools.java:86` | 经 `RemoteFileFetcher.fetchFromStorage` 取字节 → `DocParseService.parse` → 定长截断后回给 LLM（`:104-107`） |
| `MarkdownLiteParser` | `module-ai` `generation/MarkdownLiteParser.java:18` | 零依赖块解析，服务 **输出方向**（LLM Markdown → Word/PDF） |

### 2.2 直接复用会踩的 6 个坑

1. **`.md` 被当纯文本**：`AutoDetectParser`（`FileParseUtils.java:68`）对 `text/markdown` 只走文本解析器，没有 AST、没有标题层级、没有表格结构、没有 frontmatter。
2. **HTML 组装是逐行包 `<p>`**：`DocParseService.buildHtml`（`:111-125`）把每行非空文本包成 `<p>原文</p>`，`# 标题` 变成 `<p># 标题</p>`，表格变成一串 `<p>| a | b |</p>`，编辑器里是废内容。
3. **不转义，存在 HTML 注入**：同一段代码把文件正文与文件名直接拼进 `<p>` / `alt='...'`，正文里的 `<script>` / `<img onerror=>` 会原样进入 HTML（Markdown 常内嵌原始 HTML，这条对 `.md` 尤其致命）。
4. **图片必然丢失**：`extractImages` 只收 Tika 的 *embedded* `image/*` 资源（`FileParseUtils.java:285-318`），而 Markdown 图片是 URL 引用 → `.md` 恒返回空列表，相对路径图片全断。
5. **上传无扩展名白名单**：`zhush.file.allowed-extensions` 在四个环境里都是注释状态（如 `application-dev.yaml:167`），当前任何后缀都能传，导入接口上线前需要显式收紧。
6. **`MarkdownLiteParser` 不能当解析真源**：它只认 ATX 标题、单层列表（`:97-114`）、管道表格，无 frontmatter、无嵌套、无 HTML 渲染（`:21-33` 的正则集可见），定位是"渲染用的降级解析器"，改成双向真源成本高于新建。

结论：**必须新增一条 Markdown 专用解析路径**，与既有 Tika 路径并列而非改造它。

---

## 三、方案总览

```
                 ┌── L0 解析能力（starter-file，新增，无状态、不落库）
上传 .md / zip ──┤     MarkdownParseService
                 │       ├─ 编码归一（BOM/CRLF/GBK 探测）
                 │       ├─ CommonMark Parser  → Document(AST)
                 │       ├─ YamlFrontMatter    → Map<String,Object>
                 │       ├─ HtmlRenderer(+清洗) → html
                 │       ├─ TextContentAccessor → text
                 │       └─ MarkdownStructureExtractor → toc / headings / links / images / codeBlocks / tables
                 │
                 ├── L1 导入编排（module-system，新增）
                 │     MarkdownImportService
                 │       ├─ 批量容器解析（多文件 / zip，防 zip slip + 解压比）
                 │       ├─ 资源搬运：相对图片 → FileClient.upload → URL 重写
                 │       └─ 结果聚合：成功清单 + 失败原因 + infra_file 落库
                 │
                 └── L2 业务落地（按需求接入，本期不做）
                     公告 / 博客 / 门户内容 Service 调 L0/L1 产物写各自业务表
```

设计原则：

- **解析与落库分离**：L0 纯函数式，可被 Controller、Agent 工具、定时任务、单测直接调用；L1 只在需要批量与资源转存时出现。
- **不改现有语义**：`parse-doc` 对 docx/pdf 的行为保持不变，`.md` 走新分支（避免回归已上线的 Word/PDF 转富文本）。
- **一处真源**：Markdown 的 AST 只在 L0 产出一次，HTML/文本/目录/切块都从同一棵树派生。

---

## 四、解析库选型

| 方案 | 依赖 | 优点 | 代价 | 结论 |
|---|---|---|---|---|
| **A. `org.commonmark`** | `commonmark` + `ext-gfm-tables` / `-strikethrough` / `-task-list-items` / `-heading-anchor` / `-autolink` / `-yaml-front-matter`，0.24.0，Apache-2.0，单包约 200KB | 严格 CommonMark 合规 + GFM 扩展；AST 遍历器（`NodeVisitor`）现成；`HtmlRenderer` 可配置 `escapeHtml` / `sanitizeUrls`；frontmatter 官方扩展；与前端 `marked` 语义高度一致 | 扩展生态比 flexmark 小；任务列表/frontmatter 需各自 ext 坐标 | ✅ **推荐** |
| B. `com.vladsch.flexmark` | `flexmark-all` 0.64.8，BSD-2 | 扩展最全（TOC、锚点、定义列表、emoji、Jekyll frontmatter） | 模块极多、传递依赖重、更新慢（0.64.8 为末版线），与本仓"轻量、可控"取向不符 | ⏸ 仅当出现 commonmark 覆盖不了的语法（如 Wiki link / 脚注 + TOC 联动）再评估 |
| C. 扩展 `MarkdownLiteParser` | 零依赖 | 不引新依赖 | CommonMark 边界规则（参考式链接、setext、嵌套缩进、表格对齐、原始 HTML 块、实体转义）实现量大且难自证正确；与前端 marked 输出不一致会长期打架 | ❌ 否 |
| D. Tika 加 Markdown parser | 已引入 Tika | 复用现链 | Tika 面向"抽取文本"，不输出结构树；解决不了坑 2/3/4 | ❌ 否 |

依赖落位（BOM 统一管版本，遵循既有 POI/Tika/OpenPDF 的写法）：

```xml
<!-- zhush-dependencies/pom.xml -->
<properties>
    <commonmark.version>0.24.0</commonmark.version>   <!-- 落地前用 versions:display-dependency-updates 复核 -->
</properties>
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.commonmark</groupId>
            <artifactId>commonmark</artifactId>
            <version>${commonmark.version}</version>
        </dependency>
        <!-- ext-gfm-tables / ext-gfm-strikethrough / ext-gfm-task-list-items /
             ext-heading-anchor / ext-autolink / ext-yaml-front-matter 同版本各一条 -->
    </dependencies>
</dependencyManagement>
```

```xml
<!-- zhush-framework/zhush-spring-boot-starter-file/pom.xml：只在此处显式引入 -->
<dependency>
    <groupId>org.commonmark</groupId>
    <artifactId>commonmark</artifactId>
</dependency>
<!-- 其余 ext 同理，版本交给 BOM -->
```

> 落地前需复核两点：① 各 ext 的 **准确 artifactId**（任务列表扩展在不同版本命名有出入）；② 与 Tika 传递依赖是否有版本重叠（预期无，commonmark 不依赖 POI/PDFBox）。

---

## 五、模块落位与包结构

```
zhush-framework/zhush-spring-boot-starter-file
└─ src/main/java/com/zhush/file/core/markdown/
   ├─ MarkdownParseService.java          // 入口：byte[]/InputStream/String → MarkdownDocument
   ├─ MarkdownDocument.java              // 出参模型（html/text/frontMatter/toc/references/stats/warnings）
   ├─ MarkdownTocNode.java               // 目录节点（level, anchor, title, children）
   ├─ MarkdownReference.java             // 链接/图片引用（inline / reference 两种，含行号与原始定位）
   ├─ MarkdownStats.java                 // 字符/词/行数、各块类型计数、代码块语言分布
   ├─ MarkdownParseOptions.java          // 单次解析开关（是否渲染 HTML、清洗强度、上限）
   ├── internal/MarkdownStructureExtractor.java  // NodeVisitor：收集标题树/链接/图片/表格/代码块
   ├── internal/MarkdownNormalizer.java          // BOM、CRLF、制表符、零宽字符
   ├── internal/MarkdownHtmlSanitizer.java       // 原始 HTML 白名单清洗（Jsoup safelist 或自研）
   └── internal/MarkdownAssetResolver.java       // 相对路径解析与 URL 重写规则（L1 用）

zhush-module-system
├─ service/MarkdownImportService.java     // 批量容器解析 + 资源搬运 + 落库
├─ vo/MarkdownParseVO.java / MarkdownImportVO.java / ImportItemVO.java
├─ query/MarkdownImportQuery.java
└─ controller/admin/AdminFileController.java   // 新增 3 个接口（或独立 AdminMarkdownController）
```

装配：`FileAutoConfiguration`（已在此装配 `DocParseService`，`:80`）追加

```java
@Bean
@ConditionalOnMissingBean
public MarkdownParseService markdownParseService(FileStorageProperties properties) { ... }
```

**接口归属建议**：若端点数 ≤3，直接加在 `AdminFileController`（同一权限域、同一 `infra_file` 语义）；若 L2 之后端点增多，再拆 `AdminMarkdownController`，避免过早分层。

---

## 六、数据契约

### 6.1 `MarkdownDocument`（L0 出参）

| 字段 | 类型 | 说明 |
|---|---|---|
| `fileName` | String | 原始文件名（仅展示，格式判定看内容） |
| `title` | String | 优先级：frontmatter `title` > 首个 H1 > 文件名去后缀 |
| `frontMatter` | `Map<String,Object>` | YAML 头（已按 `strings` 策略安全解析） |
| `body` | String | 剥掉 frontmatter 后的正文 Markdown |
| `html` | String | 渲染结果（已按配置清洗） |
| `text` | String | 纯文本（供摘要/检索/AI） |
| `toc` | `List<MarkdownTocNode>` | 标题树，节点含 `anchor`（与 `ext-heading-anchor` 生成规则一致，保证页内锚点可用） |
| `references` | `List<MarkdownReference>` | 全部链接与图片引用，`resolved=LOCAL_REMOTE/REMOTE/ANCHOR` 三类 |
| `stats` | `MarkdownStats` | 字数、行数、块计数、代码块语言分布 |
| `truncated` | boolean | 是否触发上限截断 |
| `warnings` | `List<String>` | 降级项（未识别语法、图片缺失、层级跳跃 H1→H3 等） |

与既有 `DocParseResult` 的关系：**不替换、不继承**，新增独立模型；`DocParseResult` 继续服务 docx/pdf。若某调用方希望统一入口，由 `FileService` 侧按后缀分派后做一层薄适配，不把 Markdown 字段塞进 `DocParseResult`。

### 6.2 表设计（L1 不建表，L2 视需要）

P0/P1 **零新表**：转存图片继续复用 `infra_file`（与 `FileService.parseDoc` 一致，`:96-109`）。

仅当出现"导入结果需跨请求查询/重试/审计"时，才新增两张通用导入表（不绑 Markdown 语义）：

```
infra_import_batch(id, biz_type, source_name, total, success, failed, status, cost_ms,
                   operator_id, create_time, ...)
infra_import_item (id, batch_id, rel_path, file_id, title, status, error_code, error_msg,
                   create_time, ...)
```

号段与 ID 规范沿用既有约定：雪花 ID 出参序列化为 String，SQL 主键按号段规划（见《SQL 菜单/授权脚本 ID 号段分配》）。

---

## 七、接口设计

| # | 方法 | 路径 | 权限点 | 说明 |
|---|---|---|---|---|
| 1 | POST | `/admin/system/file/parse-markdown` | `system:file:parse` | 单文件解析，`MultipartFile` → `MarkdownDocument`（不落库、不转存资源） |
| 2 | POST | `/admin/system/file/import-markdown` | `system:file:import` | 批量导入（多 `.md` + 图片，或单个 zip），转存资源并改写链接，返回批次结果 |
| 3 | POST | `/admin/system/file/parse-doc` | `system:file:create`（**保持现状**） | 内部按内容 MIME 分派：`text/markdown` 走 L0 新链，其余走原 Tika 链 |

> 接口 3 的分派是可选增强。默认建议**先只做 1、2**，把 `parse-doc` 保持原样，等 Markdown 链稳定后再决定是否统一入口——否则一次改动同时影响已上线的 Word/PDF 转富文本。

统一约定：`Result<T>` 包装、`@Idempotent`（接口 2 用 `timeout=30` + key 由 `文件名+size` 参与，防重复导入）、`@OperateLog(type=IMPORT/COPY)`、Knife4j 注解齐全；context-path 为 `/api`，前端调用带 `/api` 前缀。

响应示例（接口 1）：

```json
{
  "code": 0,
  "data": {
    "fileName": "deploy-guide.md",
    "title": "部署指南",
    "frontMatter": {"date": "2026-10-01", "tags": ["docker", "nginx"], "draft": false},
    "toc": [
      {"level": 2, "anchor": "1-前置条件", "title": "前置条件", "children": []},
      {"level": 2, "anchor": "2-构建镜像", "title": "构建镜像",
       "children": [{"level": 3, "anchor": "21-dockerfile", "title": "Dockerfile"}]}
    ],
    "references": [
      {"kind": "IMAGE", "target": "./images/arch.png", "resolved": "LOCAL", "line": 18},
      {"kind": "LINK", "target": "https://zhush.cloud", "resolved": "REMOTE", "line": 42}
    ],
    "stats": {"chars": 8213, "words": 1120, "headings": 7, "codeBlocks": 5, "tables": 1},
    "html": "<h1 id=\"部署指南\">…",
    "text": "部署指南\n前置条件\n…",
    "truncated": false,
    "warnings": []
  }
}
```

错误码（扩展 `SystemErrorCode` / 复用 `GlobalErrorCode`）：

| 码 | 语义 |
|---|---|
| `FILE_PARSE_ERROR`（既有） | 解析失败兜底 |
| `MARKDOWN_UNSUPPORTED_ENCODING` | 无法确定的编码（非 UTF-8/UTF-8-BOM/GBK） |
| `MARKDOWN_TOO_LARGE` | 超过大小/字符/行数任一上限 |
| `MARKDOWN_BATCH_EMPTY` / `MARKDOWN_BATCH_OVERSIZE` | 批量无可解析文件 / 超数量上限 |
| `MARKDOWN_ARCHIVE_ENTRY_ILLEGAL` | zip 条目路径非法（zip slip / 绝对路径 / `..`） |
| `MARKDOWN_ASSET_MISSING` | 正文引用的本地资源在批次内找不到（降级为 warning，不阻断） |

---

## 八、批量导入与资源搬运（L1 核心）

### 8.1 输入形态

- 表单多文件：`files[] = a.md, b.md, images/1.png`（浏览器目录上传时 Chrome 带 `webkitRelativePath`，需在 DTO 里显式收 `relPaths[]` 与 `files[]` 一一对应）。
- 单 zip：`import.zip`，内部 `docs/**/*.md` + `docs/images/*`。

### 8.2 相对路径解析规则（必须写死并单测覆盖）

1. 以**当前 md 文件所在目录**为基准解析 `./x.png`、`../assets/x.png`；
2. 归一化后必须以批次内某文件的**相对根路径**为前缀（大小写不敏感可选），否则判 `MARKDOWN_ASSET_MISSING`；
3. 归一化前禁止出现 `..` 越出批次根、绝对路径、盘符、UNC——统一在 zip 条目层面拒绝（`MARKDOWN_ARCHIVE_ENTRY_ILLEGAL`）；
4. URL 编码路径（`./images/%E5%9B%BE.png`）先 decode 再匹配；
5. 锚点（`#section`）、协议相对（`//host/x`）、`data:` URI 不参与搬运；`data:` 内联图默认拒绝并转 warning（体积放大明显）。

### 8.3 转存与改写

```
命中本地资源 → FileClient.upload(bytes, safeName, contentType)
            → 落 infra_file（复用 FileService.parseDoc 的落库写法）
            → 取 FileUploadResult.url 写回 AST（同时改写 HTML 与"可选的规范化 Markdown"）
            → 同名资源批次内去重：按内容 SHA-256 复用已上传记录，避免重复传图
```

URL 形态需与部署一致：容器环境 `FILE_DOMAIN=/api/files/`（`deploy/backend/docker-compose.yaml:32`）、`FILE_BASE_PATH=/data/upload`（`:30`），即**回写的是相对路径 URL**，前端可直接用，Agent 工具侧再由 `RemoteFileFetcher` 剥 `/files/` 前缀还原磁盘路径。文档层面要明确：不要把这里的相对 URL 再拼成绝对域名，否则换域名又会出问题（与本项目近期域名切换踩的坑同源）。

### 8.4 规范化 Markdown 回写（可选，默认关）

`returnCanonicalMarkdown=true` 时额外返回"资源已替换 + frontmatter 保留"的 Markdown 原文，供博客/门户"存 Markdown 而非存 HTML"的场景。实现方式：从 AST 渲染 Markdown（自写 `MarkdownWriter`，或用 commonmark 的 `core` 渲染器扩展），**不是字符串正则替换**——后者对参考式链接、多行 HTML 块不可靠。

### 8.5 失败语义

单文件失败 → 记入明细 `failed`，批次继续；zip 非法条目 → 跳过并 warning；整批超过上限 → 直接拒绝（`MARKDOWN_BATCH_OVERSIZE`）。接口 2 同步返回（上限内），不引入 MQ：`zhush-spring-boot-starter-mq` 目前零引用，为这一个功能拉起消息链路不划算。

---

## 九、安全与健壮性

| 风险 | 处置 |
|---|---|
| **XSS（最高优先级）** | 三级策略，配置 `sanitize-policy`：`SAFE`（默认，Jsoup `safelist()` 白名单，原始 HTML 一律清洗）/ `BALANCED`（保留 `span/class/id/style` 有限属性，够高亮样式）/ `RAW`（不清洗，**仅超管 + 明确审计**）。后端清洗不能只依赖前端：AI 助手的 `attachments` 与门户 SSR 都会二次消费 `html`。 |
| **zip slip** | 每个 `ZipEntry` 归一化后校验：无绝对路径、无 `..`、无符号链接属性；以批次根为唯一前缀。单测给 3 组恶意条目。 |
| **zip bomb** | 限制条目数 ≤ `max-archive-entries`（默认 500）、解压总量 ≤ `max-archive-uncompressed`（默认 100MB）、**压缩比 > 100 即中断**；边读边累计字节，禁止 `readAllBytes` 一次性放大。 |
| **frontmatter 反序列化** | SnakeYAML 必须用 `new Loader(new SafeConstructor(new LoaderOptions()))` 或限制 `codePointLimit`/`maxAliasesForCollections`，拒绝 `!!` 自定义标签与锚点爆炸。 |
| **SSRF** | L0/L1 不做服务端外链抓取；若后续加"远程图片本地化"，复用 `RemoteFileFetcher`（`module-ai/generation/RemoteFileFetcher.java:18`，已收敛白名单 + 目录穿越校验），禁止另起 HTTP 客户端。 |
| **超大文件 / 深嵌套** | 单文件 ≤ `max-size`（默认 2MB，容器 `spring.servlet.multipart.max-file-size: 50MB` 是总闸不是本功能闸）；正文 ≤ `max-chars`（默认 200_000，超限 `truncated=true`）；AST 深度 > 100 时停止展开嵌套列表；解析耗时打点，超 `slow-threshold-ms` 告警。 |
| **编码** | UTF-8 / UTF-8-BOM / UTF-16（有 BOM 才判）/ GBK（仅在显式开 `fallback-gbk` 时尝试）；禁止把二进制当文本喂解析器（先 `detectContentType`）。 |
| **文件名** | 展示名保留原文，落库/存路径一律走既有 `FileClient` 命名策略，防路径注入；`DocParseService` 现在把文件名直插 `alt`，新链不得重复这个写法。 |
| **权限与配额** | 独立权限点 `system:file:parse` / `system:file:import` 并在菜单 SQL 里授权（号段规则见既有约定）；若开放给会员端（`AppFileController` 域），必须叠加接口级限流——本项目 `limit_req` 目前全链为零，导入是重接口，务必先限流再放。 |

---

## 十、与既有链路的协同

1. **Agent 工具**：`parseDocument`（`AgentCapabilityTools.java:86`）对 `.md` 现在只会返回带 `#` 的"纯文本"。改动方式二选一，推荐①：
   - ① 新增 `@AgentTool parseMarkdown(fileUrl)`，返回 `{title, toc, text, headings}` 结构化摘要，比整篇塞给 LLM 更省 token（现有 `MAX_PARSE_CHARS` 截断问题自然缓解）；
   - ② 在 `parseDocument` 内按 MIME 分派到 `MarkdownParseService` 取 `text`（小改，但拿不到结构收益）。
   任一方案都要同步 `agent_definition.tool_names` 增量 SQL（参考 `sql/25_agent_capability.sql:27` 的写法），并记住该工具**尚未纳入审批门禁**（`sensitive=true` 只给了 `complianceCheck`）。
2. **输出方向统一（P3，可选）**：`generateDocument` 目前吃 `MarkdownLiteParser`。若 L0 落地，可让 Word/PDF 渲染改为遍历 CommonMark AST，得到"导入与生成同一棵树"的一致性（表格对齐、嵌套列表、脚注都能进 docx）。收益明确但属重构，需要单测兜底后再切，不与本期绑定。
3. **前端一致性**：管理端已用 `marked@^18.0.11` + `highlight.js`（`apps/web-antdv-next/package.json:49-50`）。后端 `ext-gfm-tables/strikethrough/task-list-items` 与 marked 的 GFM 默认行为一致；`heading-anchor` 的 slug 规则要与前端确认（中文标题、去重后缀），否则预览锚点跳不动——这是最容易返工的一条。
4. **富文本编辑器**：公告/内容编辑用的 wangEditor/tiptap 只吃 HTML，`html` 字段即其导入回显值；建议导入后**先进入编辑器草稿态**让用户确认，而不是直接发布。

---

## 十一、配置项

```yaml
zhush:
  file:
    markdown:
      enabled: true
      max-size: 2097152            # 单文件字节上限
      max-chars: 200000            # 正文字符上限，超出截断并置 truncated
      max-batch-files: 200         # 批量文件数上限
      max-archive-entries: 500
      max-archive-uncompressed: 104857600
      max-compression-ratio: 100
      max-ast-depth: 100
      sanitize-policy: SAFE        # SAFE / BALANCED / RAW
      render-html: true
      extract-front-matter: true
      asset-transfer: true         # L1 相对资源转存开关
      return-canonical-markdown: false
      fallback-gbk: false
```

落位：`FileStorageProperties` 嵌套 `markdown`（该类的 `allowedExtensions` 等字段风格已确立，`FileStorageProperties.java:34`）。

> 注意本仓有"yaml 嵌套键与扁平 `@ConfigurationProperties` 不匹配会被静默忽略"的既往教训：新增嵌套配置类后要用一次启动日志或 `@ConfigurationProperties` 绑定断言确认真的绑定上了。

---

## 十二、测试方案（JUnit5 + Mockito + AssertJ，`zhush-spring-boot-starter-test`）

L0 `MarkdownParseServiceTest`（starter-file，纯单测，不拉 Spring 上下文）：

- frontmatter：正常 / 缺失 / 未闭合 / 非法 YAML / `!!` 自定义标签被拒
- 结构：H1~H6 树、层级跳跃 warning、setext 标题、围栏代码块（含语言、内部 `#`、未闭合）
- 表格：对齐符、转义 `\|`、空行终止；任务列表 `[ ]`/`[x]`；参考式链接与图片
- TOC anchor 与 marked 生成规则对齐（中文、重复标题 `-1` 后缀）
- 清洗：`<script>`、`<img onerror>`、`javascript:` 链接；`RAW` 策略下的对照断言
- 编码：UTF-8-BOM、CRLF、GBK（开关内）、二进制内容拒绝
- 上限：字符截断、深度限制、`truncated` 标记

L1 `MarkdownImportServiceTest`（module-system，`FileClient` 用 `mockStatic`/假 storage，参考 `starter-file` 现有 `DocParseServiceTest.java:49-52` 的 `RecordingStorage` 写法）：

- 多文件与 zip 两种输入；相对路径命中/未命中/大小写；同名不同内容不去重、同内容复用
- zip slip（`../evil.png`、`/etc/passwd`、`C:\`）逐条拒绝；压缩比超限中断
- 部分失败时批次统计正确、事务不回滚已成功项的语义验证
- 图片落 `infra_file` 字段完整性（`userType`、`userId`、`storageType`）

定向执行（两个必用标志）：

```
mvn -o -pl zhush-framework/zhush-spring-boot-starter-file -am \
    -Dsurefire.failIfNoSpecifiedTests=false \
    -Dtest=MarkdownParseServiceTest test
```

---

## 十三、分期与工作量

| 期 | 交付 | 涉及文件 | 量 | 验收 |
|---|---|---|---|---|
| **P0** 解析能力 | BOM 依赖 + `markdown` 包 + `MarkdownParseService` + `POST /parse-markdown` + 权限点与菜单 SQL + 单测 | starter-file 新增 ~8 类；module-system 改 3 个（Controller/Service/VO）+ `FileAutoConfiguration` + pom 2 处 | 2~3 人日 | 一份真实 `.md`（含 frontmatter/表格/嵌套列表/代码块/图片）解析出正确 toc、html 干净、text 无语法残留 |
| **P1** 批量与资源 | `MarkdownImportService` + zip/多文件 + 转存改写 + 限额防弹 + `POST /import-markdown` + 明细返回 | module-system 新增 2~3 类；starter-file `MarkdownAssetResolver` | 3~4 人日 | 上传一个 20 篇 + 图片的 zip，得到 20 条 HTML 且图片全部可访问、无越权条目 |
| **P2** 业务接入 | 公告或博客文章调 L0/L1；导入批次表视需要 | 各业务模块 | 按业务定 | 由业务方需求决定，先不设计 |
| **P3** 统一与增强 | `generateDocument` 切 CommonMark AST；`parseMarkdown` Agent 工具；heading-aware 分块 | module-ai | 2~3 人日 | docx/pdf 对嵌套列表与表格还原度提升 |

P0 是 P1 的前置；P3 独立可插队，但不与 P0 同期做（避免一次改动同时碰"解析"和"已上线的生成渲染"）。

---

## 十四、待确认事项（需拍板，影响 P0 边界）

1. **主要使用方是谁**：富文本编辑器导入（只要 html）、内容入库（要 canonical markdown + frontmatter 映射字段）、还是 AI 读文档（只要 text + toc）？三者对 `return-canonical-markdown` 与字段映射的要求不同。
2. **`sanitize-policy` 默认值**：管理端后台导入自家文档，`SAFE` 可能砍掉作者手写的 `<br>`/`<details>`；若选 `BALANCED` 需要同时决定属性白名单范围。
3. **是否允许 `RAW` 出口**：一旦允许，公告/门户 SSR 渲染就是二跳 XSS 面；建议只给超管并留操作日志。
4. **L1 是否需要"目录树保真"**：博客迁移场景通常要求 `docs/sub/x.md` 的层级映射到栏目；若需要，`infra_import_item.rel_path` 就不能省，且 zip 结构约定要写成对外契约。

---

## 十五、明确不做

- 不建知识库/向量表、不引入 embedding 模型（全仓无 RAG 底座，`zhush-spring-boot-starter-search` 亦零引用）。
- 不改造 `DocParseService`/`FileParseUtils` 对 docx/pdf 的既有行为，Markdown 走独立路径。
- 不为导入引入 MQ 或异步任务框架（同步 + 上限即可；超过上限请拆包）。
- 不在服务端拉取外链图片（SSRF 面），需要"远程图片本地化"时单独立项并复用 `RemoteFileFetcher`。
- 不动 `zhush-module-pay` 及其他无关模块。
