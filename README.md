# FitPlan 健身计划

一款本地优先的 Android 健身计划与训练记录应用，使用 Kotlin 和 Android View 编写。

## 功能

- 自定义周一至周日的训练计划，包括训练部位、动作、组数、次数、爬坡参数和提醒时间。
- 手动添加、编辑或删除训练动作与训练日。
- 内置胸部、背部、肩部、臀腿、手臂五套模板，可一键添加或替换。
- 每个训练日独立的定时通知，修改计划后自动更新提醒。
- 首页显示当前日期、时间和本周打卡进度。
- 为每个力量动作录入实际重量（kg）；今天可直接记录，本周过去的训练日可补录。
- 训练历史按日期倒序完整展示，包括计划快照、实际重量、有氧安排和打卡状态。
- 本地保存计划、打卡和历史数据，无需账号，无需联网。

## 环境要求

- JDK 17
- Android SDK Platform 34
- Android SDK Build Tools 35.0.0
- 最低运行系统：Android 8.0（API 26）
- targetSdk：34

项目使用 Gradle wrapper，无需单独安装 Gradle。主要版本：AGP 8.9.1、Kotlin 2.0.21、Gradle 8.11.1。

## 构建

在项目根目录执行：

```bash
./gradlew :app:assembleDebug
```

Windows 使用：

```bat
gradlew.bat :app:assembleDebug
```

构建产物：

```text
app/build/outputs/apk/debug/app-debug.apk
```

如本机未自动识别 Java 或 Android SDK，请先设置 `JAVA_HOME` 和 `ANDROID_HOME`。

## 安装

连接已开启 USB 调试的手机后：

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

也可以把 APK 发送到手机，通过文件管理器安装。首次安装时需要允许对应来源的“安装未知应用”。

## 使用与权限

1. 打开 App 后可编辑现有计划，或通过“训练模板”恢复内置计划。
2. 打开“训练提醒”时，请允许系统通知权限。
3. 如果国产 Android 系统限制后台活动，建议在应用设置中将省电策略设为“无限制”，以保证提醒准时。
4. 在今天或本周已经过去训练日的卡片中录入各动作实际重量；完成后点击“完成打卡”或“补打卡”。
5. 未来训练日会显示“未开始”，不能提前打卡。点击“训练历史”可查看全部历史记录。

## 数据说明

- 训练计划、提醒开关、打卡和历史记录均保存在应用本地 SharedPreferences 中。
- 历史记录保存的是训练发生时的计划快照，后续修改计划不会回写旧历史。
- 当前版本不包含云同步；卸载应用会清除本地数据。

## 项目结构

```text
app/src/main/java/com/chenshanhan/fitplan/
├── MainActivity.kt              # 首页、日期时间与本周进度
├── EditPlanActivity.kt          # 自定义计划与动作录入
├── TemplateActivity.kt          # 内置模板
├── HistoryActivity.kt           # 训练历史
├── PlanRepository.kt            # 计划本地存储
├── TrainingLogRepository.kt     # 历史与实际重量存储
├── ReminderScheduler.kt         # 定时提醒调度
└── ReminderReceiver.kt          # 通知内容与每周重排
```
