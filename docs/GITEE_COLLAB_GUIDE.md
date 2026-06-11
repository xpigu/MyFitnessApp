# MyFitnessApp Gitee 协作开发说明

## 适用范围

本文档用于统一 `MyFitnessApp` 项目的 Git 与 Gitee 协作流程。

- 主仓库：`https://gitee.com/pigu233/MyFitnessApp.git`
- 主分支：`main`
- 协作原则：每个人先在自己的功能分支开发，确认稳定后再合并到 `main`
- 适用场景：3 人协作开发，也适合后续继续扩展成员

如果终端里 `git` 命令不可用，请在 PowerShell 中先定义：

```powershell
$git = 'C:\Program Files\Git\cmd\git.exe'
```

如果你的终端已经可以直接使用 `git`，那么下面所有 `& $git` 都可以直接替换成 `git`。

## 分支命名建议

不要直接在 `main` 上长期开发，每个成员都应使用自己的分支。

推荐示例：

- `feature/profile`
- `feature/diet`
- `feature/workout`
- `fix/reminder-bug`
- `docs/update-guide`

命名规则建议：

- 新功能：`feature/模块名`
- 修复问题：`fix/问题名`
- 文档修改：`docs/主题名`

## 第一次拉取项目

如果协作者第一次获取项目代码，建议直接从 Gitee 克隆：

```powershell
$git = 'C:\Program Files\Git\cmd\git.exe'
& $git clone https://gitee.com/pigu233/MyFitnessApp.git
cd .\MyFitnessApp
```

克隆完成后，先检查远程仓库配置：

```powershell
& $git remote -v
```

一般情况下，克隆后会自动生成 `origin` 远程。如果团队想统一把主远程命名为 `gitee`，可以执行：

```powershell
& $git remote rename origin gitee
& $git remote -v
```

如果希望同时保留 GitHub 作为备用远程，可以额外添加：

```powershell
& $git remote add origin https://github.com/xpigu/MyFitnessApp.git
& $git remote -v
```

## 每个人每天开发前必须执行的命令

这是整个团队最重要的一部分。开始写代码前，必须先同步最新主分支，避免后面集中冲突。

### 情况一：继续在自己原来的功能分支上开发

```powershell
$git = 'C:\Program Files\Git\cmd\git.exe'
& $git fetch gitee
& $git checkout main
& $git pull gitee main
& $git checkout feature/你的分支名
& $git merge main
& $git status
```

这几条命令的含义：

- `fetch gitee`：先把远程最新信息取下来
- `checkout main`：切到本地主分支
- `pull gitee main`：同步团队已经整合好的最新代码
- `checkout feature/...`：切回你自己的开发分支
- `merge main`：把最新主分支合并进你的分支
- `status`：确认当前工作区是否正常

### 情况二：今天要开始一个新任务，新建自己的功能分支

```powershell
$git = 'C:\Program Files\Git\cmd\git.exe'
& $git fetch gitee
& $git checkout main
& $git pull gitee main
& $git checkout -b feature/你的任务名
& $git status
```

这样可以确保新分支是从最新的 `main` 拉出来的，不会基于旧代码开发。

## 标准开发流程

### 1. 开发前先确认状态

```powershell
$git = 'C:\Program Files\Git\cmd\git.exe'
& $git status
& $git branch
```

确认你当前所在分支正确，并且没有遗留的未处理改动。

### 2. 本地开发并调试

你完成功能后，建议先在本地进行基础验证，再提交到远程。

这个 Android 项目建议先执行：

```powershell
.\gradlew.bat assembleDebug
.\gradlew.bat test
```

如果涉及页面、交互或设备能力，建议再在 Android Studio 里运行并手动调试一次。

### 3. 提交自己的修改

```powershell
$git = 'C:\Program Files\Git\cmd\git.exe'
& $git add .
& $git commit -m "feat: 完成 xxx 模块"
```

提交信息建议写清楚一点，例如：

- `feat: 完成运动记录页面`
- `fix: 修复提醒设置闪退`
- `refactor: 重构仓库层逻辑`
- `docs: 更新协作说明`

### 4. 把自己的分支推送到 Gitee

如果这个分支是第一次推送：

```powershell
$git = 'C:\Program Files\Git\cmd\git.exe'
& $git push -u gitee feature/你的分支名
```

如果这个分支之前已经推送过：

```powershell
$git = 'C:\Program Files\Git\cmd\git.exe'
& $git push gitee feature/你的分支名
```

## 负责人如何把其他人的修改同步到本地调试

当其他协作者已经把代码推送到 Gitee 后，你可以用以下两种方式把他们的代码同步到本地调试。

### 方式一：同步已经合并进 `main` 的代码

如果协作者的功能已经被合并到 `main`，你只需要同步主分支：

```powershell
$git = 'C:\Program Files\Git\cmd\git.exe'
& $git fetch gitee
& $git checkout main
& $git pull gitee main
```

同步完成后，建议立刻执行本地调试验证：

```powershell
.\gradlew.bat assembleDebug
.\gradlew.bat test
```

这是你后续进行整体验证、联调和总调试时最常用的方法。

### 方式二：提前拉取某个协作者的功能分支做联调

如果对方的代码还没合并进 `main`，但你想提前在本地看效果或一起联调，可以直接拉他的分支。

先抓取远程分支信息：

```powershell
$git = 'C:\Program Files\Git\cmd\git.exe'
& $git fetch gitee
```

如果本地还没有这个分支，可以创建一个本地跟踪分支：

```powershell
& $git checkout -b feature/队友分支名 gitee/feature/队友分支名
```

如果本地已经有这个分支：

```powershell
& $git checkout feature/队友分支名
& $git pull gitee feature/队友分支名
```

这种方式适合提前检查某个成员的模块，或者在合并前做联调测试。

## 负责人日常整合流程

如果你是最后负责整合和调试的人，建议每天按下面的流程执行。

### 每日整合检查

```powershell
$git = 'C:\Program Files\Git\cmd\git.exe'
& $git fetch gitee
& $git checkout main
& $git pull gitee main
& $git branch -a
```

### 拉到最新后立即做基础验证

```powershell
.\gradlew.bat assembleDebug
.\gradlew.bat test
```

### 如果要把某个成员分支先合到本地 `main` 再调试

```powershell
$git = 'C:\Program Files\Git\cmd\git.exe'
& $git fetch gitee
& $git checkout main
& $git pull gitee main
& $git merge gitee/feature/队友分支名
```

本地验证通过后，再推回 Gitee：

```powershell
& $git push gitee main
```

## 如果本地有未提交修改，怎么同步别人的代码

在拉取别人最新代码之前，先执行：

```powershell
$git = 'C:\Program Files\Git\cmd\git.exe'
& $git status
```

如果工作区不是干净的，建议用以下两种方式之一处理。

### 方式一：先提交当前进度

```powershell
$git = 'C:\Program Files\Git\cmd\git.exe'
& $git add .
& $git commit -m "wip: 保存当前开发进度"
```

然后再同步：

```powershell
& $git fetch gitee
& $git checkout main
& $git pull gitee main
```

### 方式二：先暂存当前改动

```powershell
$git = 'C:\Program Files\Git\cmd\git.exe'
& $git stash
& $git fetch gitee
& $git checkout main
& $git pull gitee main
& $git stash pop
```

如果当前改动还不适合提交，用 `stash` 会更方便。

## 发生冲突时怎么处理

如果你和协作者修改了同一个文件的同一段代码，在 `pull` 或 `merge` 时就可能出现冲突。

先查看冲突状态：

```powershell
$git = 'C:\Program Files\Git\cmd\git.exe'
& $git status
```

打开冲突文件后，你会看到类似下面的标记：

```text
<<<<<<< HEAD
你本地的代码
=======
队友的代码
>>>>>>> branch-name
```

处理方法：

1. 手动决定保留哪部分代码
2. 删除冲突标记
3. 保存文件
4. 执行下面命令完成冲突处理

```powershell
& $git add .
& $git commit -m "fix: 解决合并冲突"
```

## 团队协作规则

为了尽量减少冲突，建议整个团队统一遵守下面的规则：

1. 不要直接在 `main` 上长期开发
2. 每天开始开发前必须先同步 `main`
3. 功能做到一个可用阶段就及时提交并推送自己的分支
4. 推送前至少执行一次本地构建或测试
5. 提交信息要清晰，不要只写 `update` 或 `fix bug`
6. 只有验证通过的代码才允许合并到 `main`

## 快速命令清单

### 每天开始开发前

```powershell
$git = 'C:\Program Files\Git\cmd\git.exe'
& $git fetch gitee
& $git checkout main
& $git pull gitee main
& $git checkout feature/你的分支名
& $git merge main
& $git status
```

### 每天开发结束后

```powershell
$git = 'C:\Program Files\Git\cmd\git.exe'
& $git add .
& $git commit -m "feat: 描述你今天完成的内容"
& $git push gitee feature/你的分支名
```

### 负责人同步并调试最新代码

```powershell
$git = 'C:\Program Files\Git\cmd\git.exe'
& $git fetch gitee
& $git checkout main
& $git pull gitee main
.\gradlew.bat assembleDebug
.\gradlew.bat test
```

## 最终建议

对于你们这个项目，最简单也最稳的流程是：

- 每个人只在自己的分支开发
- 每天写代码前先同步最新 `main`
- 每个人先把代码推到自己的功能分支
- 负责人本地验证通过后再合并到 `main`
- 本地总调试尽量基于最新 `main` 进行，只有需要提前联调时才单独拉队友分支

这样可以最大限度减少冲突，也更方便你统一调试和管理进度。
