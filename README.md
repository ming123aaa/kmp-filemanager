# File Manager

基于 Kotlin Multiplatform + Compose Multiplatform 的跨平台文件管理器，内置 HTTP 服务器，支持在同一局域网内通过浏览器访问和管理文件。
目前支持 Android 和 desktop。IOS/webApp 还未实现。

## 功能特性

- **远程文件管理** — 通过 HTTP API 连接远程服务器，浏览和管理远程文件
- **内嵌 HTTP 服务器** — 在设备上启动本地 HTTP 服务器，提供完整 RESTful API 和 Web 管理界面
- **文件浏览** — 支持网格、预览、列表三种视图模式
- **文件操作** — 新建、删除、重命名、移动、复制链接
- **批量操作** — 支持 Shift/Ctrl 多选，批量删除、移动、下载
- **文本编辑** — 在线编辑器，支持 UTF-8 / GBK / GB2312 / BIG5 等多种编码
- **媒体预览** — 图片、视频、音频在线预览
- **视频播放器** — 基于 Media3 ExoPlayer
- **下载管理** — 多任务并发下载（最多 5 个），断点续传，文件夹递归下载
- **上传管理** — 多文件上传，进度回调
- **拖拽上传** — Web 界面支持拖拽文件上传
- **只读模式** — 可切换只读/可读写模式，保护文件安全
- **跨平台** — Android、iOS、Desktop (Windows/macOS/Linux)、Web

## 技术栈

| 技术 | 版本 |
|------|------|
| Kotlin | 2.4.10 |
| Compose Multiplatform | 1.11.1 |
| Android Gradle Plugin | 8.9.1 |
| Ktor (HTTP 客户端 + 服务端) | 3.1.3 |
| Coil (图片加载) | 3.5.0 |
| Media3 ExoPlayer | 1.6.1 |
| compileSdk / minSdk / targetSdk | 36 / 24 / 35 |

## 项目结构

```
kmp-fileManager/
├── androidApp/              # Android 应用入口
├── desktopApp/              # Desktop (JVM) 应用入口
├── iosApp/                  # iOS 应用入口 (SwiftUI + Xcode)
├── webApp/                  # 独立 Web 前端 (React + Vite)
├── sharedUI/                # 共享 Compose Multiplatform UI 层
│   └── src/commonMain/      # 跨平台 UI 代码
├── sharedLogic/             # 共享业务逻辑层
│   ├── src/commonMain/      # 跨平台逻辑 (API、数据模型、配置)
│   ├── src/androidMain/     # Android 平台实现
│   ├── src/jvmMain/         # Desktop 平台实现
│   ├── src/iosMain/         # iOS 平台实现
│   └── src/androidAndJvmMain/  # Android + Desktop 共享代码 (HTTP 服务器)
├── build.gradle.kts         # 根构建文件
├── settings.gradle.kts      # 模块配置
├── gradle.properties        # Gradle 属性
└── gradle/libs.versions.toml # 版本目录
```

## 环境要求

- **JDK** 17+
- **Android Studio** (用于 Android 开发)
- **Xcode** (用于 iOS 开发，仅 macOS)
- **Node.js** 18+ (用于 Web 前端)

## 打包命令

### Android

```bash
# Debug APK
./gradlew :androidApp:assembleDebug

# Release APK（已配置签名，使用 androidApp/a123456.jks）
./gradlew :androidApp:assembleRelease
```

输出路径：[androidApp/build/outputs/apk/](androidApp/build/outputs/apk/)

### Desktop

```bash
# 直接运行
./gradlew :desktopApp:run

# 打包当前系统安装包（Windows → MSI, macOS → DMG, Linux → DEB）
./gradlew :desktopApp:packageDistributionForCurrentOS

# 单独打包各平台
./gradlew :desktopApp:packageMsi    # Windows MSI
./gradlew :desktopApp:packageDmg    # macOS DMG
./gradlew :desktopApp:packageDeb    # Linux DEB
```

输出路径：[desktopApp/build/compose/binaries/](desktopApp/build/compose/binaries/)

### iOS(待实现)

1. 在 macOS 上安装 Xcode
2. 先在终端执行一次 Gradle 构建共享框架：
```bash
./gradlew :sharedUI:linkDebugFrameworkIosArm64
```
3. 用 Xcode 打开 [iosApp/iosApp.xcodeproj](iosApp/iosApp.xcodeproj)
4. 选择目标设备，执行 **Product → Archive** 打包

### Web(待实现)

```bash
cd webApp
npm install
npm run build    # 构建生产版本
npm start        # 启动开发服务器
```

输出路径：[webApp/dist/](webApp/dist/)

## 使用说明

1. 启动应用后，进入 **设置** 页面
2. 在「服务器管理」区域配置端口号和根路径
3. 点击「启动服务器」，应用会显示局域网访问地址
4. 同一局域网内的其他设备通过浏览器访问该地址，即可使用 Web 文件管理器
5. 也可在设置中配置远程服务器地址，连接其他运行此应用的设备

## 许可证

MIT License