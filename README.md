# 记万物 - 智能物品管理系统

<div align="center">

![记万物 Logo](app/src/main/ic_launcher-playstore.png)

**一款优雅、强大的 Android 物品管理应用**

[![Latest Release](https://img.shields.io/badge/release-v1.0.1-blue.svg)](https://github.com/Joshuayang228/ItemManagement/releases)
[![Platform](https://img.shields.io/badge/platform-Android-green.svg)](https://www.android.com)
[![Min API](https://img.shields.io/badge/API-24%2B-orange.svg)](https://android-arsenal.com/api?level=24)
[![License](https://img.shields.io/badge/license-MIT-red.svg)](LICENSE)

[📥 下载 APK](https://github.com/Joshuayang228/ItemManagement/releases) · [📖 使用指南](#功能特性) · [🐛 反馈问题](https://github.com/Joshuayang228/ItemManagement/issues)

</div>

---

## 📱 应用简介

**记万物**是一款功能强大的 Android 物品管理应用，帮助您轻松管理家中、办公室或仓库中的所有物品。支持分类管理、过期提醒、购物清单、地图定位等功能，让物品管理变得简单高效。

### ✨ 核心特性

- 🗂️ **智能分类管理** - 多层级分类，支持自定义字段和标签
- 📋 **模板系统** - 快速添加常用物品，预设字段和默认值
- 🗺️ **地图集成** - 精准记录物品存放位置，支持地图选点和导航
- 📸 **图片管理** - 拍照或从相册添加多张图片，支持全屏查看
- ⏰ **智能提醒** - 保质期、保修期到期自动提醒
- 🛒 **购物清单** - 管理待购物品，支持预算控制和优先级
- 📊 **数据统计** - 库存分析、消费报告、浪费统计
- 🗓️ **日历视图** - 可视化查看物品时间线
- ♻️ **回收站** - 误删除物品可恢复
- 🌙 **深色模式** - 完整支持系统深色主题
- 🔄 **自动更新** - 从 GitHub Release 自动检测并下载更新

---

## 🎬 功能演示

### 物品管理
- 添加/编辑物品，支持20+字段（名称、分类、数量、价格、保质期等）
- 多图片上传，全屏查看和缩放
- 地图选点，记录物品精确位置
- 批量操作（删除、导出）

### 智能模板
- 创建物品模板，预设常用字段
- 为字段设置默认值（分类、标签、单位等）
- 快速应用模板，提升录入效率

### 地图功能
- 集成高德地图，支持POI搜索
- 地图选点：拖拽标记或点击地图选择位置
- 地图查看：查看物品位置，一键导航
- 智能定位：轻点按钮获取当前位置

### 提醒与通知
- 保质期到期提醒（提前7天、3天、1天）
- 保修期到期提醒
- 自定义提醒时间

### 购物清单
- 管理待购物品，设置预算和优先级
- 从库存快速添加到购物清单
- 购买后自动转入库存

### 数据分析
- 库存统计（按分类、标签、地点）
- 消费分析（按时间、分类）
- 浪费报告（过期物品统计）

---

## 🚀 快速开始

### 下载安装

#### 方式一：直接下载 APK（推荐）
1. 访问 [Releases 页面](https://github.com/Joshuayang228/ItemManagement/releases)
2. 下载最新版本的 `jiwanwu-v1.0.1.apk`
3. 安装到您的 Android 设备

#### 方式二：从源码编译
```bash
# 克隆仓库
git clone https://github.com/Joshuayang228/ItemManagement.git
cd ItemManagement

# 使用 Android Studio 打开项目
# 或使用 Gradle 命令行编译
./gradlew assembleRelease

# APK 输出路径：app/release/app-release.apk
```

### 系统要求

- **最低 Android 版本**：Android 7.0 (API 24)
- **推荐 Android 版本**：Android 10 (API 29) 及以上
- **存储空间**：约 20 MB
- **网络权限**：地图和自动更新功能需要网络连接

### 权限说明

| 权限 | 用途 |
|------|------|
| 📷 相机 | 拍照添加物品图片 |
| 🖼️ 存储 | 读取和保存图片 |
| 📍 位置 | 地图定位和位置记录 |
| 🌐 网络 | 地图数据加载和自动更新 |
| 🔔 通知 | 过期提醒推送 |

---

## 🛠️ 技术栈

### 开发语言与框架
- **Kotlin** - 100% Kotlin 代码
- **Android SDK** - Target API 34
- **Material Design 3** - 现代化 UI 设计

### 核心架构
- **MVVM 架构** - ViewModel + LiveData/StateFlow
- **Jetpack 组件**
  - Room - 本地数据库
  - Navigation - 导航管理
  - DataBinding - 数据绑定
  - WorkManager - 后台任务
  - Lifecycle - 生命周期管理

### 第三方库
- **高德地图 SDK** (9.7.0) - 地图和定位功能
- **Glide** (4.16.0) - 图片加载和缓存
- **MPAndroidChart** (3.1.0) - 数据可视化图表
- **Gson** - JSON 解析
- **Coroutines** - 协程异步处理

### 开发工具
- **Android Studio** - Koala | 2024.1.1
- **Gradle** - 8.11.1
- **Kotlin** - 1.9.0
- **KSP** - 符号处理器（替代 KAPT）

---

## 📂 项目结构

```
ItemManagement/
├── app/                          # 主应用模块
│   ├── src/main/
│   │   ├── java/com/example/itemmanagement/
│   │   │   ├── data/             # 数据层
│   │   │   │   ├── dao/          # Room DAO
│   │   │   │   ├── entity/       # 数据实体
│   │   │   │   ├── model/        # 数据模型
│   │   │   │   └── repository/   # 数据仓库
│   │   │   ├── ui/               # UI 层
│   │   │   │   ├── home/         # 主页
│   │   │   │   ├── add/          # 添加物品
│   │   │   │   ├── detail/       # 物品详情
│   │   │   │   ├── map/          # 地图相关
│   │   │   │   ├── template/     # 模板管理
│   │   │   │   ├── shopping/     # 购物清单
│   │   │   │   └── ...           # 其他功能模块
│   │   │   ├── util/             # 工具类
│   │   │   ├── utils/            # 辅助工具
│   │   │   └── MainActivity.kt   # 主 Activity
│   │   └── res/                  # 资源文件
│   │       ├── layout/           # 布局文件
│   │       ├── drawable/         # 图标和图片
│   │       └── values/           # 字符串、颜色、样式等
│   ├── schemas/                  # Room 数据库 schema
│   └── build.gradle.kts          # 应用级构建配置
├── charts/                       # 图表库模块
├── keystore/                     # 签名密钥（不提交到 Git）
│   └── release-key.jks
├── version.json                  # 自动更新配置
├── gradle.properties             # Gradle 配置
├── settings.gradle.kts           # 项目设置
└── README.md                     # 项目说明文档
```

---

## 🔧 开发指南

### 环境配置

1. **安装 Android Studio**
   - 下载并安装 Android Studio Koala (2024.1.1) 或更高版本
   - 配置 Android SDK (API 24-34)

2. **克隆项目**
   ```bash
   git clone https://github.com/Joshuayang228/ItemManagement.git
   cd ItemManagement
   ```

3. **配置高德地图 API Key**
   - 访问 [高德开放平台](https://lbs.amap.com/) 申请 API Key
   - 在 `app/build.gradle.kts` 中替换 `AMAP_KEY`
   ```kotlin
   manifestPlaceholders["AMAP_KEY"] = "YOUR_AMAP_KEY_HERE"
   ```

4. **运行项目**
   ```bash
   ./gradlew assembleDebug
   # 或直接在 Android Studio 中运行
   ```

### 编译打包

#### Debug 版本
```bash
./gradlew assembleDebug
# 输出: app/build/outputs/apk/debug/app-debug.apk
```

#### Release 版本
```bash
./gradlew assembleRelease
# 输出: app/release/app-release.apk
```

> **注意**：Release 版本需要配置签名密钥，详见 [发布指南.md](发布指南.md)

### 数据库迁移

项目使用 Room 数据库，当前版本：**49**

数据库 schema 文件存储在 `app/schemas/` 目录，每次数据库结构变更需要：
1. 更新 `AppDatabase.kt` 中的 `version` 号
2. 添加 `Migration` 类
3. 运行测试确保迁移成功

---

## 📝 版本历史

### v1.0.1 (2025-11-15) - 重大功能更新

**新功能**
- 🗺️ 集成高德地图，支持地图选点和位置查看
- 📋 智能模板系统，支持字段默认值
- 📐 智能单位默认（数量→个、价格→元、期限→月）

**优化**
- 🎨 底部导航切换动画优化
- 📍 地点字段交互改进（轻点定位、长按地图）
- 🔢 允许物品数量为 0

**修复**
- 🐛 修复地图页面底部导航闪烁
- 🐛 修复地图缩放按钮被遮挡

### v1.0.0 (2025-10-01) - 首次发布

- 🎉 记万物正式发布
- ✅ 完整的物品管理功能
- ✅ 购物清单和提醒功能
- ✅ 数据统计和分析

[查看完整更新日志](https://github.com/Joshuayang228/ItemManagement/releases)

---

## 🤝 贡献指南

欢迎提交 Issue 和 Pull Request！

### 提交 Issue
- 🐛 Bug 报告：请详细描述复现步骤、预期行为和实际行为
- 💡 功能建议：说明功能需求和使用场景
- 📖 文档改进：指出文档中的错误或不清楚的地方

### 提交 Pull Request
1. Fork 本仓库
2. 创建您的特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交您的更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 开启一个 Pull Request

### 代码规范
- 遵循 Kotlin 官方编码规范
- 使用有意义的变量和函数名（中文注释可接受）
- 添加必要的注释和文档
- 确保代码通过 Lint 检查

---

## 📄 开源协议

本项目采用 [MIT License](LICENSE) 开源协议。

```
MIT License

Copyright (c) 2025 Joshua Yang

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

---

## 📮 联系方式

- **作者**：Joshua Yang
- **GitHub**：[@Joshuayang228](https://github.com/Joshuayang228)
- **项目地址**：[ItemManagement](https://github.com/Joshuayang228/ItemManagement)
- **问题反馈**：[Issues](https://github.com/Joshuayang228/ItemManagement/issues)

---

## 🙏 致谢

感谢以下开源项目和服务：

- [Android Jetpack](https://developer.android.com/jetpack) - Google 官方组件库
- [Material Design 3](https://m3.material.io/) - 现代化 UI 设计规范
- [高德地图](https://lbs.amap.com/) - 地图和定位服务
- [Glide](https://github.com/bumptech/glide) - 高效的图片加载库
- [MPAndroidChart](https://github.com/PhilJay/MPAndroidChart) - 强大的图表库
- [GitHub](https://github.com/) - 代码托管和自动更新服务

---

## 📸 应用截图

### 主界面
![主界面](screenshots/home.png)

### 物品详情
![物品详情](screenshots/detail.png)

### 地图选点
![地图选点](screenshots/map.png)

### 购物清单
![购物清单](screenshots/shopping.png)

### 数据分析
![数据分析](screenshots/analysis.png)

---

## ⭐ Star History

如果这个项目对您有帮助，请给我一个 ⭐ Star！

[![Star History Chart](https://api.star-history.com/svg?repos=Joshuayang228/ItemManagement&type=Date)](https://star-history.com/#Joshuayang228/ItemManagement&Date)

---

<div align="center">

**用心管理，轻松生活 📦✨**

Made with ❤️ by Joshua Yang

</div>

