# 架构与实现

## 目标

Foabic 的目标不是复刻 Fabric，而是验证一个更小的内核：

1. 加载代码之前不扫描 classpath；
2. 分发事件不需要反射/注解；
3. 注入游戏不需要 ASM/Mixin；
4. API 不强制模组依赖 Minecraft 类；
5. 单 jar、零第三方运行时依赖、可审计。

## 分层

```text
+---------------------------------------------------+
| mods/*.jar                                        |
|   foabic.mod + ModInitializer + event listeners   |
+-------------------------+-------------------------+
                          | 实现 API 接口
+-------------------------v-------------------------+
| foabic-api                                        |
|   EventBus / Scheduler / Command tree             |
|   Server / Player / Level / Config / Resources    |
+-------------------------+-------------------------+
                          | 安装 runtime
+-------------------------v-------------------------+
| foabic-loader                                     |
|   discovery / dependency resolver / classloader   |
|   entrypoint invocation / agent                    |
+-------------------------+-------------------------+
                          | 方法边界注入
+-------------------------v-------------------------+
| mc26 adapter                                      |
|   lifecycle hooks / wrappers / Brigadier bridge   |
|   server-only in 0.1                              |
+-------------------------+-------------------------+
                          |
                     Minecraft 26.1
```

`foabic-api` 不引用任何 `net.minecraft` 类。`foabic-0.1.0.jar` 把三层合并成一个约 95 KiB 的 agent jar；`foabic-api-0.1.0.jar` 单独发布给模组编译期使用。

## 启动时序

```text
java -javaagent:foabic.jar ...
  -> FoabicAgent.premain
       -> 解析 gameDir / modsDir / environment
       -> FoabicLoader.load
            -> 扫描 mods/*.jar 的 foabic.mod
            -> 版本/依赖/环境校验
            -> 构建共享 ModClassLoader
            -> 安装 FoabicRuntime 到 Foabic 门面
            -> 按依赖顺序调用 ModInitializer.onInitialize
       -> 反射加载 Mc26Adapter.install
            -> Instrumentation.addTransformer
  -> Minecraft main
```

模组初始化发生在 Minecraft 主类启动之前。初始化阶段应该只注册监听器/命令/配置，不能访问世界、玩家或注册表。

## 为什么不需要 Mixin/ASM

Minecraft 26.1 已完全去混淆，方法名和描述符可以直接使用。Foabic 只需要在极少数方法边界插入一个静态调用：

```text
方法 entry:  aload <local>; invokestatic Mc26Hooks.xxx(Object)V
方法 return: aload <local>; invokestatic Mc26Hooks.xxx(Object)V
```

实现使用 JDK 25 的 `java.lang.classfile` API，编译进 agent jar，不增加依赖。`BytecodeInjector` 约 100 行：

- 匹配非静态方法名；
- 在第一个代码元素前插入 entry hook；
- 在每个 `ReturnInstruction` 前插入 return hook；
- 让 JVM 在定义类时做最终校验。

> 不要在 transformer 内调用 `ClassFile.verify()`。默认 verifier 可能解析正在定义的类，导致同一 classloader 递归加载并抛出 `LinkageError: duplicate class definition`。测试通过“真正定义并执行转换后的类”来验证字节码。

## 注入点

| 类 | 方法 | Entry | Return |
| --- | --- | --- | --- |
| `MinecraftServer` | `runServer` | `serverStarting` | `serverStopped` |
| `MinecraftServer` | `tickServer` | `serverTickStart` | `serverTickEnd` |
| `MinecraftServer` | `stopServer` | `serverStopping` | — |
| `PlayerList` | `placeNewPlayer` | — | `playerJoin` (local 2) |
| `PlayerList` | `remove` | `playerLeave` (local 1) | — |
| `ServerLevel` | `tick` | `levelTickStart` | `levelTickEnd` |

所有 hook 签名都是 `(Object)V`。注入代码不直接引用 Minecraft 类型，因此 hook 类可以被 application loader 安全解析。

## 两个 classloader 的问题

Flat classpath 启动时，Minecraft、agent、模组都在 application loader 的可见范围，`Mc26Hooks -> McBridge -> Minecraft` 可以直接链接。

官方 `server.jar` 是 bundler：它把真正的 server jar 和库解压后，放进一个 **parent 为 platform loader** 的 `URLClassLoader`。agent jar 在 application loader，子 loader 看不到它，于是：

```text
NoClassDefFoundError: dev/foabic/mc26/Mc26Hooks
```

因此 0.1 的启动脚本先展开 bundle，再以 flat classpath 启动。为了给未来的 bundler 支持留出空间，`Mc26Hooks` 设计成 Minecraft-free 的薄垫片：首次调用时用 `AdapterLoader` 创建一个 child-first 的 bridge classloader，其 parent 是 Minecraft 的 classloader。当前 flat 启动下这个 bridge 同样工作；以后可以通过 bootstrap 注入/launcher 让垫片在 bundler 子 loader 中可见。

## 事件总线

- `ConcurrentHashMap<Class<?>, CopyOnWriteArrayList<Listener<?>>>`；
- 订阅时按优先级排序，派发时读快照；
- 监听器异常交给 error handler，不向游戏传播；
- `Cancellable` 事件取消后停止后续监听器；
- 没有注解、没有反射、没有通用事件对象。

## 调度器

- 以服务器 tick 为时钟，不依赖 wall clock；
- `delay=0` 表示下一个 tick；
- 重复任务固定周期，tick 落后时不补跑；
- 任务队列用 `PriorityQueue`，所有 action 在服务器线程执行；
- 任务异常会被记录并取消该任务。

## 类加载与依赖

0.1 使用一个共享的 `URLClassLoader`（parent 为 loader 所在 loader）：

- 优点：实现简单，模组之间可直接调用，初始化顺序可控；
- 代价：没有模组间隔离；两个模组带同名类会冲突；
- API/游戏类 parent-first，防止模组覆盖 `dev.foabic` 或 Minecraft 类；
- 依赖解析只保证初始化顺序，不保证类加载隔离。

真正的隔离（每模组 loader + 依赖可见性）会在需要时引入，不应以牺牲体积为代价提前实现。

## 体积预算

| 组件 | 约 |
| --- | --- |
| API | 42 KiB |
| loader | ~20 KiB |
| 26.x adapter | ~30 KiB |
| 完整 jar | 95 KiB |

没有压缩资源、没有内嵌库、没有 Kotlin 运行时。jar 里只有 `.class` 和 manifest。

## 线程模型

- 模组初始化：agent 的 premain 线程（Minecraft 启动前）；
- 事件/命令/调度任务：Minecraft 服务器线程；
- 唯一由模组触发的异步行为是模组自己创建的线程，Foabic 不提供线程池，也不替模组管理线程。

## 失败策略

- 发现/依赖/初始化失败：阻止服务器启动，抛出带上下文的 `ModLoadException`；
- transformer 失败：阻止目标类加载（`IllegalClassFormatException`），避免“看似加载成功但事件不工作”；
- 事件监听器异常：记录并继续；
- 命令异常：记录、向命令源发送简短失败消息、返回 0；
- 调度任务异常：记录并取消该任务。
