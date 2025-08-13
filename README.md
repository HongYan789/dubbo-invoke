# Dubbo Invoke Plugin for IntelliJ IDEA

<!-- Plugin description -->
一个用于IntelliJ IDEA的Dubbo调用命令生成插件，可以快速为Java方法生成Dubbo invoke命令。

## 功能特性

- 🚀 **快速生成**: 在Java方法上右键或使用快捷键快速生成Dubbo invoke命令
- 📋 **一键复制**: 生成的命令可以一键复制到剪切板
- ⚙️ **灵活配置**: 支持配置注册中心地址、应用名称、超时时间等参数
- 🔧 **泛化调用**: 支持生成泛化调用和直接调用两种模式
- 💡 **智能参数**: 自动生成示例参数值，支持基本类型和复杂对象
- 📝 **详细注释**: 可选择生成带注释的详细命令
<!-- Plugin description end -->

## 安装方法

1. 下载插件jar包
2. 在IntelliJ IDEA中打开 `File` -> `Settings` -> `Plugins`
3. 点击齿轮图标，选择 `Install Plugin from Disk...`
4. 选择下载的jar包进行安装
5. 重启IDE

## 使用方法

### 1. 生成Dubbo命令

**方法一：右键菜单**
- 将光标放在Java方法上
- 右键选择 `Generate Dubbo Invoke Command`
- 在弹出的对话框中查看和复制命令

**方法二：快捷键**
- 将光标放在Java方法上
- 按 `Ctrl+Alt+D` 打开生成对话框
- 按 `Ctrl+Alt+I` 快速复制命令到剪切板

### 2. 配置插件

- 打开 `Tools` -> `Dubbo Invoke Settings`
- 配置以下参数：
  - **Registry Address**: 注册中心地址 (默认: zookeeper://127.0.0.1:2181)
  - **Application Name**: 应用名称 (默认: dubbo-invoke-client)
  - **Timeout**: 超时时间，毫秒 (默认: 3000)
  - **Retries**: 重试次数 (默认: 0)
  - **Protocol**: 协议 (默认: dubbo)
  - **Version**: 服务版本
  - **Group**: 服务分组
  - **Use Generic Invocation**: 是否使用泛化调用
  - **Show Detailed Command**: 是否显示详细命令（带注释）
  - **Generate Example Values**: 是否生成示例参数值

## 生成的命令示例

### 直接调用模式
```
invoke com.example.UserService.getUserById(1L)
```

### 泛化调用模式
```
invoke com.example.UserService.$invoke("getUserById", new String[]{"java.lang.Long"}, new Object[]{1L})
```

### 带注释的详细命令
```
# Dubbo invoke command for method: com.example.UserService.getUserById
# Method signature: User getUserById(Long id)
# Registry: zookeeper://127.0.0.1:2181
# Application: dubbo-invoke-client

invoke com.example.UserService.getUserById(1L)
```

## 支持的参数类型

插件会自动为不同类型的参数生成示例值：

- **基本类型**: `int` -> `0`, `boolean` -> `false`, `double` -> `0.0`
- **包装类型**: `Integer` -> `0`, `Boolean` -> `false`, `Double` -> `0.0`
- **字符串**: `String` -> `"example"`
- **日期**: `Date` -> `new Date()`
- **集合**: `List` -> `new ArrayList<>()`, `Map` -> `new HashMap<>()`
- **自定义对象**: `User` -> `new User()`

## 快捷键

- `Ctrl+Alt+D`: 打开Dubbo命令生成对话框
- `Ctrl+Alt+I`: 快速生成并复制命令到剪切板

## 开发环境

- IntelliJ IDEA 2023.1+
- Java 17+
- Gradle 8.0+

## 构建插件

```bash
# 克隆项目
git clone <repository-url>
cd dubbo-invoke

# 构建插件
./gradlew buildPlugin

# 运行测试
./gradlew test

# 在IDE中运行插件
./gradlew runIde
```

## 贡献

欢迎提交Issue和Pull Request来改进这个插件！

## 许可证

MIT License