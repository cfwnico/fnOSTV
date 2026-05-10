# 开源要求

本项目采用 MIT License 开源。使用、修改、分发或二次开发本项目时，请遵守以下要求。

## 许可要求

- 保留 `LICENSE` 中的版权声明和许可声明。
- 分发源码、二进制 APK、修改版或衍生项目时，应同时附带本项目的 MIT License 文本。
- 如果分发修改版，建议在发布说明、README 或变更记录中说明修改内容和修改者。

## 署名与声明

- 项目名称、说明文档或发布页面中应保留对 `fnOSTV Android 4` 的来源说明。
- 本项目参考了 `QiaoKes/fntv-electron` 的公开实现思路；分发时建议保留 `NOTICE` 中的参考声明。
- 不得使用原作者、参考项目或相关服务方的名称暗示未经授权的官方背书。

## 安全与密钥

- 不得提交或公开 `keystore.properties`、`signing/`、私有服务器地址、账号、密码、Token、Cookie 等敏感信息。
- release 签名文件只应保存在发布维护者的可信环境中。丢失签名文件会导致后续 APK 无法用同一签名升级。
- 如果发现安全问题，请先通过私下渠道联系维护者，不要直接公开可复现攻击细节。

## 依赖与第三方组件

- Android SDK、Gradle、JDK、emulator system images 等工具链由各自上游许可约束，本仓库不重新分发这些工具本体。
- 如果未来引入第三方库、图片、字体、图标、音视频资源或其他素材，必须确认其许可证允许在本项目中使用和分发。
- 引入第三方组件时，应在 README、NOTICE 或单独的依赖清单中补充名称、来源和许可证。

## 贡献要求

- 贡献者提交代码时，应确认自己有权提交这些代码，并同意按 MIT License 授权给本项目。
- 不接受来源不明、许可证不兼容、包含商业闭源素材或包含敏感信息的贡献。
- 新增功能应尽量保持 Android 4.x 兼容目标，不主动引入 AndroidX、Kotlin、Compose 或要求较高 API 的运行时依赖，除非维护者明确接受该兼容性变化。

## 发布要求

- 发布 APK 前应执行至少一次本地构建验证：

```powershell
scripts\build-debug.cmd
scripts\build-release.cmd
```

- 发布 release APK 前应确认签名状态，并妥善保存对应 keystore。
- 发布包不应包含 `.tooling/`、`local.properties`、`keystore.properties`、`signing/`、构建缓存或本地调试数据。

## 当前第三方运行时依赖

- OkHttp 3.12.13，Apache License 2.0，用于 Android 4 兼容网络层和 WebSocket。
- IJKPlayer 0.8.8，LGPL-2.1，用于 Android 4 原生视频播放兼容，包含 FFmpeg 原生播放组件。分发 APK 时应保留 IJKPlayer/FFmpeg 相关许可证声明，不得移除 `NOTICE` 中的第三方组件说明。
