# Foabic API 参考（0.1）

本文只列 0.1 的公开 API。内部类（`dev.foabic.api.internal`、`dev.foabic.loader.*`、`dev.foabic.mc26.*`）不属于模组契约。

## 入口

```java
import dev.foabic.api.Foabic;

Foabic.events();
Foabic.scheduler();
Foabic.commands();
Foabic.environment();
Foabic.gameDirectory();
Foabic.mods();
Foabic.mod("example");
Foabic.isReady();
Foabic.VERSION;       // "0.1.0"
Foabic.MINECRAFT;     // "26.n+"
```

在 `ModInitializer.onInitialize` 执行期间 runtime 一定已安装。模组代码之外调用前先检查 `Foabic.isReady()`。

## 模组

```java
@FunctionalInterface
public interface ModInitializer {
    void onInitialize(ModContext context) throws Exception;
}
```

`ModContext`：

```java
ModContainer mod();
EventBus events();
Scheduler scheduler();
CommandRegistry commands();
Environment environment();
System.Logger logger();
default Config config();          // == mod().config()
default ModResources resources(); // == mod().resources()
```

`ModContainer` 提供：

```java
String id();
String name();
Version version();
String description();
List<String> authors();
Environment environment();   // 声明的环境，可能是 UNIVERSAL
ModMetadata metadata();
Path jar();
Path dataDirectory();
System.Logger logger();
ModResources resources();
Config config();
ClassLoader classLoader();
```

`ModResources`：

```java
Optional<InputStream> open(String path) throws IOException;
byte[] read(String path) throws IOException;
boolean contains(String path);
```

路径相对模组 jar 根，禁止 `..`，只能看到当前模组。

`Config`：

```java
Config config = context.config();
config.set("enabled", true);
config.set("limit", 32);
config.save();

int limit = config.getInt("limit", 10);
boolean enabled = config.getBoolean("enabled", false);
```

配置是 properties 格式，显式 `save()`；类型错误会抛出 `IllegalArgumentException`，不会静默回退。

## 事件

```java
EventSubscription subscription = context.events().subscribe(
        ServerTickEvent.class,
        EventPriority.NORMAL,
        event -> { /* ... */ });

subscription.close();
```

- 精确类型分发；
- 优先级：`HIGHEST` → `HIGHEST` 先执行，`LOWEST` 最后；
- 监听器异常由 `EventBus` 捕获并记录；
- `Cancellable` 事件取消后，后续监听器不再执行；
- `post` 同步执行，在调用线程上。

事件列表：

| 事件 | 时机 |
| --- | --- |
| `ServerStartingEvent` | `runServer` 入口 |
| `ServerStartedEvent` | 首个服务器 tick，命令已注册后 |
| `ServerTickEvent` | 每个服务器 tick 开始 |
| `ServerTickEndEvent` | 每个服务器 tick 结束 |
| `LevelTickEvent` | 每个维度 tick 开始 |
| `LevelTickEndEvent` | 每个维度 tick 结束 |
| `PlayerJoinEvent` | 玩家被放入世界后 |
| `PlayerLeaveEvent` | 玩家从列表移除时 |
| `ServerStoppingEvent` | `stopServer` 入口 |
| `ServerStoppedEvent` | `runServer` 返回后 |

事件记录都暴露 `server()`，tick 事件暴露 `long tick()`。

## 命令

```java
context.commands().literal("warp")
        .requires(source -> source.hasPermission(2))
        .then(context.commands().literal("spawn")
                .executes(command -> {
                    command.source().sendMessage("warping...");
                    return 1;
                }))
        .then(context.commands().argument("name", ArgumentType.STRING)
                .suggests(command -> List.of("home", "farm"))
                .executes(command -> {
                    String name = command.getString("name");
                    command.source().sendMessage("warp " + name);
                    return 1;
                }));
```

规则：

- 根命令名 `[a-z0-9_-]{1,32}`，不能与已有根命令重名；
- 子节点名 `[a-zA-Z0-9_-]{1,32}`；
- 参数类型：`STRING`、`WORD`、`GREEDY_STRING`、`INTEGER`、`LONG`、`FLOAT`、`DOUBLE`、`BOOLEAN`；
- `suggests` 只对参数节点有效；Brigadier 的 literal 没有 suggestions，Foabic 会忽略 literal 上的建议；
- 命令在首个服务器 tick 时统一注册；在 `ServerStartedEvent` 中新注册的命令也会在同 tick 再刷新一次；
- handler 返回正整数表示成功，0 表示失败；
- handler 抛异常会记录日志并返回 0，不会断开服务器。

`CommandSource`：

```java
String name();
boolean isPlayer();
boolean hasPermission(int level);
void sendMessage(String message);
Optional<Player> player();
<T> T unwrap(Class<T> type);
```

`CommandContext`：

```java
CommandSource source();
Object get(String name);
String getString(String name);
int getInt(String name);
long getLong(String name);
float getFloat(String name);
double getDouble(String name);
boolean getBoolean(String name);
Map<String, Object> arguments();
```

## 调度器

```java
Task task = context.scheduler().schedule(20, () -> {
    context.logger().log(System.Logger.Level.INFO, "one second later");
});

Task repeating = context.scheduler().scheduleRepeating(0, 20, self -> {
    // runs every second
    if (/* done */ false) {
        self.cancel();
    }
});

task.cancel();
```

- 延迟单位是服务器 tick；
- `0` 表示下一个 tick；
- 周期必须大于 0；
- 固定周期，不追赶；
- 任务在服务器线程执行；
- 任务异常会被记录并取消该任务；
- `cancelAll()` 取消全部任务。

## 游戏视图

`Server`：

```java
String name();
String motd();
String version();
boolean dedicated();
long tick();
int playerCount();
List<Player> players();
Path directory();
<T> T unwrap(Class<T> type);
```

`Player extends CommandSource`：

```java
UUID id();
Server server();
int permissionLevel();
void sendActionBar(String message);
```

`Level`：

```java
ResourceLocation dimension();
Server server();
long time();                 // 26.x 默认 world clock
List<Player> players();
<T> T unwrap(Class<T> type);
```

`unwrap` 用于取回平台对象，例如：

```java
var mcServer = event.server().unwrap(net.minecraft.server.MinecraftServer.class);
```

这会让该模组在编译期依赖 Minecraft；0.1 不阻止这种用法，但核心 API 本身不需要它。

## 版本与标识符

`dev.foabic.api.version.Version`：

```java
Version v = Version.parse("1.2.3-rc.1");
v.compareTo(Version.parse("1.2.3")); // < 0
```

`dev.foabic.api.version.VersionRange`：

```java
VersionRange range = VersionRange.parse(">=1.0 <2.0");
range.test(Version.parse("1.5.0")); // true
```

`dev.foabic.api.util.ResourceLocation`：

```java
ResourceLocation id = ResourceLocation.parse("example:path/to/thing");
```

## 稳定性

- `0.x` 不保证二进制兼容；
- 事件类/接口若在 0.2 修改，会写 changelog；
- 内部包不保证任何兼容性；
- 清单字段只增不改语义；未知字段会被忽略。
