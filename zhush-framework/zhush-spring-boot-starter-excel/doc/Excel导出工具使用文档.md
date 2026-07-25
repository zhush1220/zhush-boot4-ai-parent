# zhush-spring-boot-starter-excel 使用文档

基于 **Apache Fesod**（`org.apache.fesod:fesod-sheet`，原 EasyExcel 的 Apache 孵化版，API 完全兼容）封装的 Excel 导出工具，提供：

- ✅ 简单导出：注解模型（`@ExcelProperty`）一行代码导出
- ✅ 多级表头 / 合并表头：一级表头合并两个或以上二级表头，支持任意级数
- ✅ 复杂导出：动态表头（运行时构建表头，无需实体类）
- ✅ 多 Sheet 导出：一次导出多个 Sheet，各 Sheet 独立配置
- ✅ 图片导出：单图、一格多图、图文混合、边距、跨行列
- ✅ 扩展点：所有方法均可追加 `WriteHandler`（合并策略、样式策略等），默认注册列宽自适应

## 1. 引入依赖

```xml
<dependency>
    <groupId>com.zhush</groupId>
    <artifactId>zhush-spring-boot-starter-excel</artifactId>
</dependency>
```

> 版本由 `zhush-dependencies` 统一管理，无需指定 version。

## 2. 核心类一览

| 类 | 说明 |
|---|---|
| `ExcelUtils` | 导出入口工具类（静态方法），支持写响应流 / 任意输出流 |
| `ExcelHeadBuilder` | 动态多级合并表头构建器 |
| `ExcelSheetData` | 多 Sheet 导出时单个 Sheet 的数据描述 |
| `ExcelImageCell` | 图片单元格构建器（多图、边距、跨行列、图文混合） |

包路径均为 `com.zhush.excel.core`。

## 3. 简单导出（注解模型）

### 3.1 定义导出模型

```java
@Data
public class UserExcelVO {

    @ExcelProperty("序号")
    private Integer id;

    // 相邻列同名一级表头自动合并："基本信息" 横向合并 2 个单元格
    @ExcelProperty({"基本信息", "姓名"})
    private String name;

    @ExcelProperty({"基本信息", "年龄"})
    private Integer age;

    @ExcelProperty({"联系方式", "手机号"})
    private String mobile;

    @ExcelProperty({"联系方式", "邮箱"})
    private String email;
}
```

### 3.2 Controller 导出

```java
@GetMapping("/export")
public void export(HttpServletResponse response) {
    List<UserExcelVO> list = userService.listExportData();
    // 文件名可不带 .xlsx 后缀，自动补齐；中文文件名已处理编码
    ExcelUtils.export(response, "用户列表", "用户", UserExcelVO.class, list);
}
```

生成的表头效果：

```
+------+-----------------+-------------------+
| 序号 |    基本信息      |     联系方式       |
|      +--------+--------+---------+---------+
|      |  姓名  |  年龄  |  手机号  |  邮箱   |
+------+--------+--------+---------+---------+
```

### 3.3 写到任意输出流（保存文件、上传 OSS 等）

```java
try (FileOutputStream out = new FileOutputStream("D:/用户列表.xlsx")) {
    ExcelUtils.write(out, "用户", UserExcelVO.class, list);
}
```

## 4. 复杂导出（动态多级合并表头）

无需实体类，运行时动态构建表头，适合列不固定的场景。

### 4.1 构建合并表头

```java
List<List<String>> head = ExcelHeadBuilder.create()
        .col("序号")                            // 单级列，自动纵向合并占满表头高度
        .group("基本信息", "姓名", "年龄")        // 一级表头合并 2 个二级表头
        .group("联系方式", "手机号", "邮箱")
        .col("备注")
        .build();
```

三级及以上表头：

```java
List<List<String>> head = ExcelHeadBuilder.create()
        .col("序号")
        .group(new String[]{"成绩", "理科"}, "物理", "化学")   // 一级"成绩"、二级"理科"合并"物理""化学"
        .group(new String[]{"成绩", "文科"}, "语文", "历史")
        .build();
```

也可以用 `col(...)` 逐列指定任意级数：`col("成绩", "理科", "物理")`。

### 4.2 导出

```java
// 行数据为 List<List<Object>>，与表头列一一对应
List<List<Object>> data = new ArrayList<>();
data.add(List.of(1, "张三", 25, "13800000000", "zhangsan@zhush.com", "无"));
data.add(List.of(2, "李四", 30, "13900000000", "lisi@zhush.com", "VIP"));

ExcelUtils.export(response, "用户列表", "用户", head, data);
```

> 合并原理：表头结构为"每列自上而下的各级标题"，相邻列相同的上级标题自动横向合并；
> 级数不足的列（如"序号"）由 `build()` 用最后一级标题向下补齐，自动纵向合并。

## 5. 多 Sheet 导出

每个 Sheet 通过 `ExcelSheetData` 描述，注解模型与动态表头可混用：

```java
List<ExcelSheetData> sheets = new ArrayList<>();

// Sheet1：注解模型
sheets.add(ExcelSheetData.of("用户", UserExcelVO.class, userList));

// Sheet2：动态表头，并独立追加该 Sheet 的样式/合并策略
List<List<String>> head = ExcelHeadBuilder.create()
        .col("部门").group("统计", "人数", "在职率").build();
sheets.add(ExcelSheetData.of("部门统计", head, deptRows)
        .addWriteHandler(new LoopMergeStrategy(2, 0)));   // 示例：每 2 行合并第 1 列

ExcelUtils.exportMultiSheet(response, "综合报表", sheets);
// 非 Web 场景：ExcelUtils.writeMultiSheet(outputStream, sheets);
```

## 6. 图片导出

### 6.1 注解模型（开箱即用）

字段类型为 `byte[]`、`File`、`InputStream`、`URL` 时自动写出为图片：

```java
@Data
public class GoodsExcelVO {

    @ExcelProperty("商品名称")
    private String name;

    @ExcelProperty("商品图片")
    private byte[] image;               // 图片字节数组，直接写出为图片

    @ExcelProperty("网络图片")
    private URL imageUrl;               // 自动下载并写出

    // String 图片路径需显式指定转换器，否则按普通文本写出
    @ExcelProperty(value = "本地图片", converter = StringImageConverter.class)
    private String imagePath;
}
```

### 6.2 ExcelImageCell 构建器（精细控制）

将图片包装为 `WriteCellData`，可作为动态表头行数据的列值，或注解模型中
`WriteCellData<Void>` 类型的字段值：

```java
// 1. 最简单：单图（支持 byte[] / File / URL 三种重载）
WriteCellData<Void> cell = ExcelImageCell.of(imageBytes);

// 2. 复杂：多图 + 边距 + 跨列 + 图文混合
WriteCellData<Void> cell = ExcelImageCell.create()
        .image(png1).margin(2, 2, 2, 2)      // 上、右、下、左边距（像素）
        .image(png2).span(1, 2)              // 该图向下覆盖 1 行、向右覆盖 2 列
        .text("图文混合")                     // 单元格同时显示文字
        .build();

// 3. 在动态表头导出中使用（图片行务必调大行高）
List<List<String>> head = ExcelHeadBuilder.create()
        .col("姓名").group("资料", "头像", "证件照").build();
List<Object> row = new ArrayList<>();
row.add("张三");
row.add(ExcelImageCell.of(avatarBytes));
row.add(ExcelImageCell.create().image(idPhotoBytes).margin(5, 5, 5, 5).build());

ExcelUtils.export(response, "用户资料", "资料", head, List.of(row),
        new SimpleRowHeightStyleStrategy(null, (short) 60));   // 内容行高 60
```

`ExcelImageCell` 方法说明（`margin/span/imageType/anchorType` 作用于**最近一次** `image(...)` 添加的图片）：

| 方法 | 说明 |
|---|---|
| `of(byte[] / File / URL)` | 静态快捷方法，单图单元格 |
| `create()` | 创建构建器 |
| `image(byte[] / File / InputStream / URL)` | 添加一张图片，可多次调用实现一格多图 |
| `margin(top, right, bottom, left)` | 图片边距，单位像素 |
| `span(rows, cols)` | 图片跨行/跨列展示（≥1） |
| `imageType(ImageData.ImageType)` | 显式指定图片格式（默认自动识别 PNG/JPEG 等） |
| `anchorType(ClientAnchorData.AnchorType)` | 锚定方式（默认随单元格移动并缩放） |
| `text(String)` | 单元格文字内容，与图片同时展示 |
| `build()` | 构建 `WriteCellData<Void>` |

## 7. WriteHandler 扩展（合并 / 样式 / 行高）

所有导出方法末尾均可追加任意个 `WriteHandler`：

```java
ExcelUtils.export(response, "报表", "数据", head, data,
        // 内容区域每 5 行合并第 1 列
        new LoopMergeStrategy(5, 0),
        // 表头行高 25、内容行高 60（图片行常用）
        new SimpleRowHeightStyleStrategy((short) 25, (short) 60),
        // 自定义表头/内容单元格样式
        new HorizontalCellStyleStrategy(headStyle, contentStyle));
```

常用内置 Handler（包 `org.apache.fesod.sheet.write.*`）：

| Handler | 用途 |
|---|---|
| `LongestMatchColumnWidthStyleStrategy` | 列宽自适应（**已默认注册**，无需手动添加） |
| `SimpleRowHeightStyleStrategy` | 统一设置表头/内容行高 |
| `LoopMergeStrategy` | 内容区域循环合并 |
| `OnceAbsoluteMergeStrategy` | 指定区域一次性合并 |
| `HorizontalCellStyleStrategy` | 表头/内容样式策略 |

## 8. 常见问题

**Q1：导出的图片太小 / 行高不够？**
图片默认填充单元格，追加 `new SimpleRowHeightStyleStrategy(null, (short) 60)` 调大内容行高；
需要更大展示区域时用 `ExcelImageCell.span(rows, cols)` 跨行列。

**Q2：`String` 类型的图片路径字段导出的是文字而不是图片？**
`String` 默认按文本写出，需显式指定 `@ExcelProperty(converter = StringImageConverter.class)`。

**Q3：一级表头没有合并？**
合并的前提是**相邻列**的上级标题字符串完全相同。注解模型中两个字段的一级标题必须一致
（如都是 `"基本信息"`），且两列在类中相邻声明。

**Q4：异常处理？**
Web 导出方法（`export*`）内部捕获异常后统一抛出
`ServiceException(GlobalErrorCode.INTERNAL_ERROR)`，由全局异常处理器接管；
非 Web 方法（`write*`）直接抛出，由调用方处理。

**Q5：需要导入（读取）Excel 怎么办？**
当前模块仅封装导出，导入可直接使用 Fesod 原生 API：
`FesodSheet.read(inputStream, DemoData.class, listener).sheet().doRead()`。
