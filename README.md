# fnOSTV Android

fnOSTV 是面向旧版 Android 电视盒子和电视设备的飞牛影视轻量客户端。当前实现已经从早期 WebView 壳升级为原生轻量客户端路线：原生登录、原生首页、文件库浏览、最近播放、收藏、媒体播放和调试脚本都在 Android 端直接实现。

项目参考了 [QiaoKes/fntv-electron](https://github.com/QiaoKes/fntv-electron) 的连接和使用思路，但 Android 版本不再依赖 Electron/Web 桌面环境。

## 当前状态

- 原生 Java Android 工程，不使用 Kotlin、Compose 或 AndroidX。
- 当前内置 VLC 播放器要求 `minSdkVersion 17`，支持 Android 4.2+。
- `targetSdkVersion 28`，便于旧设备运行，同时保留较新的构建工具兼容性。
- 使用 OkHttp 3.12.x 作为 Android 4 兼容网络层。
- 通过 fnOS RPC/WebSocket 流程实现原生登录和 token 恢复。
- 首页参考飞牛影视视觉风格，包含首页、收藏、影视大全、分类、搜索、用户、设置等入口图标。
- 原生设置页支持本地媒体库添加、编辑、删除和扫描。
- 媒体库扫描结果会保存为本地索引，首页、影视大全、分类和搜索优先使用该索引。
- 文件库支持目录浏览、媒体文件识别、最近播放、收藏和播放入口。
- 播放器优先使用 LibVLC，失败后回退 IJKPlayer，再失败时交给外部播放器 Intent。
- 支持播放/暂停、快进/快退、进度条、倍速、画面适应/铺满、清晰度源切换。
- 播放性能配置已统一为 `PlaybackOptions`，默认提升网络缓存并记录诊断日志。

## 架构

```text
com.fnostv.android4
├── config   服务器配置、校验和本地存储
├── media    原生媒体库配置、分类、扫描和本地索引
├── net      fnOS 登录、RPC、文件库、收藏、最近播放
├── player   播放器抽象、VLC/IJK 播放引擎、播放性能配置
├── tv       遥控器按键分发
├── ui       原生登录、首页、文件库、播放页、设置页
├── util     常量和日志
└── web      WebView 兼容辅助能力
```

`MainActivity` 负责原生模式的生命周期、会话恢复、页面切换和播放协调。`NativeVideoPlayerView` 只依赖 `PlayerEngine` 抽象，当前实现为 `VlcPlayerEngine` 优先、`IjkPlayerEngine` 兜底。

## 播放能力

默认播放链路：

1. 优先使用 LibVLC 3.1.12。
2. VLC 初始化或播放失败时自动切换到 IJKPlayer 0.8.8。
3. IJK 硬解失败时自动切换软解。
4. 内置播放器仍失败时，通过系统 Intent 交给外部播放器。

播放优化策略：

- HTTP/远程流默认 `network-caching=6000ms`。
- 稳定模式默认 `network-caching=3000ms`。
- 默认关闭跳帧/丢帧，减少画面不连续。
- 播放日志会输出播放器内核、格式、清晰度源、解码方式、缓存档位和缓冲次数。

注意：引入 LibVLC 后，内置 VLC 播放构建目标为 Android 4.2+。如果需要继续覆盖 Android 4.0/4.1，应新增 IJK-only 构建变体或只使用外部播放器。

## 构建

准备 Android 构建环境：

```powershell
scripts\setup-android-env.cmd
```

生成 debug APK：

```powershell
scripts\build-debug.cmd
```

生成 release APK：

```powershell
scripts\build-release.cmd
```

构建脚本会使用项目内 `.tooling` 目录中的 JDK 11、Gradle 6.7.1 和 Android SDK。`.tooling`、`local.properties`、`keystore.properties`、`signing/` 和构建产物不会提交到 Git。

## 安装调试

启动本地 Android 4.4 模拟器：

```powershell
scripts\start-emulator.cmd
```

安装并启动 debug 包：

```powershell
scripts\install-debug.cmd
```

查看应用日志：

```powershell
scripts\logcat-app.cmd
```

播放问题排查建议重点关注日志中的：

- `VLC preparing`
- `VLC prepared`
- `VLC buffering start/end`
- `VLC fallback to IJK`
- `IJK retry software`
- `options=decoder=... cache=... network=...`

## 使用

1. 安装 APK 后打开 `fnOSTV`。
2. 首次进入设置页，填写 fnOS/飞牛影视服务地址、账号和密码。
3. 保存后应用会执行原生登录并保存 token。
4. 登录成功后进入原生首页。
5. 点击右上角设置，进入“媒体库”页。
6. 选择“添加媒体库”，填写名称、分类和目录路径。目录路径支持每行一个，例如 `/video/Movies`。
7. 选择“扫描媒体库”，应用会通过 fnOS 文件接口索引目录下的视频文件。
8. 返回首页，通过影视大全、分类、搜索、收藏或继续观看浏览媒体。
9. 点击媒体文件后进入内置播放器。

媒体库说明：

- 媒体库配置保存在本机，适合 Android 4 长期稳定使用。
- 扫描深度默认限制为 4 层，最多索引 1000 个视频，避免旧设备长时间卡顿。
- 如果尚未扫描，影视大全仍会尝试 fnOS mediaCenter 接口；不可用时回退文件模式。
- 后续可继续把稳定的 fnOS mediaCenter 管理 API 接到同一套原生界面。

遥控器常用操作：

- 方向键：列表导航、播放时左右快进/快退。
- 确认键：打开项目或播放/暂停。
- 返回键：退出播放页或返回上一层。
- 菜单键：播放时切换倍速。
- 上键：播放时切换画面适应/铺满。
- 下键：播放时切换清晰度源。

## 开源协议

本项目采用 MIT License 开源，详见 [LICENSE](LICENSE)。开源使用、分发、贡献和发布要求见 [OPEN_SOURCE.md](OPEN_SOURCE.md)，项目声明和第三方组件声明见 [NOTICE](NOTICE)。
