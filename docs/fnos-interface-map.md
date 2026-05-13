# 飞牛影视接口梳理

> 记录时间：2026-05-13。已在同一局域网内登录 `192.168.0.198:5666` 完成 Web 抓包，并将首页/列表/分类/收藏/继续观看对应的 REST 接口接入 Android 端。敏感 token 只在内存中验证，不写入文档或日志。

## 页面与操作

| 页面 | 路由 | 关键操作 | Android 对接状态 |
| --- | --- | --- | --- |
| 登录 | `/v/login?redirect_uri=...` | 用户名、密码、保持登录、NAS 登录 | 已用原生 RPC 登录替代 WebView 登录 |
| 首页 | `/v` | 媒体库入口、继续观看、收藏、分类导航 | 已用 `NativeHomeView` 复刻主要结构，计数接入 `/mediadb/sum` 和 `/play/list` |
| 收藏 | `/v/favorites` | 收藏列表、进入详情/播放 | 已接入 `/favorite/list`，失败时回退本地收藏 |
| 媒体库 | `/v/library/{guid}` | 展示媒体库条目、筛选、布局 | 已接入 `/mediadb/list` + `/item/list`，目录条目回退文件 RPC |
| 全部 | `/v/list/all` | 全部条目、筛选、布局 | 已接入 `/item/list` 的全类型查询 |
| 电影/电视/其他 | `/v/list/movie`、`/v/list/tv`、`/v/list/other` | 分类浏览 | 已接入 `/item/list` 分类查询，失败时回退本地分类 |
| 设置-媒体库 | 设置页媒体库模块 | 新增、编辑、删除、排序、扫描媒体库 | Android 端本地配置、扫描、删除已实现；服务端媒体库管理 API 待实测 |

## 已对接 RPC/WebSocket

Android 4 兼容层不依赖现代 Web 前端，统一走 `ws(s)://<host>/websocket?type=<main|file>`。

| 能力 | Socket | RPC `req` | 请求要点 | 实现位置 |
| --- | --- | --- | --- | --- |
| 获取登录公钥/会话标识 | `main` | `util.crypto.getRSAPub` | 返回 `pub` 和 `si` | `FnosRpcClient.loadSessionId(true)` |
| 获取会话标识 | `main`/`file` | `util.getSI` | token 鉴权前获取 `si` | `FnosRpcClient.loadSessionId(false)` |
| 密码登录 | `main` | `user.login` wrapped by `encrypted` | RSA 包 AES key，AES 包 `{ user, password, app/device, did, si }` | `FnosRpcClient.login()` |
| token 恢复 | `main`/`file` | `user.authToken` | 请求体前拼 HMAC-SHA256 签名 | `FnosRpcClient.authenticateToken()` |
| 文件列表 | `file` | `file.ls` | `path` 为空时列根目录 | `FnosRpcClient.listDir()` |
| 下载/播放直链 | `file` | `file.download` | `files: [path]`，兼容分片 `doing` 响应 | `FnosRpcClient.downloadUrl()` / `playbackSources()` |
| 影视中心候选 | `main` | `app.mediaCenter.home/index/list/recent`、`mediaCenter.home/list` | 逐个探测，递归提取可播放条目 | `FnosRpcClient.mediaCenterEntries()` |

## 已对接 REST API

REST 根路径为 `http(s)://<host>/v/api/v1`。登录成功后，后续请求使用原始 token 作为 `Authorization` 头值，不加 `Bearer` 前缀。

| 能力 | 方法与路径 | 请求/响应要点 | Android 实现 |
| --- | --- | --- |
| Web 登录 | `POST /login` | `{"username","password","app_name":"trimemedia-web"}`；返回 token | `FnosRestClient.ensureToken()` |
| 服务状态 | `GET /server/oauthStatus`、`/sys/config`、`/sys/version`、`/server/info`、`/user/info` | 服务器名、版本、授权目录、用户信息 | 已封装查询方法，当前 UI 主要使用 RPC 登录状态 |
| 首页计数 | `GET /mediadb/sum` | `favorite`、`total`、`movie`、`tv`、`video`，媒体库 guid 作为动态 key | `FnosRestClient.mediaCounts()` |
| 媒体库列表 | `GET /mediadb/list` | `data[]` 含 `guid/title/category/posters` | `FnosRestClient.mediaLibraries()` |
| 影视条目 | `POST /item/list` | `ancestor_guid` 可选，`tags.type` 为 `Movie/TV/Directory/Video`，按 `create_time DESC` 分页 | `FnosRestClient.mediaItems()` |
| 收藏列表 | `POST /favorite/list` | `tags:{}`、`sort_type:"DESC"`、`sort_column:"create_time"` | `FnosRestClient.favoriteItems()` |
| 继续观看 | `GET /play/list` | 返回数组或 `{list,total}` | `FnosRestClient.recentItems()` |
| 任务列表 | `GET /task/running` | 刮削/扫描任务状态 | 已封装 `runningTasks()` |
| 标签字典 | `GET /tag/list`、`/tag/iso6391`、`/tag/iso6392`、`/tag/iso3166`、`/tag/genres` | 分类筛选字典 | 待 UI 筛选面板接入 |
| 图片资源 | `GET /sys/img/...` | poster/backdrop 图片 | 当前 Android 4 原生卡片先用文字海报占位，图片加载待接入 |

## 端到端验证清单

1. 在能访问 `192.168.0.198:5666` 的机器上登录 Web，确认页面路由和字段仍与离线抓取一致。
2. 在 Android 设置页填入 `http://192.168.0.198:5666`、账号和密码，确认原生 RPC 登录成功并保存 token。
3. 打开首页，确认媒体库、全部、电影、电视、其他、收藏、继续观看计数可刷新。
4. 进入设置-媒体库，新增目录后扫描，确认扫描结果进入影视大全。
5. 打开视频，确认 `file.download` 返回的播放源可用，并验证 VLC/IJK/外部播放器回退链路。
6. 返回、遥控方向键、确认键、菜单键、播放页快进/快退/倍速/清晰度切换都需要在 Android 4.4 模拟器或真机上跑一遍。

## 待补接口

- 详情页 `/item/{guid}`、播放页 `/video/{guid}` 的完整播放源/剧集/版本接口还需要继续抓播放链路。
- 服务端媒体库新增、编辑、删除、排序 API 还没有稳定确认；Android 端目前保留本地媒体库管理和文件 RPC，保证 Android 4 设备可用。
- 海报图 `/sys/img/...` 已确认，但 Android 4 原生卡片尚未接入图片缓存和降级加载。

## 本轮 Android 端验证

2026-05-13 在 `emulator-5554` 上安装并启动 debug APK：

- `user.authToken` 成功，应用进入原生首页。
- 首页渲染正常，显示媒体库、收藏、分类、继续观看和搜索/用户/设置入口。
- 首页 REST 计数刷新成功：媒体库 `1`、全部 `2`、电影 `0`、电视节目 `1`、收藏 `0`。
- 点击影视大全后，Android 端通过 `/mediadb/list` + `/item/list` 展示 2 个服务端条目：`文化大观园`、`测试`。
- 点击电视节目后，通过 `/item/list` 分类查询展示 1 个服务端条目：`文化大观园`。
- 点击收藏后，通过 `/favorite/list` 展示服务端空状态 `共 0 项`。
- mediaCenter RPC 候选接口仍返回 `errno=10000002`，现在只作为 REST/RPC 文件模式后的兼容回退。
- 搜索入口可以打开 Android 4 原生 `AlertDialog`。
- 历史日志显示 `file.download`、VLC 首帧、seek 操作均已成功跑通。

截图证据：

- `.tooling/fnostv-e2e-current-20260513.png`
- `.tooling/fnostv-e2e-library-20260513.png`
- `.tooling/fnostv-e2e-search-20260513.png`
- `.tooling/fnostv-rest-home-20260513.png`
- `.tooling/fnostv-rest-library-20260513.png`
- `.tooling/fnostv-rest-tv-20260513.png`
- `.tooling/fnostv-rest-favorites-20260513.png`
