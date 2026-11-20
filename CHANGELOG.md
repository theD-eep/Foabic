# Changelog

## 0.1.0 — 2026-11-20

首个 preview。

- 新增 `foabic-api`：事件总线、生命周期/维度/玩家事件、命令树、tick 调度器、properties 配置、模组资源、Server/Player/Level 抽象、SemVer/VersionRange/ResourceLocation。
- 新增 loader：`foabic.mod` 发现、依赖/版本/环境解析、共享类加载器、入口类初始化。
- 新增 26.1 服务端 adapter：JDK classfile 注入、Minecraft 26.x 包装器、Brigadier 命令注册、26.x `PermissionSet` 到 0–4 权限映射。
- 新增 `-javaagent` 启动、flat classpath 服务端启动脚本。
- 新增测试：事件、调度、命令、版本、字节码注入、加载器。
- 在 Minecraft 26.1 专用服务器完成真实启动、tick、维度 tick、命令、停止冒烟测试。
- 已知限制：仅服务端；无方块/物品/网络/客户端；官方 bundler 需通过启动脚本展开后运行。
