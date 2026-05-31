# fnOSTV Android 项目规则

## 构建系统

- 使用项目内建工具链（`.tooling/` 目录下）：
  - JDK 11: `.tooling/jdk/jdk-11.0.31+11/`
  - Gradle 6.7.1: `.tooling/gradle/gradle-6.7.1/`
  - Android SDK: `.tooling/android-sdk/`
  - AVD 位置: `.tooling/avd/`
- 所有脚本需通过 `scripts/env.ps1` 设置环境变量（JAVA_HOME, ANDROID_HOME 等）
- `local.properties` 由 `env.ps1` 自动生成

## 可用脚本

```powershell
scripts\setup-android-env.cmd    # 首次环境搭建（下载 JDK/Gradle/SDK）
scripts\start-emulator.cmd       # 启动 Android 4.4 模拟器
scripts\build-debug.cmd          # 编译 debug APK
scripts\install-debug.cmd        # 安装并启动 app
scripts\logcat-app.cmd           # 查看应用日志
scripts\watch-errors.cmd         # 监控运行时异常
scripts\build-release.cmd        # 编译 release APK
scripts\test-debug.cmd           # 运行单元测试
```

## 启动模拟器并测试

```powershell
# 顺序执行
scripts\start-emulator.cmd
scripts\build-debug.cmd
scripts\install-debug.cmd
scripts\logcat-app.cmd
```

## 代码规范

- 纯 Java，不使用 Kotlin/Compose/AndroidX
- 最低支持 API 17（Android 4.2），目标 API 28（Android 9）
- 命名：camelCase 方法名，PascalCase 类名，常量大写
- 错误消息使用中文
- 日志使用 `Logger.d/w/e`，tag 为 `"FnOSTV"`
- 注释只写 WHY，不写 WHAT

## 架构

- `config/` — 服务器配置、校验、JSON 持久化
- `media/` — 本地媒体库配置和扫描
- `net/` — REST API 客户端、RPC 通信、数据模型
- `player/` — 播放器抽象层（VLC 优先，IJK 兜底）
- `ui/` — 所有原生 UI 组件（不含 WebView）
- `web/` — WebView 兼容辅助
- `MainActivity` — 主 Activity，生命周期/页面切换/播放协调

## 登录方式

仅 REST API 登录：`FnosRestClient.ensureToken()` 在首次 API 调用时自动 POST `/v/api/v1/login`，缓存 token 供后续请求使用。
