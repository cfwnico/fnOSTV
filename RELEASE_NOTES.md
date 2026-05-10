# 发布版本信息

## fnOSTV Android 4 v0.1.0

- 版本号：`0.1.0`
- Version Code：`1`
- 包名：`com.fnostv.android4`
- 最低系统：Android 4.0 / API 14
- 目标系统：Android 9 / API 28
- 构建类型：Release
- APK 路径：`app\build\outputs\apk\release\app-release.apk`
- APK 大小：`33,811 bytes`
- SHA-256：`5D4ABDC6EB6507088528271B7B44F0878D5BE9C1A8CB20BD318C742BB0CB1B2C`

### 本次发布内容

- 提供 Android 4.x 兼容的 fnOSTV WebView 客户端。
- 支持首次启动配置飞牛影视 Web 地址、账号、密码、自动登录和 SSL 异常信任策略。
- 支持 Cookie 持久化、DOM Storage、旧版 WebView 数据库路径和全屏视频播放。
- 支持 Android TV/遥控器常用交互：菜单键进入设置、返回键退出全屏或网页后退、播放/暂停键控制页面视频。
- 提供 debug/release 打包脚本，以及 Android 4.4 x86 本地模拟器调试脚本。
- 补充 MIT License、NOTICE 和开源使用/分发/贡献要求。
- 完成架构重构，将配置、WebView、遥控器、UI 状态和工具类分层，便于后续 review 和维护。

### 验证结果

- `scripts\build-release.cmd`：通过。
- `apksigner verify --verbose`：v1 和 v2 签名通过。
- `aapt dump badging`：包名、版本号、最低系统和启动 Activity 正常。

### 已知限制

- Android 4 WebView 内核较旧，如果飞牛 Web 端依赖较新的 JavaScript、CSS 或 TLS 能力，建议优先使用可信内网 HTTP 访问或在服务端做兼容降级。
- 首次 release 打包会在本地生成签名文件：`signing\fnostv-release.jks` 和 `keystore.properties`。这两个文件不会提交到 Git，发布维护者需要自行备份。
- 自动登录依赖页面表单结构识别，若服务端登录页结构变化，可能需要调整 `LoginScript`。

### 安装调试

安装到已连接设备：

```powershell
.tooling\android-sdk\platform-tools\adb.exe install -r app\build\outputs\apk\release\app-release.apk
```

本地模拟器调试：

```powershell
scripts\start-emulator.cmd
scripts\install-debug.cmd
scripts\logcat-app.cmd
```
