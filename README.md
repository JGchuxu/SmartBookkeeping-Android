<div align="center">

# 📒 基于 Android 平台的智能记账系统

**自动捕捉消费通知 · 无感记账 · 零第三方图表库**

Java 17 · Android SDK 34 · SQLite · Material Design 3 · Canvas

</div>

---

## 📖 项目简介

一款面向个人用户的智能记账应用，围绕 **"降低记账门槛"** 这一核心目标设计。

传统的记账 App 需要用户在每次消费后手动打开应用、填写金额、选择分类，流程繁琐导致难以坚持。本项目的核心思路是：**利用 Android 系统的通知监听能力，在用户完成支付的瞬间自动捕捉消费信息并完成记账**——用户付完钱，账已经记好了，全程无需任何额外操作。

除自动记账外，还提供账单管理、多账户财务总览、消费统计图表、笔记待办、自然语言快捷记账等完整功能。

> 🎓 本项目为个人独立开发作品（同时作为毕业设计项目），从需求分析、架构设计到编码实现、打包发布全流程独立完成。

---

## ✨ 核心特性

### 🔔 智能通知识别（核心亮点）
- 基于 `NotificationListenerService` 实时监听微信、支付宝的消费通知
- 通过正则与关键词规则提取**金额、收支方向、退款标识**，自动写入账单
- 构建**广告词库**（促销/活动/电商大促/直播种草等 7 大类）过滤营销内容，避免垃圾信息污染账目
- 无法识别的通知（如分不清金额或收支）自动转入**待办列表**，供用户手动处理
- 三重去重策略（来源 + 金额 + 5 分钟时间窗口），避免同一笔消费重复入账

### 🔋 后台常驻保活
- 监听服务升级为**前台服务**，提升进程优先级
- `AlarmManager` 定时唤醒 + `NotificationListenerService.requestRebind()` 主动重绑
- 开机广播（`BOOT_COMPLETED`）自动重新注册，设备重启后无需手动开启

### 💰 多账户财务总览
- 支持为微信、支付宝、银行卡等渠道设置**独立初始金额**
- 自动核算各账户余额及日/月/年收支
- 基于规则引擎生成个性化**理财建议**

### 📊 数据可视化
- 基于 **Canvas 自绘**饼图与柱状图
- **零第三方图表库依赖**，减小包体积，可控性强

### 🧠 自然语言快捷记账
- 输入"昨天买咖啡 35 块"即可完成记账
- 自动解析**时间、金额、分类、渠道**，并对文本做清洗优化

### 🎨 主题换肤系统
- 10+ 套配色方案 + 10 款自定义字体全局切换
- 界面切换动画优化（纯平移动画，无闪白/黑屏）

### 📝 笔记与待办
- 笔记/待办双 Tab 管理，支持设置闹钟提醒
- 待办支持自然语言时间提取（"明天下午 3 点开会"）

### 📤 数据导出与备份
- 导出账单为 CSV 文件
- 完整数据库备份与恢复

---

## 🛠 技术栈

| 层次 | 技术选型 |
|------|---------|
| **开发语言** | Java 17（主体，46 个源文件约 5900 行）+ Kotlin（1 个文件，JNI 封装） |
| **平台** | Android SDK 34（compileSdk / targetSdk），minSdk 24（Android 7.0+） |
| **UI 框架** | Material Design 3、XML 布局、RecyclerView |
| **数据持久化** | 原生 SQLite（SQLiteOpenHelper），**手写全部 SQL，未使用 ORM** |
| **图表** | Canvas 自绘（Paint / Path / RectF） |
| **核心服务** | NotificationListenerService、Foreground Service、AlarmManager |
| **组件** | 9 个 Activity + 4 个 Fragment + 3 个 BroadcastReceiver |
| **构建** | Gradle 8.9 + AGP 8.5.2（纯命令行打包，未使用 Android Studio） |

### 📦 第三方依赖（仅 5 项）

```gradle
androidx.appcompat:appcompat:1.7.0                    // 兼容支持
com.google.android.material:material:1.11.0           // Material Design 3
androidx.recyclerview:recyclerview:1.3.2              // 列表
androidx.constraintlayout:constraintlayout:2.1.4      // 布局
dev.ffmpegkit-maintained:llama-android:0.1.1          // 本地 AI（已停止部署，依赖保留）
```

> 💡 核心功能（图表绘制、SQL 操作、规则引擎、通知解析）**全部自主实现**，未依赖任何 ORM、图表或 NLP 框架。

---

## 🚀 快速开始

### 环境要求

| 工具 | 版本 |
|------|------|
| JDK | 17+ |
| Android SDK | 34（含 build-tools 34.0.0、platform-tools） |
| Gradle | 8.9 |

### 编译步骤

1. **配置 SDK 路径** —— 在项目根目录创建 `local.properties`（该文件已被 Git 忽略）：

```properties
sdk.dir=你的Android SDK路径
```

2. **命令行构建**：

```bash
# Windows（PowerShell）
$env:JAVA_HOME = "你的JDK17路径"
$env:ANDROID_HOME = "你的Android SDK路径"
$env:ANDROID_SDK_ROOT = "你的Android SDK路径"
gradle assembleDebug

# 产物：app/build/outputs/apk/debug/app-debug.apk
```

> ⚠️ 项目路径请避免使用中文，AGP 对非 ASCII 路径支持不佳。

---

## 📱 使用说明

### 开启通知识别（核心功能）

1. 打开应用 → **设置** → **通知识别**
2. 点击授权，在系统「通知使用权」设置中勾选本应用
3. 之后微信/支付宝的消费/收款通知会被自动识别并入账

> 📌 微信需在「我 → 服务 → 钱包 → 账单」中开启支付提醒；支付宝默认开启。

### 后台保活设置（重要）

由于国产 ROM（MIUI / EMUI / ColorOS 等）的省电策略，需在系统设置中额外授权：

1. **自启动**：设置 → 应用管理 → 本应用 → 开启「自启动」
2. **电池优化**：设置为「不限制」或加入白名单
3. **后台锁定**：在多任务界面下拉锁定应用

---

## 📂 目录结构

```
app/src/main/
├── AndroidManifest.xml
├── java/com/example/jizhang/
│   ├── MainActivity / AddTransactionActivity / FinanceOverviewActivity ...
│   ├── fragment/    BillFragment / StatsFragment / NoteFragment / SettingsFragment
│   ├── adapter/     BillAdapter / ChannelAdapter / CategoryAdapter / NoteAdapter
│   ├── service/     BillNotificationListener / KeepAliveReceiver / BootReceiver
│   ├── view/        PieChartView / BarChartView
│   ├── db/          DatabaseHelper（6 张表，手写 SQL）
│   ├── model/       Transaction / Channel / Category / Note / StatItem
│   └── util/        BillParser / NaturalLangParser / ThemeManager / Palette / Exporter
└── res/
    ├── layout/ drawable/ values/ menu/ mipmap-*/
    └── font/  10 款免费商用字体
```

### 🗄 数据库设计（6 张表）

| 表名 | 用途 |
|------|------|
| `tbl_transaction` | 账单记录（金额、类型、时间、渠道、分类、备注） |
| `tbl_channel` | 支付渠道（微信、支付宝、银行卡等） |
| `tbl_category` | 收支分类 |
| `tbl_note` | 笔记与待办（`todo` 字段区分） |
| `tbl_pending` | 待确认账单 |
| `tbl_notify_log` | 通知识别日志（便于排查解析问题） |

---

## 🔄 版本历史

| 版本 | 主要更新 |
|------|---------|
| v1.10 | 新增 5 款字体（共 10 款）、界面切换动画优化（无闪白/黑屏） |
| v1.9.x | 修复金额精度问题、数组越界与类型转换崩溃 |
| v1.8 | 后台常驻保活（前台服务 + 保活闹钟 + 开机自启） |
| v1.7 | 自然语言 AI 化识别、账单列表精简、滚动与动画优化 |
| v1.6 | 内置 8 款免费商用字体 |
| v1.5 | 菲比主题插画、浅色化 UI 重做 |
| v1.3 | 笔记与待办模块 |
| v1.2 | 财务总览（多账户） |
| v1.1 | 通知识别（核心功能） |
| v1.0 | 基础记账 + 图表统计 + 导出备份 |

---

## 📄 开源协议

本项目基于 **MIT License** 开源，详见 [LICENSE](LICENSE) 文件。

> ⚠️ 项目内置字体均来自开源/免费商用字体（100font 等），其授权以各字体原始许可为准。

---

## 👤 作者

独立开发 · 2025.09 – 2026.09

如有问题或建议，欢迎提交 Issue。
