# fnOSTV Android

fnOSTV Android 是面向旧版 Android 电视盒子和电视设备的飞牛影视轻量客户端。项目已经从早期 WebView 壳升级为原生 Java 客户端路线，在 Android 端直接实现登录、首页、媒体库、文件浏览、收藏、最近播放和内置播放器。

项目参考了 [QiaoKes/fntv-electron](https://github.com/QiaoKes/fntv-electron) 的连接和使用思路，但 Android 版本不依赖 Electron 或桌面 Web 环境。

## 当前版本

- 包名：`com.fnostv.android4`
- 版本号：`0.1.0`
- Version Code：`1`
- 最低系统：Android 4.2 / API 17
- 目标系统：Android 9 / API 28
- 技术栈：Java Android，不使用 Kotlin、Compose 或 AndroidX

## 主要能力

- 原生登录和 token 会话恢复。
- fnOS RPC/WebSocket 连接，以及 REST 文件和媒体接口访问。
- 飞牛影视风格的原生登录页、首页、设置页和线性小图标。
- 首页入口包含首页、收藏、影视大全、分类、搜索、用户和设置。
- 本地媒体库管理：添加、编辑、删除和扫描媒体库目录。
- 本地媒体索引：扫描结果用于首页、影视大全、分类和搜索。
- 影视大全稳定加载：REST 影视服务优先，本地索引兜底，再回退 RPC mediaCenter 和文件模式。
- 文件库目录浏览、媒体文件识别、最近播放、收藏和播放入口。
- 海报匹配和首页海报槽位展示。
- 首页海报墙支持继续观看、收藏和影视大全分区展示，并限制每区渲染数量以照顾旧设备性能。
- 海报解码加入采样保护，降低旧设备海报墙加载时的内存峰值。
- 影视详情页：播放前展示格式、路径、收藏状态和播放源。
- 播放源选择：可在详情页切换可用播放源后再开始播放。
- 内置播放器支持播放/暂停、快进/快退、进度条、倍速、画面适应/铺满和清晰度源切换。
- 播放链路优先使用 LibVLC，失败后回退 IJKPlayer，再失败时交给外部播放器 Intent。
- Android 4.4 x86 模拟器、安装、日志和构建脚本。

## 环境准备

项目提供 Windows 脚本来准备本地 Android 构建环境：

```powershell
scripts\setup-android-env.cmd
```

脚本会使用项目内 `.tooling` 目录中的 JDK 11、Gradle 6.7.1 和 Android SDK。以下内容属于本地环境或敏感配置，不应提交到 Git：

- `.tooling/`
- `local.properties`
- `keystore.properties`
- `signing/`
- `app/build/`
- `logs/`

## 快速开始

生成 debug APK：

```powershell
scripts\build-debug.cmd
```

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

## Release 构建

生成 release APK：

```powershell
scripts\build-release.cmd
```

如需签名 release 包，在仓库根目录准备 `keystore.properties` 和 `signing/` 下的 keystore 文件。可使用脚本生成本地 release keystore：

```powershell
scripts\create-release-keystore.cmd
```

发布前建议至少执行一次：

```powershell
scripts\build-debug.cmd
scripts\build-release.cmd
```

## 使用流程

1. 安装 APK 后打开 `fnOSTV`。
2. 首次进入设置页，填写 fnOS/飞牛影视服务地址、账号和密码。
3. 首次保存有效配置后应用会自动返回主流程，执行原生登录并保存 token。
4. 登录成功后进入原生首页。
5. 进入设置页的“媒体库”，添加媒体库名称、分类和目录路径。
6. 目录路径支持每行一个，例如 `/video/Movies`。
7. 执行“扫描媒体库”，应用会通过 fnOS 文件接口索引目录下的视频文件。
8. 返回首页，通过影视大全、分类、搜索、收藏或继续观看浏览媒体。
9. 点击媒体文件后进入影视详情页，可查看格式、路径、收藏状态和播放源。
10. 选择播放源并确认播放后进入内置播放器。

## 媒体库说明

- 媒体库配置保存在本机，适合旧电视盒子长期稳定使用。
- 扫描深度默认限制为 4 层，最多索引 1000 个视频，避免旧设备长时间卡顿。
- 影视大全加载顺序为 REST 影视媒体库、REST 全部条目、本地媒体库索引、RPC mediaCenter、文件模式。
- UI 副标题会显示当前数据来源，例如 `fnOS 影视媒体库`、`本地媒体库索引` 或 `fnOS mediaCenter 回退`。
- 后续可继续把稳定的 fnOS mediaCenter 管理 API 接到同一套原生界面。

## 播放能力

默认播放链路：

1. 优先使用 LibVLC 3.1.12。
2. VLC 初始化或播放失败时自动切换到 IJKPlayer 0.8.8。
3. IJK 硬解失败时自动切换软解。
4. 内置播放器仍失败时，通过系统 Intent 交给外部播放器。

默认播放策略：

- HTTP/远程流默认 `network-caching=6000ms`。
- 稳定模式默认 `network-caching=3000ms`。
- 局域网/本机低风险 MP4 使用低延迟启动档位：`network-caching=2500ms`、`file-caching=1000ms`。
- 高风险格式、HEVC、4K 或超大文件仍优先使用更保守的流畅档位和软解友好配置。
- 默认关闭跳帧/丢帧，减少画面不连续。
- 播放日志会输出播放器内核、格式、清晰度源、解码方式、缓存档位和缓冲次数。

注意：引入 LibVLC 后，默认构建目标为 Android 4.2+。如果需要继续覆盖 Android 4.0/4.1，应新增 IJK-only 构建变体或只使用外部播放器。

## 遥控器操作

- 方向键：列表导航，播放时左右快进/快退。
- 确认键：打开项目、详情页执行当前操作，或播放/暂停。
- 返回键：关闭详情页、退出播放页或返回上一层。
- 菜单键：详情页收藏/取消收藏，播放时切换倍速。
- 播放/暂停键：详情页开始播放，播放时切换播放状态。
- 上键：播放时切换画面适应/铺满。
- 下键：播放时切换清晰度源。

## 排查建议

播放问题建议重点关注日志中的：

- `VLC preparing`
- `VLC prepared`
- `VLC buffering start/end`
- `VLC fallback to IJK`
- `IJK retry software`
- `options=decoder=... cache=... network=...`

也可以使用错误监听脚本观察运行期异常：

```powershell
scripts\watch-errors.cmd
```

## 项目结构

```text
com.fnostv.android4
├── config   服务器配置、校验和本地存储
├── media    原生媒体库配置、分类、扫描和本地索引
├── net      fnOS 登录、RPC、REST、文件库、收藏、最近播放
├── player   播放器抽象、VLC/IJK 播放引擎、播放性能配置
├── tv       遥控器按键分发
├── ui       原生登录、首页、文件库、播放页、设置页、海报墙
├── util     常量和日志
└── web      WebView 兼容辅助能力
```

`MainActivity` 负责原生模式的生命周期、会话恢复、页面切换和播放协调。`NativeVideoPlayerView` 只依赖 `PlayerEngine` 抽象，当前实现为 `VlcPlayerEngine` 优先、`IjkPlayerEngine` 兜底。

## 测试

运行当前单元测试：

```powershell
. .\scripts\env.ps1
gradle --no-daemon testDebugUnitTest
```

重点测试覆盖媒体库分类、fnOS REST 解析、播放策略、海报采样、文件浏览标签、首页海报槽位和首页视图状态。

## 相关文档

- [RELEASE_NOTES.md](RELEASE_NOTES.md)：版本能力和已知限制。
- [OPEN_SOURCE.md](OPEN_SOURCE.md)：开源使用、分发、贡献和发布要求。
- [NOTICE](NOTICE)：项目声明和第三方组件声明。
- [docs/fnos-interface-map.md](docs/fnos-interface-map.md)：fnOS 接口梳理。

## 已知限制

- LibVLC 3.1.12 的 AAR 声明最低 API 17，因此默认构建不再覆盖 Android 4.0/4.1。
- 当前媒体库管理优先使用本地配置和文件扫描，服务端 mediaCenter 管理 API 仍需根据实际 fnOS API 稳定性继续完善。
- 清晰度切换依赖 fnOS 返回多播放源；没有多码率源时只能显示当前源和实际分辨率。
- 不同旧电视盒子的硬解能力差异较大，遇到 H.265、10bit、高码率或特殊封装时仍可能需要 IJK 软解或外部播放器兜底。

## 开源协议

本项目采用 MIT License 开源，详见 [LICENSE](LICENSE)。开源使用、分发、贡献和发布要求见 [OPEN_SOURCE.md](OPEN_SOURCE.md)，项目声明和第三方组件声明见 [NOTICE](NOTICE)。
