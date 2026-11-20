# 贡献指南

感谢你考虑为 Foabic 做贡献。这是一个刻意保持极小的项目：**先读设计约束，再写代码**。

## 环境要求

| 依赖 | 版本 | 用途 |
| --- | --- | --- |
| JDK | **25**（必须，`--release 25`） | 编译、测试、运行 |
| Minecraft 26.1 官方 `server.jar` | 可选 | 只有构建 26.x adapter、跑真实服务器时才需要 |
| Git | 任意较新版本 | 提交 |
| Bash（Windows 上可用 Git Bash） | 可选 | 运行 `build.sh`；Windows 也可只用 `build.ps1` |

Foabic 不要求 Gradle/Maven，也不要在仓库里引入包管理器。构建就是 `javac` + `jar`。

## 构建与测试

```bash
# Linux / macOS（core-only，不构建 adapter）
JAVA_HOME=/path/to/jdk-25 ./build.sh

# 带 adapter（需要官方 server.jar）
JAVA_HOME=/path/to/jdk-25 FOABIC_MC_JAR=/path/to/server.jar ./build.sh

# 跳过测试
SKIP_TESTS=1 ./build.sh
```

```bat
:: Windows
build.cmd -JavaHome "C:\path\to\jdk-25"
build.cmd -JavaHome "C:\path\to\jdk-25" -McJar "C:\path\to\server.jar"
build.cmd -JavaHome "C:\path\to\jdk-25" -SkipTests
```

产物在 `dist/`：

```text
foabic-api-0.1.0.jar            API，模组编译期依赖
foabic-api-0.1.0-sources.jar    API 源码
foabic-0.1.0.jar                agent：API + loader + adapter
foabic-0.1.0-sources.jar        agent 源码（api + loader + mc26）
```

测试套件是纯 Java 的 `main` 程序（`src/test/java/dev/foabic/test/AllTests.java`），没有 JUnit。新增功能时请同时补测试，并确保 `ALL TESTS PASSED`。

## 代码约定

- **零第三方运行时依赖**：不引入 ASM、Mixin、Gson、Jackson、Guava、日志框架。需要解析就写最小解析器，或者重新考虑设计。
- **不扫描 classpath、不做注解反射**：加载只读 `mods/*.jar` 根部的 `foabic.mod`。
- **不创建线程池**：所有回调在服务器线程执行；需要延迟用 `Scheduler`。
- **公开 API 保持小**：新增公开类型前先问“模组真的需要它吗”。内部包（`dev.foabic.api.internal`、`dev.foabic.loader.*`、`dev.foabic.mc26.*`）不属于模组契约。
- 4 空格缩进、UTF-8、无 BOM；事件类型用不可变 `record`。
- 新增/修改公开 API 时，同步更新 `docs/API.md` 和 `CHANGELOG.md`。
- 修改字节码注入（`BytecodeInjector`、`Mc26Transformer`）必须补真实类加载测试。

## 版本号

版本号出现在四处，发布前必须一致：

1. `src/api/java/dev/foabic/api/Foabic.java` 的 `VERSION`
2. `build.ps1` 的 `$Version`
3. `build.sh` 的 `VERSION`
4. 根目录 `CHANGELOG.md` 的小节标题

标签使用 `v` 前缀，例如 `v0.1.0`；Release 工作流会校验标签与 `build.sh` 中的版本一致。

## 提交与 PR

1. Fork，从 `main` 开分支：`feat/xxx`、`fix/xxx`、`docs/xxx`。
2. 本地跑完整构建和测试。
3. **不要提交** `dist/`、`build/`、`server.jar`、`run/`、`.foabic/` 或任何游戏文件；这些已在 `.gitignore` 中。
4. PR 描述写清楚：改了什么、为什么、怎么验证的；涉及 API 变更时附迁移说明。
5. 一个 PR 只做一件事；格式化/重命名请单独开 PR。

## 报告问题

- 功能缺陷用 Bug Report 模板，附上启动日志（`Foabic ... ready`、`loaded N mod(s)`、adapter 日志）和环境信息。
- 安全问题**不要**开公开 issue，见 [SECURITY.md](SECURITY.md)。
- 使用问题请先看 [README](README.md)、[MOD_FORMAT](docs/MOD_FORMAT.md) 和 [API](docs/API.md)。

## 许可证

提交即表示你同意以 [MIT](LICENSE) 许可贡献你的代码。
