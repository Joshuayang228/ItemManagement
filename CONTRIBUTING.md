# 🤝 贡献指南

感谢您对「记万物」项目的关注！我们欢迎任何形式的贡献，包括但不限于：

- 🐛 报告 Bug
- 💡 提出新功能建议
- 📝 改进文档
- 🔧 提交代码修复或新功能
- 🌐 翻译文档或应用界面

---

## 📋 行为准则

参与本项目时，请遵守以下基本准则：

- **尊重他人**：友善、专业地对待所有贡献者
- **建设性反馈**：提供具体、有建设性的意见
- **协作精神**：开放心态，愿意讨论和改进
- **遵守规范**：遵循项目的代码规范和提交规范

---

## 🐛 报告 Bug

### 提交 Bug 前的检查

1. **搜索已有 Issue**：确认问题是否已被报告
2. **使用最新版本**：确保在最新版本中问题仍然存在
3. **准备复现步骤**：确保问题可以稳定复现

### Bug 报告模板

在 [Issues](https://github.com/Joshuayang228/ItemManagement/issues/new) 页面创建新 Issue，请包含以下信息：

```markdown
**Bug 描述**
简短描述问题是什么

**复现步骤**
1. 打开 '...'
2. 点击 '...'
3. 滚动到 '...'
4. 看到错误

**预期行为**
描述您期望发生什么

**实际行为**
描述实际发生了什么

**截图**
如果适用，添加截图帮助解释问题

**环境信息**
- 设备：[例如 Samsung Galaxy S21]
- Android 版本：[例如 Android 12]
- 应用版本：[例如 v1.0.1]

**其他信息**
添加关于问题的其他上下文信息
```

---

## 💡 功能建议

### 提交建议前的思考

1. **明确需求**：清楚说明为什么需要这个功能
2. **考虑用户场景**：描述具体的使用场景
3. **评估可行性**：思考实现的可行性和复杂度

### 功能建议模板

```markdown
**功能描述**
简短描述您想要的功能

**使用场景**
描述这个功能解决什么问题，在什么情况下使用

**期望行为**
详细描述功能应该如何工作

**替代方案**
是否考虑过其他解决方案？

**补充说明**
添加任何其他相关信息、截图或示例
```

---

## 🔧 提交代码

### 开发环境准备

1. **Fork 仓库**
   ```bash
   # 在 GitHub 上 Fork 仓库到您的账号
   ```

2. **克隆到本地**
   ```bash
   git clone https://github.com/YOUR_USERNAME/ItemManagement.git
   cd ItemManagement
   ```

3. **添加上游仓库**
   ```bash
   git remote add upstream https://github.com/Joshuayang228/ItemManagement.git
   ```

4. **创建分支**
   ```bash
   git checkout -b feature/your-feature-name
   # 或
   git checkout -b fix/your-bug-fix
   ```

### 代码规范

#### Kotlin 编码规范

遵循 [Kotlin 官方编码规范](https://kotlinlang.org/docs/coding-conventions.html)

**命名约定：**
- 类名：`PascalCase`（如 `ItemRepository`）
- 函数名：`camelCase`（如 `getUserName`）
- 变量名：`camelCase`（如 `itemList`）
- 常量：`UPPER_SNAKE_CASE`（如 `MAX_ITEMS`）

**注释规范：**
```kotlin
/**
 * 获取用户的物品列表
 *
 * @param userId 用户 ID
 * @param category 分类名称，可为 null 表示所有分类
 * @return 物品列表
 */
fun getItemList(userId: Long, category: String?): List<Item> {
    // 实现代码
}
```

#### 布局文件规范

**XML 属性顺序：**
```xml
<TextView
    android:id="@+id/textView"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:layout_margin="16dp"
    android:padding="8dp"
    android:text="@string/title"
    android:textSize="16sp"
    android:textColor="?attr/colorOnSurface"
    app:layout_constraintTop_toTopOf="parent" />
```

顺序：`id` → `layout_*` → `padding/margin` → `content` → `style` → `app:*`

#### 字符串资源

所有用户可见的文字必须使用字符串资源：

```xml
<!-- strings.xml -->
<string name="app_name">记万物</string>
<string name="add_item_title">添加物品</string>
<string name="error_network">网络连接失败，请检查网络设置</string>
```

### 提交信息规范

使用语义化的提交信息：

```bash
# 格式
<type>(<scope>): <subject>

# 示例
feat(map): 添加地图选点功能
fix(detail): 修复图片加载失败的问题
docs(readme): 更新安装说明
style(ui): 优化底部导航栏样式
refactor(database): 重构数据库迁移逻辑
test(item): 添加物品添加功能的单元测试
chore(deps): 更新依赖版本
```

**Type 类型：**
- `feat`: 新功能
- `fix`: Bug 修复
- `docs`: 文档更新
- `style`: 代码格式调整（不影响功能）
- `refactor`: 代码重构
- `test`: 测试相关
- `chore`: 构建或辅助工具的变动
- `perf`: 性能优化

### 提交 Pull Request

1. **确保代码质量**
   ```bash
   # 运行 Lint 检查
   ./gradlew lint
   
   # 运行单元测试（如果有）
   ./gradlew test
   
   # 编译通过
   ./gradlew assembleDebug
   ```

2. **同步上游代码**
   ```bash
   git fetch upstream
   git rebase upstream/master
   ```

3. **推送到您的 Fork**
   ```bash
   git push origin feature/your-feature-name
   ```

4. **创建 Pull Request**
   - 访问您的 Fork 仓库页面
   - 点击 "New Pull Request"
   - 填写 PR 描述

### Pull Request 模板

```markdown
**变更类型**
- [ ] 🐛 Bug 修复
- [ ] ✨ 新功能
- [ ] 📝 文档更新
- [ ] 🎨 UI/样式改进
- [ ] ♻️ 代码重构
- [ ] ⚡ 性能优化

**变更说明**
简要描述这个 PR 做了什么

**相关 Issue**
关闭 #issue_number（如果适用）

**测试**
- [ ] 本地测试通过
- [ ] Lint 检查通过
- [ ] 在真机/模拟器上测试

**截图**（如果是 UI 变更）
变更前 | 变更后
--- | ---
![before](url) | ![after](url)

**检查清单**
- [ ] 代码遵循项目规范
- [ ] 添加了必要的注释
- [ ] 更新了相关文档
- [ ] 没有引入新的警告
- [ ] 测试覆盖了变更内容
```

---

## 📝 文档贡献

### 文档类型

- **README.md**：项目介绍和快速开始
- **发布指南.md**：版本发布流程
- **代码注释**：函数、类的文档注释
- **Wiki**（计划中）：详细的使用教程

### 文档规范

1. **使用清晰的标题层级**
2. **提供代码示例**
3. **添加截图辅助说明**
4. **保持更新**：代码变更时同步更新文档

---

## 🌐 翻译贡献

我们欢迎将应用翻译成其他语言！

### 翻译流程

1. **创建语言资源文件**
   ```
   app/src/main/res/
   ├── values/          # 默认（中文简体）
   ├── values-en/       # 英语
   ├── values-zh-rTW/   # 中文繁体
   └── values-ja/       # 日语
   ```

2. **翻译 strings.xml**
   - 保持 `name` 属性不变
   - 只翻译文本内容
   - 注意保留占位符（如 `%s`、`%d`）

3. **测试翻译**
   - 切换系统语言测试
   - 确保文字不会溢出界面

---

## 🏆 贡献者名单

感谢所有为本项目做出贡献的人！

<!-- 这里会自动生成贡献者列表 -->

<a href="https://github.com/Joshuayang228/ItemManagement/graphs/contributors">
  <img src="https://contrib.rocks/image?repo=Joshuayang228/ItemManagement" />
</a>

---

## 📞 联系我们

如有任何问题，欢迎通过以下方式联系：

- **GitHub Issues**：[提交 Issue](https://github.com/Joshuayang228/ItemManagement/issues)
- **Email**：通过 GitHub Profile 查看
- **Discussions**：[项目讨论区](https://github.com/Joshuayang228/ItemManagement/discussions)（计划中）

---

## 🎓 学习资源

### Android 开发
- [Android 官方文档](https://developer.android.com/docs)
- [Kotlin 官方文档](https://kotlinlang.org/docs/home.html)
- [Material Design 3](https://m3.material.io/)

### Git & GitHub
- [GitHub Flow](https://guides.github.com/introduction/flow/)
- [Git 教程](https://www.atlassian.com/git/tutorials)
- [如何参与开源项目](https://opensource.guide/zh-hans/how-to-contribute/)

---

## ❓ 常见问题

**Q: 我不会编程，能贡献吗？**
A: 当然！您可以报告 Bug、提出功能建议、改进文档、翻译或测试应用。

**Q: 我的 PR 会被接受吗？**
A: 只要符合项目规范且功能合理，我们会认真考虑。即使不被接受，也会给出原因和建议。

**Q: 需要签署贡献者协议吗？**
A: 不需要，本项目使用 MIT 开源协议，您的贡献自动遵循该协议。

**Q: 可以重构整个项目吗？**
A: 大规模重构请先创建 Issue 讨论，确保方向一致后再动手。

---

## 🙏 致谢

再次感谢您的贡献！每一个 Issue、PR、Star 都是对项目的支持！

**让我们一起让「记万物」变得更好！** 🚀

---

**最后更新：2025-11-15**

