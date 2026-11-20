# 路线图

## 0.1 — kernel（当前）

- [x] `foabic.mod` 发现、SemVer 依赖、环境过滤
- [x] 共享类加载器、入口类初始化
- [x] 同步事件总线、优先级、取消、异常隔离
- [x] 服务端生命周期/tick/维度 tick 事件
- [x] 玩家加入/离开事件（编译完成，客户端联调待做）
- [x] 命令树 → Brigadier、0–4 权限映射
- [x] tick 调度器
- [x] properties 配置、模组内只读资源
- [x] JDK classfile 方法边界注入
- [x] 真实 26.1 服务端冒烟测试
- [x] API jar + 完整 agent jar + 构建脚本

## 0.2 — server content

优先级最高的是让“内容模组”可行，而不是继续堆抽象。

- [ ] 方块/物品注册桥接（26.1 registry / bootstrap freeze 前注入）
- [ ] 创造模式标签页、BlockItem、基础数据生成约定
- [ ] 数据包/资源包自动注入（模组 jar 内的 `data/`、`assets/`）
- [ ] 玩家 join/leave 真实客户端联调
- [ ] 更多命令参数：entity/player selector、resource location、registry 元素
- [ ] 命令建议补全的上下文完善
- [ ] 服务端配置重载（非类重载）
- [ ] 26.2+ adapter 兼容层与版本探测

## 0.3 — network + client

- [ ] 自定义 payload 注册/收发（配置阶段 + play 阶段）
- [ ] 客户端生命周期/tick 事件
- [ ] 按键绑定
- [ ] 简单 Screen/HUD 注册辅助
- [ ] 客户端资源包注入

## 0.4 — ergonomics

- [ ] `server.jar` bundler 原生启动（bootstrap 垫片或 launcher 集成）
- [ ] 每模组隔离 classloader（可选，按需）
- [ ] 热重载（先做命令/配置层，不承诺类重载）
- [ ] 多 MC 版本 adapter 并存的发布方式
- [ ] 可选的代码生成器/模板（不进入核心 jar）

## 1.0 — stability

- [ ] API 二进制兼容承诺
- [ ] 事件/清单格式冻结
- [ ] 多版本 CI
- [ ] 安全审计与权限模型文档

## 非目标

- 不做 Mixin 级别的任意方法补丁；
- 不做沙箱（模组是受信任代码）；
- 不把 Gradle 插件、IDE、下载器、账户系统塞进核心；
- 不追求 Fabric 生态兼容（那是另一个量级的工程）；
- 不为了“功能多”牺牲单 jar 体积和可审计性。

## 设计约束

任何新功能都要先回答：

1. 会增加多少 jar 体积/运行时依赖？
2. 是否能用现有 JDK 25 能力完成，而不是引入 ASM/Mixin/Netty 封装？
3. 是否必须进入核心，还是可以做可选模块？
4. 26.x 更新后，失败是否明确、可诊断？
