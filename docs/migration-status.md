# KMP 迁移状态文档

## 项目结构

```
kmp-fileManager/
├── sharedLogic/          # 共享业务逻辑 (API, 数据模型, 下载管理, 设置)
│   ├── commonMain/       # 跨平台通用代码
│   ├── jvmMain/          # Desktop (JVM) 平台实现
│   └── androidMain/      # Android 平台实现
├── sharedUI/             # 共享 UI (Compose Multiplatform)
│   └── commonMain/       # 跨平台 UI 代码
├── androidApp/           # Android 入口
├── desktopApp/           # Desktop 入口
└── .androidCode/main/    # 原始 Android 代码 (参考)
```

## 编译状态

| 模块 | JVM (Desktop) | Android |
|------|:--:|:--:|
| sharedLogic | 编译成功 | 编译成功 |
| sharedUI | 编译成功 | 编译成功 |
| desktopApp | 启动正常 | — |
| androidApp | — | 编译成功 |

---

## 一、已迁移功能

### 1.1 sharedLogic — 共享业务逻辑

| 文件 | 功能 |
|------|------|
| `ApiService.kt` | 基于 Ktor 的 HTTP API（文件列表、新建/删除/重命名/移动、读写文本、下载、测试连接） |
| `HttpConfig.kt` | 服务器 URL 配置、只读状态检测 |
| `Settings.kt` | expect/actual 键值存储抽象 |
| `Platform.kt` | 平台信息抽象 |
| `data/FileItem.kt` | 文件项数据模型（含格式化、日期计算） |
| `data/DownloadTask.kt` | 下载任务数据模型（状态、进度、格式化） |
| `data/Models.kt` | 枚举/数据类（FilterMode, SortBy, SortDirection, ViewMode, FolderTreeNode, TextEditorNavData） |
| `data/AppDownloadManager.kt` | 下载管理器（并发队列×5、暂停/恢复/取消/重试、断点续传、文件夹下载、下载设置） |
| `data/FileManager.kt` | expect/actual 跨平台文件操作（下载目录、文件读写、存在性检查） |

### 1.2 sharedUI — 共享 UI 组件

| 文件 | 功能 |
|------|------|
| `App.kt` | 应用入口，三屏导航（文件管理、设置、下载） |
| `ui/theme/AppTheme.kt` | 亮色/暗色主题 |
| `ui/components/Breadcrumb.kt` | 面包屑路径导航 |
| `ui/components/Dialogs.kt` | 对话框（新建文件夹、新建文件、重命名、删除、移动、下载、批量删除、批量移动、批量下载、加载中） |
| `ui/components/FileCard.kt` | 网格文件卡片（含操作菜单） |
| `ui/components/FileIcon.kt` | 文件类型图标 |
| `ui/components/FileList.kt` | 文件列表（网格/预览模式） |
| `ui/components/FileTypeHelper.kt` | 文件类型检测与图标映射 |
| `ui/components/PreviewCard.kt` | 预览模式卡片 |
| `ui/components/Toolbar.kt` | 工具栏（筛选、排序、视图切换、新建、多选） |
| `ui/screens/FileManagerScreen.kt` | 远程文件管理主界面（搜索、浏览、CRUD、多选批量操作） |
| `ui/screens/DownloadScreen.kt` | 下载管理界面（任务列表、状态筛选、暂停/恢复/重试、新建下载、下载设置、多选批量删除） |
| `ui/screens/SettingsScreen.kt` | 设置界面（服务器地址配置、测试连接、应用信息、下载目录） |
| `ui/viewmodel/FileViewModel.kt` | 文件管理 ViewModel（CRUD、筛选、排序、多选、文件夹树） |

---

## 二、未迁移功能

### 2.1 核心页面（5 个缺失）

| 页面 | 功能描述 | 优先级 |
|------|------|:--:|
| **TextEditorScreen** | 在线查看/编辑文本文件，URL 链接识别，保存到远程服务器 | 高 |
| **LocalFileManagerActivity** | 浏览设备本地存储，本地文件 CRUD、导入/导出 | 高 |
| **UploadActivity** | 多文件/文件夹上传到服务器，进度显示 | 高 |
| **MediaPreviewActivity** | 全屏图片查看（缩放/平移）、视频播放（ExoPlayer） | 中 |
| **PowerSavingActivity** | 黑屏遮罩降低 AMOLED 屏幕功耗 | 低 |

### 2.2 内嵌 HTTP 服务器（9 个文件，0% 迁移）

> 原始 Android 应用可在手机本地启动 Web 服务器，通过浏览器访问管理文件。

| 文件 | 职责 |
|------|------|
| `server/MutableWebServer.kt` | 可配置的 Web 服务器主类 |
| `server/NormalServer.java` | 普通 HTTP 服务器实现 |
| `server/config/AppConfig.kt` | 服务器配置（端口、主机等） |
| `server/controller/MainApiController.kt` | 主 API 控制器 |
| `server/controller/TestApiController.kt` | 测试 API 控制器 |
| `server/adapter/DownloadAdapter.kt` | 下载适配器 |
| `server/bean/FileBean.kt` | 文件实体 Bean |
| `server/util/AppContext.kt` | 应用上下文工具 |
| `server/util/DownloadUtil.java` | 下载工具类 |

### 2.3 Android 后台服务（2 个）

| 文件 | 职责 |
|------|------|
| `service/DownloadService.kt` | 后台下载服务（通知栏进度） |
| `service/UploadService.kt` | 后台上传服务（通知栏进度） |

### 2.4 状态数据总线框架（8 个文件，0% 迁移）

| 文件 | 职责 |
|------|------|
| `statedata/StateData.java` | 可观察状态数据容器 |
| `statedata/SateDataBus.java` | 状态数据总线 |
| `statedata/StateDataKtx.kt` | Kotlin 扩展 |
| `statedata/Observer.java` | 观察者接口 |
| `statedata/DifferentObserver.kt` | 差异化观察者 |
| `statedata/Consumer.java` | 消费者接口 |
| `statedata/MapConsumer.java` | Map 消费者 |
| `statedata/ForeachUtil.java` | 遍历工具 |

> 注：此框架的功能在 KMP 中已被 Compose 的 `StateFlow` / `MutableState` 替代。

### 2.5 UI 组件（2 个缺失）

| 文件 | 功能 |
|------|------|
| `ui/components/FileListItem.kt` | 文件列表项组件（独立封装） |
| `ui/components/VideoThumbnail.kt` | 视频缩略图生成（依赖 Android MediaMetadataRetriever） |

### 2.6 Android Activity 入口类（5 个缺失）

| 文件 | 功能 |
|------|------|
| `MainComposeActivity.kt` | 主 Activity（Compose 入口） |
| `MyApp.java` | Application 类 |
| `ServiceLauncherActivity.kt` | 服务器启动 Activity |
| `WebActivity.java` | 内嵌 WebView Activity |
| `WebViewSetting.kt` | WebView 配置 |
| `DataMigrationActivity.kt` | 数据迁移 Activity |

### 2.7 工具类（13 个文件，0% 迁移）

| 文件 | 功能 | 平台依赖 |
|------|------|:--:|
| `BatteryOptimizationHelper.kt` | 电池优化白名单请求 | Android |
| `ClipboardUtils.java` | 剪贴板操作 | 可抽象 |
| `CrashHandler.java` | 全局崩溃捕获 | 可抽象 |
| `DisplayUtil.kt` | dp/px 转换 | 可抽象 |
| `ExecUtil.java` | 命令行执行 | 平台相关 |
| `ExoPlayerPoolManager.kt` | ExoPlayer 播放器池管理 | Android |
| `GlideImage.kt` | Glide 图片加载封装 | Android |
| `MediaRefreshHelper.java` | 媒体库刷新 | Android |
| `NetWorkUtil.java` | 网络状态检测 | 可抽象 |
| `PermissionUtil.java` | 运行时权限请求 | Android |
| `RefInvoke.java` | Java 反射工具 | JVM |
| `SPUtil.java` | SharedPreferences 封装 | 已被 Settings 替代 |
| `UploadUtil.java` | 上传工具类 | 可抽象 |
| `UriToFile.java` | Android Uri 到 File 转换 | Android |

---

## 三、迁移率汇总

| 类别 | 原始 Android | KMP 已迁移 | 迁移率 |
|------|:--:|:--:|:--:|
| 数据层 (data/config) | 5 | 7* | 100% |
| UI 组件 (components) | 10 | 8 | 80% |
| 页面 (screens) | 7 | 3 | 43% |
| 服务器模块 (server) | 9 | 0 | 0% |
| 后台服务 (service) | 2 | 0 | 0% |
| 状态管理 (statedata) | 8 | 0 | 0% |
| 工具类 (util) | 13 | 0 | 0% |
| Activity 入口类 | 6 | 1** | 17% |
| 导航 | 1 | 0 | 0% |
| 主题 | 1 | 1 | 100% |
| ViewModel | 1 | 1 | 100% |

> \* KMP 新增了 `FileManager.kt`、`Models.kt`、`Platform.kt`、`Settings.kt`  
> \** `App.kt` 可视为 KMP 的入口类

---

## 四、技术栈对比

| 项目 | 原始 Android | KMP |
|------|------|------|
| HTTP 客户端 | kthttp (自定义库) | Ktor 3.1.3 |
| JSON 序列化 | Gson | kotlinx.serialization 1.8.1 |
| 图片加载 | Glide | 未实现 (可用 Coil) |
| 视频播放 | ExoPlayer | 未实现 |
| 键值存储 | SharedPreferences | expect/actual Settings |
| 状态管理 | StateData 自研框架 | StateFlow + Compose State |
| 导航 | Jetpack Navigation | 手动状态切换 |
| 内嵌服务器 | AndServer | 未实现 |
| UI 框架 | Jetpack Compose | Compose Multiplatform 1.11.1 |
| 协程 | kotlinx.coroutines | kotlinx.coroutines 1.11.0 |

---

## 五、建议迁移优先级

1. **高优先级** — 文本编辑器 (TextEditorScreen)、本地文件管理器 (LocalFileManager)、上传页面 (UploadActivity)
2. **中优先级** — 媒体预览 (MediaPreviewActivity)、可抽象的跨平台工具类 (剪贴板、网络检测)
3. **低优先级** — 内嵌 HTTP 服务器 (server 模块)、Android 后台服务 (service 模块)、Android 独有工具类 (ExoPlayer、Glide、MediaRefresh 等)