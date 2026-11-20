# Foabic 模组清单格式

Foabic 使用一个放在模组 jar 根目录的文件：`foabic.mod`。

它是 **UTF-8 编码的 Java properties 文件**。选择 properties 而不是 JSON/YAML，是为了不把解析器打进 42 KiB 的 API jar，也避免模组作者为了一个清单引入 Gson/Jackson。代价是格式朴素：没有嵌套、没有数组，只有 `key=value`。

> 文件不要带 UTF-8 BOM。Java 的 `Properties.load(Reader)` 会把 BOM 当成 key 的一部分。

## 最小示例

```properties
id=example
version=1.0.0
entrypoint=com.example.ExampleMod
```

## 字段

| 字段 | 必填 | 说明 |
| --- | --- | --- |
| `id` | 是 | 模组 ID：`[a-z][a-z0-9_-]{0,63}`；不能是 `foabic` 或 `minecraft` |
| `name` | 否 | 显示名，默认等于 id |
| `version` | 是 | SemVer，可写 `1`、`1.2`、`1.2.3`、`1.2.3-rc.1` |
| `description` | 否 | 简介 |
| `authors` | 否 | 逗号或分号分隔 |
| `entrypoint` / `entrypoints` | 是 | 一个或多个 `ModInitializer` 实现类，逗号分隔 |
| `environment` | 否 | `server`、`client`、`*`（默认 `*`） |
| `api` | 否 | 需要的 Foabic API 范围，默认 `^0.1.0` |
| `depends` | 否 | 必需依赖，逗号分隔 |
| `optional` | 否 | 可选依赖，逗号分隔 |

未知字段会被忽略，便于以后扩展。

## 入口类

每个入口类必须：

- 是 `public`；
- 实现 `dev.foabic.api.mod.ModInitializer`；
- 有 `public` 无参构造器；
- 只初始化一次。

多个入口按 `entrypoint` 中列出的顺序执行。一个模组初始化失败会使整个加载失败并给出模组 id 和入口类名。Foabic 不会带着半初始化的模组继续启动。

## 版本与范围

SemVer 比较遵循常见规则：`1.0.0` 高于 `1.0.0-rc.1`，数字段按数值比较，预发布标识按 SemVer 规则比较。

支持的版本范围（AND 组合，空格分隔）：

```properties
api=^0.1.0
depends=library@>=1.2 <2.0
optional=extras@~1.4.0
```

| 写法 | 含义 |
| --- | --- |
| `*` 或空 | 任意版本 |
| `1.2.3` / `=1.2.3` | 精确匹配 |
| `>=1.2.3`、`>1.2.3`、`<=1.2.3`、`<1.2.3` | 边界 |
| `^1.2.3` | `>=1.2.3 <2.0.0` |
| `^0.1.2` | `>=0.1.2 <0.2.0` |
| `~1.2.3` | `>=1.2.3 <1.3.0` |
| `>=1.0 <2.0` | 两个约束同时满足 |

没有 OR 运算符。需要复杂范围时，把它拆成依赖设计，而不是把表达式写成迷宫。

## 依赖

```properties
depends=core@^2.0, utils@>=1.0 <2.0
optional=compat@^3.0
```

规则：

- 逗号分隔多个依赖；
- `id@范围`，省略 `@` 等价于 `@*`；
- 必需依赖缺失或版本不匹配 → 启动失败；
- 可选依赖缺失 → 忽略；
- 可选依赖存在但版本不匹配 → 记录警告并忽略该依赖边；
- 依赖环 → 启动失败，错误信息包含环路径；
- 依赖先于依赖者初始化。

API 版本也会被检查：清单里的 `api` 范围必须匹配当前 loader 的 `Foabic.VERSION`。

## 环境

```properties
environment=server
```

- `SERVER`：只在专用服务器加载。
- `CLIENT`：只在客户端加载；0.1 还没有客户端适配器，因此实际不会加载。
- `UNIVERSAL` / `*`：两边都可加载。

如果 A 模组依赖 B，而 B 因环境被跳过，则 A 会得到“缺失依赖”的明确错误。

## Jar 布局

```text
example.jar
  foabic.mod
  com/example/ExampleMod.class
  data/example/...        # 通过 ModResources 读取
  assets/example/...      # 通过 ModResources 读取
```

Foabic 不解压模组 jar，也不自动把 `data/`/`assets/` 注入原版资源包系统。0.1 的资源 API 只是只读访问；数据包/资源包注入在路线图中。

`ModResources` 被限制在**当前模组自己的 jar**内，路径不能包含 `..`，其他模组的资源不可见。

## 错误与诊断

加载失败会抛出 `ModLoadException`，通常包含：

- jar 文件名；
- 模组 id；
- 缺失/不匹配的依赖与期望范围；
- 入口类或初始化异常；
- 依赖环路径。

模组初始化异常会保留原始 cause，方便在日志中定位。事件监听器、命令执行、调度任务的异常不会导致服务器崩溃：它们会被记录并隔离，命令返回失败结果。

## 为什么不是 JSON

JSON 需要解析器；解析器需要依赖或几百行代码。`foabic.mod` 是纯配置，properties 足够表达。模组真正的复杂配置属于模组自己的 `Config` 或外部文件，不应该塞进加载器清单。
