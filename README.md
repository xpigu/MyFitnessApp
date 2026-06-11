# MyFitnessApp (健身管理应用)

欢迎使用 MyFitnessApp！这是一个基于 Android 的健身与健康追踪应用程序，包含运动记录、跳绳计数、饮食记录、高德地图轨迹追踪、通知提醒及徽章成就等功能。

本文档将指导您如何**下载安装**、**本地开发部署**以及**配置自动化发布流程**。

---

## 📦 1. 普通用户安装说明 (直接使用)

如果您只想在手机上安装并使用该应用，不需要编译代码：

1. 打开项目的 **[Releases 页面](https://github.com/xpigu/MyFitnessApp/releases)**。
2. 在最新版本的 `Assets` 列表中，找到并下载以 `.apk` 结尾的文件（例如 `app-release.apk`）。
3. 将 APK 文件发送到您的 Android 手机上。
4. 点击安装（如果手机提示“未知来源”，请在设置中允许安装未知来源应用）。

---

## 💻 2. 开发者本地部署指南 (二次开发)

如果您想在本地运行源码、修改功能或自己打包：

### 🛠 环境要求
- **IDE**: [Android Studio](https://developer.android.com/studio) (推荐最新版)
- **JDK**: JDK 17
- **系统**: Windows / macOS / Linux

### 🚀 启动步骤

1. **克隆项目到本地**
   ```bash
   git clone https://github.com/xpigu/MyFitnessApp.git
   # 或使用 Gitee 镜像: git clone https://gitee.com/pigu233/MyFitnessApp.git
   cd MyFitnessApp
   ```

2. **使用 Android Studio 打开项目**
   - 打开 Android Studio -> `File` -> `Open...` -> 选择 `MyFitnessApp` 文件夹。
   - 等待 Gradle 自动同步完成。

3. **配置高德地图 API Key**
   - 本项目使用了高德地图 SDK，需要您自行申请 API Key。
   - 打开 `app/src/main/AndroidManifest.xml`。
   - 找到 `<meta-data android:name="com.amap.api.v2.apikey" android:value="..." />`。
   - 将 `value` 替换为您自己申请的高德地图 Key。

4. **运行应用**
   - 连接 Android 手机（需开启开发者模式和 USB 调试），或者启动 Android 模拟器。
   - 点击 Android Studio 顶部的 `Run` (绿色三角形图标) 进行编译并安装到设备。

---

## ⚙️ 3. 自动化发布部署指南 (通过 GitHub Actions)

本项目已配置了基于 GitHub Actions 的自动化部署流水线（CI/CD）。当您将新版本推送到 GitHub 时，会自动打包出经过签名的 Release APK，并发布在 Releases 页面中供他人下载。

### 🔑 步骤一：准备发布环境与密钥
为了让 GitHub 能够自动打出正式签名的包，您需要在 GitHub 仓库的 **Settings > Secrets and variables > Actions** 中配置以下 `Repository secrets`：

| Secret 名称 | 描述 | 获取/配置方式 |
| :--- | :--- | :--- |
| `AMAP_API_KEY` | 高德地图的 API Key | 在高德开放平台申请 |
| `GITEE_TOKEN` | Gitee 私人令牌 (用于同步发布版本到Gitee) | 在 Gitee 的 `设置 -> 安全设置 -> 私人令牌` 中生成 (需勾选 projects 权限) |
| `ANDROID_KEYSTORE_BASE64` | 签名文件 (.jks) 的 Base64 编码 | 运行: `[Convert]::ToBase64String([IO.File]::ReadAllBytes("你的jks路径"))` |
| `ANDROID_KEYSTORE_PASSWORD` | 密钥库 (Keystore) 密码 | 创建 `.jks` 时设置的密码 |
| `ANDROID_KEY_ALIAS` | 密钥别名 (Alias) | 创建 `.jks` 时设置的别名 |
| `ANDROID_KEY_PASSWORD` | 密钥 (Key) 密码 | 创建 `.jks` 时设置的密码 |

### 🏷️ 步骤二：触发自动构建与发布
配置好上述 Secrets 后，您只需通过**打标签 (Tag)** 的方式推送代码，即可自动触发打包和部署流程：

1. **提交本地所有修改**
   ```bash
   git add .
   git commit -m "feat: 更新了某某功能"
   git push origin main
   ```

2. **创建版本标签并推送** (例如发布 v1.0.3 版本)
   ```bash
   git tag v1.0.3
   git push origin v1.0.3
   ```

3. **查看发布结果**
   - 推送 Tag 后，打开 GitHub 仓库的 **[Actions 页面](https://github.com/xpigu/MyFitnessApp/actions)** 即可看到名为 `Android Release APK` 的工作流正在运行。
   - 等待运行完成后（全绿），即可在 **[Releases 页面](https://github.com/xpigu/MyFitnessApp/releases)** 看到新生成的发布版本及可下载的 APK 文件。

---

## ❓ 常见问题排查

- **Q: GitHub Actions 签名步骤报错 `Unable to locate executable file ... zipalign`**
  - **A**: 这是因为找不到指定的 Build Tools。当前项目工作流 `.github/workflows/android-release-apk.yml` 已经强制指定 `BUILD_TOOLS_VERSION: "34.0.0"`。如遇问题，请检查工作流文件中的版本号。
- **Q: 构建报错缺少权限或 XML 语法错误**
  - **A**: 在发布 Release 包时，Android Lint 会进行严格检查。请确保在本地终端运行 `./gradlew lintRelease` 不报错后再推送到远程仓库。
