# 发布流程

Foabic 用 Git 标签 + GitHub Release 发布，不发布到 Maven 仓库。所有产物都在 `dist/`，由构建脚本生成。

## 1. 准备版本

版本号出现在四处，必须一致：

1. `src/api/java/dev/foabic/api/Foabic.java` → `VERSION`
2. `build.ps1` → `$Version`
3. `build.sh` → `VERSION`
4. `CHANGELOG.md` → 新小节标题

把 `CHANGELOG.md` 的改动从 `Unreleased`（如有）挪到 `## x.y.z — YYYY-MM-DD`。

## 2. 本地验证

```bash
JAVA_HOME=/path/to/jdk-25 FOABIC_MC_JAR=/path/to/server.jar ./build.sh
```

确认：

- `ALL TESTS PASSED`；
- `dist/` 中四个 jar 齐全（api、api-sources、agent、agent-sources）；
- agent jar 含 `dev/foabic/mc26/`，否则说明没有构建 adapter；
- 启动一次真实 26.1 服务器，确认 `Minecraft 26.x adapter installed` 与 `Foabic 0.1.0 ready`。

Windows 等价命令：`build.cmd -JavaHome ... -McJar ...`，或用 `tools\release.ps1` 一并生成校验和。

## 3. 打标签并推送

```bash
git tag -a v0.1.0 -m "v0.1.0"
git push origin main
git push origin v0.1.0
```

`Release` 工作流会：

1. 校验标签与 `build.sh` 中的 `VERSION` 一致；
2. 从仓库变量 `MC_SERVER_JAR_URL` 下载官方 `server.jar`（不随仓库分发游戏文件）；
3. 构建、跑测试；
4. 生成 `SHA256SUMS`；
5. 创建 GitHub Release 并附加全部 jar 与校验和。

首次发布前，在仓库 **Settings → Secrets and variables → Actions → Variables** 添加：

| 名称 | 值 |
| --- | --- |
| `MC_SERVER_JAR_URL` | 官方 Minecraft 26.1 `server.jar` 下载地址 |

没有这个变量时 Release 会明确失败，而不是发布缺少 adapter 的残缺 agent。

## 4. 发布说明

Release 正文会被自动生成，但建议手动补充：

- 主要变更（对应 `CHANGELOG.md`）；
- 兼容的 Minecraft 版本与 JDK 版本；
- 已知限制（仅服务端、无内容注册、26.2 未验证等）；
- 校验和来自 `SHA256SUMS`。

## 5. 发布后

- 确认 Release 资产的 SHA-256 与本地 `dist/SHA256SUMS` 一致；
- 更新 README 中的体积数字（如果变化）；
- 在 `CHANGELOG.md` 顶部留一个空的 `Unreleased` 小节，方便后续提交。
