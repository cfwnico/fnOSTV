# fnOSTV Android 4

fnOSTV 是支持安卓 4 的旧版电视客户端。

这是参考 `QiaoKes/fntv-electron` 思路实现的 Android 4.x 兼容版 fnOSTV 客户端。Electron 版本把飞牛影视 Web 端包成桌面应用，并加入服务器配置、账号状态、Cookie 保持、播放增强和本地代理等能力；Android 4 版本采用更轻的原生 WebView 外壳，以保证旧电视盒子和旧平板能启动。

## 已实现

- Android 原生 Java 工程，无 AndroidX、Kotlin、Compose 等现代运行时依赖。
- `minSdkVersion 14`，覆盖 Android 4.0 到 Android 4.4。
- 首次启动进入服务器设置页，保存飞牛影视 Web 地址、账号和密码。
- WebView 加载飞牛影视 Web 端，启用 JavaScript、DOM Storage、Cookie 持久化和旧版 WebView 数据库路径。
- 页面加载完成后自动尝试填充账号密码并点击登录按钮。
- 支持 Android TV/遥控器常用交互：
  - 菜单键打开设置页。
  - 返回键优先退出全屏视频，其次执行 WebView 后退。
  - 播放/暂停键尝试控制页面里的第一个 `video` 标签。
- 支持 WebChromeClient 全屏视频。
- SSL 证书异常时弹窗确认；也可以在设置里信任该服务器证书异常。
- 非 HTTP 链接和下载链接交给系统应用处理。

## 与参考 Electron 版的映射

| Electron 版能力 | Android 4 版处理 |
| --- | --- |
| BrowserWindow 承载飞牛 Web 页面 | `MainActivity` + `WebView` |
| 配置服务器和账号 | `SettingsActivity` + `SharedPreferences` |
| Cookie/session 恢复 | `CookieManager` + `CookieSyncManager` |
| 登录态辅助 | 页面完成后注入 `LoginScript` |
| MPV/桌面播放增强 | Android 4 内置 WebView 视频全屏，保留系统外部应用兜底 |
| Node 本地代理 | 暂不内置；Android 4 TLS/代理能力差异较大，优先保证基础访问稳定 |

## 构建

建议使用 Android Studio 4.2.x 或更高版本打开本目录，并安装 Android SDK Platform 28。

命令行环境可执行：

```powershell
gradle assembleDebug
```

当前开发环境未安装 Java、Gradle 和 Android SDK，因此本机尚未生成 APK。

## 使用

1. 安装 APK 后打开 `fnOSTV`。
2. 在设置页填写飞牛影视 Web 地址，例如 `http://192.168.1.20:5666`。
3. 可填写账号密码并开启自动登录。
4. 保存后进入 Web 端。
5. 遥控器菜单键可重新打开设置页。

## Android 4 注意事项

- Android 4 WebView 的内核较旧，飞牛 Web 端如果依赖非常新的 JavaScript、CSS 或 TLS 能力，可能需要服务端降级或使用 HTTP 内网访问。
- 自签名 HTTPS 建议只在可信内网中开启“信任该服务器的 SSL 证书异常”。
- 旧 WebView 的视频格式支持有限，建议服务端优先提供 H.264/AAC MP4 或 HLS。
