# FitPlan 健身计划

一款本地优先的 Android 健身计划与训练记录应用，使用 Kotlin 和 Android View 编写。当前版本：**1.4（versionCode 5）**。

## 页面与功能

### 今日训练

- 只显示当天已经安排的训练计划，不再罗列整周内容。
- 显示当前日期、星期和时间。
- 为每个力量动作录入实际重量（kg），完成后可直接打卡。
- 显示当天的提醒时间，并可统一开启或关闭训练提醒。

### 计划编辑

- 查看并管理周一至周日的训练计划。
- 手动新增、编辑或删除计划，自定义训练部位、动作、组数、次数、爬坡参数和提醒时间。
- “从模板导入”内嵌在本页，默认收起；展开后可一键添加或确认替换模板。
- 保存计划或导入模板后，对应训练日的提醒会自动更新。

### 训练历史

- 按日期倒序完整展示训练记录。
- 保留训练时的计划快照、实际重量、有氧安排和打卡状态。
- 显示累计打卡次数和训练记录数量。

## 内置模板

| 训练日 | 部位 | 提醒时间 |
| --- | --- | --- |
| 周一 | 胸部 | 18:30 |
| 周二 | 背部 | 12:45 |
| 周三 | 肩部 | 18:30 |
| 周四 | 臀腿 | 18:30 |
| 周五 | 手臂 | 18:30 |

模板保留最初设定的动作、组数、次数和爬坡参数，导入后仍可继续编辑。

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

也可以把 APK 发送到手机，通过文件管理器安装。首次安装时需要允许对应来源的“安装未知应用”。覆盖安装可保留原有本地数据。

## 使用与权限

1. 初次使用时，五套内置计划会作为当前计划显示；可在“计划编辑”中修改。
2. 在“今日训练”录入各动作实际重量，完成后点击“完成打卡”。
3. 打开“训练提醒”时，请允许系统通知权限。
4. 如果国产 Android 系统限制后台活动，建议在应用设置中将省电策略设为“无限制”，以保证提醒准时。
5. 点击底部“训练历史”可查看全部历史记录。

## 数据说明

- 训练计划、提醒开关、打卡和历史记录均保存在应用本地 SharedPreferences 中。
- 历史记录保存的是训练发生时的计划快照，后续修改计划不会回写旧历史。
- 当前版本不包含云同步；卸载应用会清除本地数据。

## 项目结构

```text
app/src/main/java/com/chenshanhan/fitplan/
├── MainActivity.kt              # 单 Activity 与三个底部导航页面
├── TodayFragment.kt             # 今日计划、日期时间、提醒开关
├── PlansFragment.kt             # 整周计划管理与内嵌模板导入
├── HistoryFragment.kt           # 训练历史页面
├── DayAdapter.kt                # 当日动作、重量与打卡
├── HistoryAdapter.kt            # 历史记录卡片
├── EditPlanActivity.kt          # 自定义计划与动作录入
├── PlanRepository.kt            # 计划本地存储
├── TrainingLogRepository.kt     # 历史与实际重量存储
├── ReminderScheduler.kt         # 定时提醒调度
└── ReminderReceiver.kt          # 通知内容与每周重排
```
