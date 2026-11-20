# Foabic

**26.n+ 的极轻量 Minecraft 模组接口。**

> A ~95 KiB server-side mod loader for Minecraft 26.n+, built on the JDK 25 class-file API. No Mixin, no ASM, no runtime dependencies.

Foabic 不是另一个“大而全”的模组平台。它只做三件事：**加载模组、分发事件、提供最小的服务端 API**。核心没有 Mixin、没有 ASM、没有 Gson、没有运行时第三方依赖，完整 jar 约 **95 KiB**，API jar 约 **42 KiB**。

> 状态：`0.1.0-preview`，服务端优先。已在 **Minecraft Java Edition 26.1** 专用服务器上完成真实启动、命令、生命周期和停止测试。客户端、方块/物品注册、网络包和资源包注入尚未实现，见 [路线图](docs/ROADMAP.md)。

---

## 为什么能这么小

| 设计决定 | 结果 |
| --- | --- |
| 用 JDK 25 自带的 `java.lang.classfile` 注入方法边界 | 不打包 ASM / Mixin / Byte Buddy |
| 26.1 官方 jar 已去混淆 | 不需要 mappings、不需要 remap |
| 自写 properties 清单、SemVer、事件总线、调度器 | 0 个运行时依赖 |
| 不扫描 classpath、不做注解反射 | 启动只读取 `mods/*.jar` 的 `foabic.mod` |
| 不创建线程池、不做后台轮询 | 所有回调都在服务器线程上执行 |

实测产物（Windows，Java 25.0.1）：

```text
foabic-api-0.1.0.jar     42.1 KiB
foabic-0.1.0.jar         95.3 KiB   # API + loader + 26.x adapter
```

Fabric Loader 与 Fabric API 通常是多 MB 级组合；Foabic 完整发行版仍小于 100 KiB，但代价是功能边界更窄。

---

## 0.1 能提供什么

- **模组发现与依赖解析**：`foabic.mod`、SemVer 版本范围、必需/可选依赖、循环检测、按依赖顺序初始化。
- **共享类加载器**：模组之间可以直接互相调用；API 和游戏类始终优先于模组自带副本。
- **事件总线**：类型安全、优先级、取消、异常隔离、可取消订阅；没有注解扫描。
- **服务端生命周期事件**：starting / started / tick start / tick end / stopping / stopped。
- **维度 tick 事件**。
- **玩家加入 / 离开事件**。
- **命令树**：literal / 参数 / 权限 / 建议 / 执行，自动转换为 Brigadier 注册到服务器。
- **Tick 调度器**：一次性/重复任务，固定周期，不追赶、不占后台线程。
- **配置**：每个模组独立的 properties 配置。
- **资源**：只能读取自己 jar 内资源的只读视图。
- **Java 25 / Minecraft 26.1 服务端适配器**：方法边界注入，不修改游戏源码。

不提供（目前）：方块/物品/实体注册、配方/战利品表注入、自定义网络包、客户端 UI/按键、热重载、数据包自动注入、任意方法 Mixin。

---

## 快速开始

### 1. 构建

需要 **JDK 25**（26.1 的最低 Java 版本）和官方 26.1 服务端 `server.jar`。

Linux / macOS：

```bash
export JAVA_HOME=/path/to/jdk-25
export FOABIC_MC_JAR=/path/to/server.jar
./build.sh
```

Windows：

```bat
build.cmd -JavaHome "C:\path\to\jdk-25" -McJar "C:\path\to\server.jar"
```

（或 `powershell -ExecutionPolicy Bypass -File build.ps1 ...`）

产物在 `dist/`：

```text
foabic-api-0.1.0.jar           # 模组编译期依赖
foabic-api-0.1.0-sources.jar   # API 源码
foabic-0.1.0.jar               # 运行时 agent（含 API + loader + 26.x adapter）
foabic-0.1.0-sources.jar       # agent 源码（api + loader + mc26）
```

如果只有 API 也能构建（不传 `-McJar`），但不会包含 26.x adapter。

### 2. 启动服务端

把 `server.jar`、`foabic.jar`、`mods/` 放在同一个工作目录：

```text
server/
  server.jar
  foabic-0.1.0.jar
  eula.txt            # eula=true
  server.properties
  mods/
    your-mod.jar
```

Windows：

```bat
tools\run-server.cmd -JavaHome "C:\path\to\jdk-25"
```

（或 `powershell -ExecutionPolicy Bypass -File tools\run-server.ps1 ...`）

Linux / macOS：

```bash
JAVA_HOME=/path/to/jdk-25 ./tools/run-server.sh
```

脚本会首次把官方 bundle 解压到 `.foabic/runtime/`，然后用 **flat classpath** 启动真正的服务器主类并加上 `-javaagent`。

> 为什么不能直接 `java -javaagent:foabic.jar -jar server.jar`？
> 26.1 的 `server.jar` 是 bundler，它把游戏放进一个 parent 为 platform loader 的子 `URLClassLoader`。agent 的类在 application loader 中，子 loader 看不到它们。`run-server` 走 flat classpath，绕开了这个隔离；直接在已展开的 classpath 上启动时，`-javaagent` 是可以工作的。bundler 原生支持在后续版本处理。

### 3. 写一个 Foabic 模组（API 片段）

`foabic.mod`（放在 jar 根目录，UTF-8 properties 格式）：

```properties
id=example
name=Example
version=1.0.0
entrypoint=com.example.ExampleMod
api=^0.1.0
environment=server
```

入口类：

```java
package com.example;

import dev.foabic.api.event.ServerTickEvent;
import dev.foabic.api.mod.ModContext;
import dev.foabic.api.mod.ModInitializer;

public final class ExampleMod implements ModInitializer {
    @Override
    public void onInitialize(ModContext context) {
        context.logger().log(System.Logger.Level.INFO, "hello from " + context.mod().id());

        context.events().subscribe(ServerTickEvent.class, event -> {
            if (event.tick() == 1) {
                context.logger().log(System.Logger.Level.INFO, "first tick");
            }
        });

        context.commands().literal("hello")
                .executes(command -> {
                    command.source().sendMessage("Hello, " + command.source().name());
                    return 1;
                });
    }
}
```

编译：

```bash
javac -cp foabic-api-0.1.0.jar -d classes src/com/example/ExampleMod.java
jar --create --file example.jar -C classes . -C . foabic.mod
```

把 `example.jar` 放进 `mods/`，重启服务器。不需要注册、不需要注解、不需要构建插件。

---

## 仓库结构

```text
src/api/java      dev.foabic.api.*        公开 API，零游戏依赖
src/loader/java   dev.foabic.loader.*     加载器、事件总线、调度器、agent 入口
src/mc26/java     dev.foabic.mc26.*       26.x 适配器，编译期依赖 server.jar
src/test/java     dev.foabic.test.*       纯 Java 测试，无 JUnit
docs/             设计、清单格式、API、验证与发布文档
tools/            启动服务器与发布脚本
```

构建只需要 JDK 25：`javac` + `jar`，没有 Gradle/Maven。

---

## 事件与线程

事件、命令和调度任务都在服务器线程同步执行；模组初始化发生在服务器启动之前。

```text
ServerStartingEvent
  -> 首个 tick: ServerStartedEvent
  -> 每个服务器 tick:
       scheduler.tick(tick)
       ServerTickEvent
       LevelTickEvent (每个维度)
       ...
       LevelTickEndEvent
       ServerTickEndEvent
  -> ServerStoppingEvent
  -> ServerStoppedEvent
```

监听器抛出的异常会被事件总线捕获并记录，不会中断其他监听器或游戏循环。监听器应保持短小；需要延迟执行时用 `Scheduler`，不要依赖自己创建的后台线程。

事件总线是**精确类型**分发：订阅 `ServerTickEvent` 不会收到其他事件。事件类都是不可变 record，订阅类型是明确的。

---

## API 一览

| 入口 | 作用 |
| --- | --- |
| `Foabic.events()` | 全局事件总线 |
| `Foabic.scheduler()` | tick 调度器 |
| `Foabic.commands()` | 命令树 |
| `Foabic.mods()` / `Foabic.mod(id)` | 已加载模组 |
| `Foabic.environment()` | `SERVER` / `CLIENT` |
| `ModContext` | 单个模组的容器、日志、配置、资源、服务 |
| `ModContainer` | 元数据、jar、数据目录、类加载器 |
| `Server` / `Player` / `Level` | 与 Minecraft 解耦的只读视图 |
| `unwrap(Class<T>)` | 需要完整平台对象时取回 MC 实例 |

完整的接口说明见 [API.md](docs/API.md)，清单格式见 [MOD_FORMAT.md](docs/MOD_FORMAT.md)。

### 权限

命令的 `requires` 谓词和 `CommandSource.hasPermission(int)` 使用传统的 0–4 权限等级。26.1 内部改成了 `PermissionSet`，适配器已经在边界完成映射；普通模组不需要接触 `PermissionSet`。

---

## 兼容性与边界

- **Minecraft**：26.1 服务端已实测；`26.n+` 的适配器结构可复用，但 26.2 若方法名/描述符变化，需要重新编译/调整 adapter。
- **Java**：必须 25+（26.1 的最低要求）。
- **客户端**：尚未接入；`environment=client` 的模组会被服务端安全跳过。
- **注册表/内容**：0.1 不提供方块、物品、实体注册。不要用这个版本做内容模组。
- **Mixin**：不支持，也不打算内置。Foabic 的钩子是有限、可审计的方法边界注入。
- **安全**：模组是受信任的 JVM 代码，Foabic 不做沙箱。只安装你信任的模组。
- **热重载**：不支持；重启服务器加载新模组。

---

## 设计文档

- [架构与字节码注入](docs/ARCHITECTURE.md)
- [模组清单格式](docs/MOD_FORMAT.md)
- [公开 API 参考](docs/API.md)
- [真实 26.1 验证记录](docs/VERIFICATION.md)
- [路线图与限制](docs/ROADMAP.md)

## 构建与测试

```bash
./build.sh                 # 需要 JAVA_HOME 和 FOABIC_MC_JAR 才能构建 adapter
SKIP_TESTS=1 ./build.sh    # 跳过测试
```

Windows：

```bat
build.cmd -JavaHome "C:\path\to\jdk-25" -McJar "C:\path\to\server.jar"
```

测试覆盖：SemVer/版本范围、事件优先级/取消/异常隔离、调度器、命令树、字节码注入（真实加载并执行转换后的类）、模组发现/依赖/配置/资源/环境过滤/API 版本拒绝。

## 参与贡献

- 开始前请读 [贡献指南](CONTRIBUTING.md) 和 [行为准则](CODE_OF_CONDUCT.md)。
- 安全问题请按 [安全策略](SECURITY.md) 私下报告，不要开公开 issue。
- CI 在 Ubuntu 与 Windows 上用 JDK 25 构建 core 并跑测试；维护者发布流程见 [RELEASING.md](docs/RELEASING.md)。

## 免责声明

Foabic 是独立的第三方项目，与 Mojang Studios、Microsoft 无关联，也未获其认可或批准。
“Minecraft”是 Mojang Studios 的商标。本项目不包含、不分发任何 Minecraft 游戏文件；
构建 26.x adapter 时使用你自己合法获取的官方 `server.jar`。

## License

MIT。见 [LICENSE](LICENSE)。
