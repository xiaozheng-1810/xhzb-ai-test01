# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目概述

星海智伴（xhzb）养老护理管理后台，由 RuoYi v3.9.0（若依）改造而来。技术栈：Spring Boot 3.5.0、Spring Security + JWT、MyBatis-Plus、PageHelper、Redis、MySQL、Spring AI 1.1.8、Swagger（springdoc）。根 `pom.xml` 的 Java 版本为 17，但 `xhzb-ai` 模块单独设置为 21，因此构建需要 **JDK 21**。本仓库只有后端，前端不在其中。

业务重点：

- **养老护理平台**：楼层、房间、房型、床位、护理等级、护理项目、护理计划、知识库。
- **健康评估**：分两阶段调用大模型，生成能力等级与健康评估报告。
- **AI 聊天助手**：Spring AI 多模型接入、Redis 会话记忆、基于 Redis 向量库的 RAG。

## 构建与运行

```bash
# 全量打包（跳过测试，项目默认方式）
mvn clean package -Dmaven.test.skip=true

# 仅编译
mvn -T 1C compile -DskipTests

# 构建单个业务模块（-am 会一并构建其依赖模块）
mvn -pl xhzb-nursing-platform -am -DskipTests package

# 运行单个测试类（现有测试多为依赖外部服务的手工探索用例）
mvn -pl xhzb-admin -am -Dtest=ETLTest -DfailIfNoTests=false test
```

- 启动入口：`xhzb-admin` 模块的 `com.xhzb.XhzbApplication`，默认端口 8080。
- 也可先打包，再执行 `java -jar xhzb-admin/target/xhzb-admin.jar`，或使用 `bin/run.bat`（`bin/package.bat`、`bin/clean.bat` 同理）。
- Swagger UI：`/swagger-ui.html`，OpenAPI JSON：`/v3/api-docs`。
- 数据库初始化脚本：`sql/xhzb.sql`。
- 运行依赖的外部服务见下一节；大模型相关密钥通过环境变量注入：`DEEPSEEK_API_KEY`、`BAI_LIAN_API_KEY`、`OSS_ACCESS_KEY_ID`、`OSS_ACCESS_KEY_SECRET`。

## 模块结构

| 模块 | 职责 |
| --- | --- |
| `xhzb-admin` | 唯一可执行入口；`application*.yml`、日志与 Swagger 配置；若依内置的 system/monitor/tool/common Controller |
| `xhzb-framework` | Spring Security/JWT、MyBatis-Plus 分页插件、Redis、资源与线程池等框架配置 |
| `xhzb-system` | 若依系统模块（用户、角色、菜单、部门、字典等） |
| `xhzb-common` | 统一返回 `AjaxResult`/`TableDataInfo`、`BaseController`/`BaseEntity`、异常、工具类（含 `PDFUtil`） |
| `xhzb-quartz` | 定时任务 |
| `xhzb-generator` | 代码生成器 |
| `xhzb-nursing-platform` | 护理业务：实体/Controller/Service/Mapper XML、健康评估 AI 流程 |
| `xhzb-oss` | 阿里云 OSS 文件上传、下载、删除 |
| `xhzb-ai` | Spring AI 聊天客户端、会话历史、工具调用、向量库配置 |

依赖方向：`xhzb-admin` 聚合其余模块；`xhzb-ai` → `xhzb-nursing-platform` → `xhzb-framework`/`xhzb-common`/`xhzb-oss`。业务代码所属包根为 `com.xhzb`，启动类扫描整个 `com.xhzb`。

## 配置与环境

- 配置文件位于 `xhzb-admin/src/main/resources/`，`application.yml` 默认激活 `dev`。
- 开发环境连接：MySQL `192.168.100.168:3306/xhzb`，Redis `192.168.100.168:6379`（密码 `123456`）。
- Spring AI 模型提供方（dev）：
  - Ollama：`http://localhost:11434`，chat `qwen3.5:9b`，embedding `qwen3-embedding:0.6b`。
  - DeepSeek：`https://api.deepseek.com`，模型 `deepseek-flash`，读取 `DEEPSEEK_API_KEY`。
  - OpenAI 兼容（阿里百炼 DashScope）：模型 `qwen3.8-flash`，embedding `qwen3.7-text-embedding`（1024 维），读取 `BAI_LIAN_API_KEY`。
- 向量库：`xhzb-ai` 的 `SpringAIConfig` 中**硬编码**了 `JedisPooled("192.168.100.168", 6378)`，索引名 `spring-ai-index`，前缀 `doc:`，启动时 `initializeSchema(true)`。
- OSS 配置在 `application-dev.yml` 的 `oss.*`，Bucket 名硬编码为 `xiaozheng01`。修改环境时注意这些硬编码地址。

## Spring AI 架构（xhzb-ai）

- `config/ChatClientConfig` 创建三个 `ChatClient` Bean：`openai`、`ollamaai`、`deepseekai`。三者统一配置：
  - 系统提示词 `Constants.PROMPT`；
  - 工具 `NursingProjectTool`（`@Tool` 方法 `find_project_tool`，查询护理项目）；
  - `SimpleLoggerAdvisor` + `MessageChatMemoryAdvisor` + `QuestionAnswerAdvisor`（RAG，相似度阈值 0.8、topK 1）。
- 会话记忆由 `chat/memory/RedisChatMemory` 实现 `ChatMemory`：消息存 Redis List `chat:memory:{chatId}`，并以时间排序还原。
- 会话索引由 `ChatHistoryServiceImpl` 维护：Redis Set `chat:history:{userId}` 保存该用户的 chatId。
- `ChattingController`（`/ai/chat`）使用 `deepseekai` 客户端，返回 `Flux<String>` 流式响应；`ChatHistoryController`（`/ai/history`）提供会话列表、详情、删除。
- `config/SpringAIConfig` 定义 `VectorStore`（Redis + OpenAI Embedding）。
- 知识库写入流程（`KnowledgeBaseServiceImpl.insertKnowledgeBase`）：从 OSS 下载 PDF → `PagePdfDocumentReader` 按页读取 → `TokenTextSplitter(500/200/10)` 切分 → `vectorStore.add` → MySQL `knowledge_base` 落库；切分后的文档 ID 列表以 JSON 存入 `remark`，删除时据此清理向量。单测 `xhzb-admin/src/test/java/com/xhzb/test2/ETLTest.java` 演示了同样的 ETL 流程。

## 健康评估业务链路（xhzb-nursing-platform）

三张表（DDL 见 `sql/xhzb.sql`）：

- `health_assessment`：评估主记录（老人、入住建议、入住状态、评估进度）。
- `health_assessment_data_collection`：表单明细，主键与主记录相同；各模块数据以 JSON 字符串存 `text` 列。
- `health_assessment_report`：AI 评估结果（能力等级、健康评分、风险等级、异常分析、系统分值、护理等级建议等）。

`HealthAssessmentServiceImpl.assessmentData()` 是核心流程：

1. `insertOrUpdateHealthAssessment()` 保存/更新主记录与采集明细（`ElderAssessmentDto`）。
2. 把四大模块的文字等级（能力完好/轻度受损/中度受损/重度受损）转成 0–3，并提取近 30 天跌倒、噎食、自杀、走失、昏迷次数、痴呆、精神疾病、画钟测试结果。
3. 用 `PromptConstant.HEALTH_PROMPT` 调 `healthChatClient`（`nursing/config/HealthAIConfig` 中的 OpenAI 模型客户端），`JSONUtil.toBean` 解析为 `ElderLevelResultVO`（初步等级、最终等级、原因），写入报告。
4. 从 OSS 下载体检 PDF → `PDFUtil.pdfToString` 提取文本 → 用 `PromptConstant.HEALTH_REPORT_PROMPT` 第二次调用模型 → 解析为 `HealthReportResultVO`。
5. 按健康评分计算建议：≥60 建议入住；≥90 三级护理、≥80 二级、≥70 一级、≥60 特级，否则不建议入住。
6. 更新主记录的 `coreSuggestion`、`evaluationProgress`。

接口文档见 `doc/AI评估API.md`；两个 prompt 的规则说明见 `doc/AI评估API 两个参考的prompt.md`。

## 持久层与接口约定

- Service 统一 `IXxxService` + `XxxServiceImpl extends ServiceImpl<XxxMapper, Xxx>`；单表 CRUD 优先用 MyBatis-Plus（`getById`、`save`、`updateById`、`removeById`、`LambdaQueryWrapper`），多表/复杂 SQL 写 `src/main/resources/mapper/nursing/*Mapper.xml`。
- Controller 继承 `BaseController`；列表接口 `startPage();` 紧跟查询，再 `getDataTable(list)`；其余返回 `AjaxResult`（`success(...)` / `toAjax(...)`）。
- 权限：`@PreAuthorize("@ss.hasPermi('nursing:xxx:list')")` 等，权限标识必须与 `sql/xhzb.sql` 中 `sys_menu` 的插入数据一致（list/query/add/edit/remove/export）。
- 写操作与导出接口加 `@Log(title=..., businessType=...)`。
- 实体用 Lombok `@Data`，字段有中文注释；Controller/实体使用 Swagger `@Tag`、`@Operation`、`@Schema`。
- 多次写库的业务方法（如健康评估主表 + 明细 + 报告）应加 `@Transactional(rollbackFor = Exception.class)`。
- 新增/修改表字段时同步更新：`sql/xhzb.sql`、Domain、DTO/VO、Mapper XML 的 `resultMap` 与列清单。
- 业务异常使用项目自定义异常（如 `BaseException` / `ServiceException`），不要抛裸 `RuntimeException`。

## 注意事项

- **构建 JDK**：`xhzb-ai` 编译级别为 21，必须用 JDK 21 构建，否则该模块会失败。
- **测试现状**：没有常规自动化测试套件；`xhzb-admin/src/test` 下的测试多为调用真实模型/Redis/OSS 的手工探索用例，直接运行可能因外部服务不可用而失败。
- **硬编码环境依赖**：向量库地址、OSS Bucket、dev 数据源/Redis 地址均写死在代码或配置中，切换环境时需逐项确认。
- **数据 JSON 化**：健康评估采集明细中大量字段以 JSON 字符串存储，读取时需用 Hutool `JSONUtil` 反序列化，不要假设它们是独立列。
- **分页混用**：项目同时存在 MyBatis-Plus 分页插件与 PageHelper，业务列表接口目前遵循若依的 `startPage()` 写法，不要混用 `Page<T>`。
