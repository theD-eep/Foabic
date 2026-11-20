# 真实环境验证记录

## 环境

| 项 | 值 |
| --- | --- |
| 日期 | 2026-11-20 |
| 操作系统 | Windows 11 |
| Java | OpenJDK 25.0.1（Microsoft build 25.0.1+8-LTS） |
| Minecraft | Java Edition 26.1 官方专用服务器 |
| Foabic | 0.1.0-preview |
| 启动方式 | 官方 server bundle 展开后的 flat classpath：`-javaagent:foabic-0.1.0.jar -cp <server-26.1.jar;libraries/*> net.minecraft.server.Main nogui` |

## 冒烟模组

测试模组只做四件事：

1. 在初始化器中打印 mod id；
2. 监听 starting / started / tick / level tick / stopping / stopped；
3. 注册 `/foabictest` 命令；
4. 通过命令向控制台输出确认信息。

没有安装其他模组，没有 Fabric/NeoForge，没有 Mixin。

## 结果

服务器正常启动到：

```text
Done (0.498s)! For help, type "help"
```

Foabic 日志：

```text
Foabic 0.1.0 starting on SERVER (Minecraft 26.n+)
initializing smoketest 1.0.0
FOABIC_SMOKE init smoketest api=0.1.0
Foabic loaded 1 mod(s) in 128 ms
Minecraft 26.x adapter installed
Foabic 0.1.0 ready: 1 mod(s), agent jar 94 KiB
installed Foabic hooks in net/minecraft/server/level/ServerLevel
installed Foabic hooks in net/minecraft/server/MinecraftServer
installed Foabic hooks in net/minecraft/server/players/PlayerList
registered 1 command(s)
```

事件与命令：

```text
FOABIC_SMOKE starting name=world
FOABIC_SMOKE started version=26.1
FOABIC_SMOKE tick=1
FOABIC_SMOKE level=minecraft:overworld time=1
FOABIC_SMOKE level=minecraft:the_end time=1
FOABIC_SMOKE level=minecraft:the_nether time=0
FOABIC_SMOKE tick=2
FOABIC_SMOKE tick=3
FOABIC_COMMAND_OK from=Server
FOABIC_ARG_OK=hello
FOABIC_SMOKE stopping
FOABIC_SMOKE stopped
SMOKE_EXIT=0
```

`/foabictest` 和 `/foabictest hello` 是通过服务器控制台 stdin 输入的；输出证明 Brigadier 命令注册、权限源包装、literal/argument 解析、`getString` 和 `sendMessage` 全链路可用。

## 自动化测试

```text
PASS versions
PASS events
PASS scheduler
PASS commands
PASS bytecode
PASS loader
ALL TESTS PASSED
```

覆盖内容：

- SemVer 解析/比较、版本范围、非法输入；
- 事件优先级、取消、异常隔离、取消订阅；
- tick 调度器、延迟、重复、取消、异常任务；
- 命令树、参数、重复根、非法名称；
- 真实加载并执行 `BytecodeInjector` 转换后的类（entry/return/多 return 路径）；
- 模组发现、依赖顺序、缺失依赖、重复 id、环境过滤、可选依赖、API 版本拒绝、配置、资源、事件订阅。

## 体积

```text
foabic-api-0.1.0.jar     42.1 KiB
foabic-0.1.0.jar         95.3 KiB
```

## 未验证/已知限制

- **客户端**没有适配器，客户端启动/UI/按键未测试。
- **玩家加入/离开**：hook 已按 26.1 的 `PlayerList.placeNewPlayer` / `remove` 描述符编译，但没有真实客户端连接测试；玩家行为在 0.2 前视为 preview。
- **26.2+**：未测试；方法名/描述符可能变化。
- **官方 server.jar bundler 直接启动**：会产生 `NoClassDefFoundError: dev/foabic/mc26/Mc26Hooks`，因为 bundler 的子加载器 parent 是 platform loader。请使用 `tools/run-server.ps1` / `.sh` 的 flat classpath 启动。
- **内容模组**：没有方块/物品注册，别用 0.1 做这类模组。
