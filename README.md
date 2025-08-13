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
- 🎯 **完整类型显示**: 方法签名显示完整的包路径，包括返回类型和参数类型的完整限定名
- 📊 **方法信息展示**: 详细显示方法的返回类型、参数列表和完整签名信息
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

通过 `Tools` -> `Dubbo Invoke Settings` 打开配置对话框，可以自定义以下设置：

**连接配置**
- **Registry Address**: 注册中心地址 (默认: `zookeeper://127.0.0.1:2181`)
- **Application Name**: 客户端应用名称 (默认: `dubbo-invoke-client`)
- **Protocol**: 通信协议 (默认: `dubbo`)
- **Timeout (ms)**: 调用超时时间 (默认: `3000`)
- **Retries**: 失败重试次数 (默认: `0`)

**服务配置**
- **Version**: 服务版本号 (可选)
- **Group**: 服务分组 (可选)

**调用选项**
- **Use Generic Invocation**: 启用泛化调用模式
- **Show Detailed Command with Comments**: 保留配置项（当前版本暂不生效）
- **Generate Example Parameter Values**: 自动生成示例参数值

## 生成的命令示例

### 直接调用模式
```
invoke com.example.UserService.getUserById(1L)
```

### 泛化调用模式
```
invoke com.example.UserService.$invoke("getUserById", new String[]{"java.lang.Long"}, new Object[]{1L})
```

### 复杂参数调用示例
```
// 带复杂对象参数的调用
invoke com.example.UserService.createUser({"class":"com.example.dto.UserRequest","name":"张三","age":25})

// 带List参数的调用
invoke com.example.OrderService.batchProcess([{"class":"com.example.dto.OrderItem","id":1},{"class":"com.example.dto.OrderItem","id":2}])
```

### 完整类型信息展示
插件会在对话框中显示完整的方法信息：
```
方法名称: getUserById
返回类型: com.example.dto.User
参数列表: 
  - request: com.example.dto.UserQueryRequest
  - userId: java.lang.Long
  - options: java.util.List<java.lang.String>
方法全路径: com.example.dto.User getUserById(com.example.dto.UserQueryRequest request, java.lang.Long userId, java.util.List<java.lang.String> options)
```

## 支持的参数类型

插件会自动为不同类型的参数生成示例值：

- **基本类型**: `int` -> `0`, `boolean` -> `false`, `double` -> `0.0`
- **包装类型**: `Integer` -> `0`, `Boolean` -> `false`, `Double` -> `0.0`
- **字符串**: `String` -> `"example"`
- **日期**: `Date` -> `new Date()`
- **集合**: `List` -> `new ArrayList<>()`, `Map` -> `new HashMap<>()`
- **自定义对象**: `User` -> `new User()`

## 完整类型显示功能

插件现在支持显示完整的类型信息，包括：

### 方法签名完整显示
- **返回类型**: 显示完整的包路径，如 `com.jzt.zhcai.common.dto.Result<com.jzt.zhcai.user.front.userbasic.dto.CompanyInfoDetailDTO>`
- **参数类型**: 显示完整的包路径，如 `com.jzt.zhcai.user.front.userbasic.dto.CompanyInfoDetailQry`
- **泛型支持**: 完整显示泛型类型信息，包括嵌套泛型

### 对话框信息展示
在生成命令的对话框中，会显示：
1. **方法名称**: 方法的简单名称
2. **返回类型**: 完整的返回类型包路径
3. **参数列表**: 每个参数的名称和完整类型
4. **方法全路径**: 完整的方法签名，包含所有类型的完整包路径

这个功能特别适用于：
- 复杂的企业级项目，需要明确区分不同包下的同名类
- 泛型方法的调用，需要准确的类型信息
- 代码审查和文档生成

## 快捷键

- `Ctrl+Alt+D`: 打开Dubbo命令生成对话框
- `Ctrl+Alt+I`: 快速生成并复制命令到剪切板

## 开发环境

- IntelliJ IDEA 2023.1+
- Java 17+
- Gradle 8.5+
- Kotlin DSL

## 构建插件

```bash
# 克隆项目
git clone <repository-url>
cd dubbo-invoke

# 构建插件
./gradlew build

# 构建插件分发包
./gradlew buildPlugin

# 运行测试
./gradlew test

# 在IDE中运行插件
./gradlew runIde

# 验证插件
./gradlew verifyPlugin
```

## 贡献

欢迎提交Issue和Pull Request来改进这个插件！

## 许可证

MIT License
