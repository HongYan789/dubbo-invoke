# Dubbo Invoke Plugin 测试指南

## 功能概述

Dubbo Invoke 是一个 IntelliJ IDEA 插件，用于在 IDE 中直接调用 Dubbo 服务。支持：

- 🚀 **真实调用**：支持真实的 Dubbo 服务调用，不仅仅是命令生成
- 🔗 **多注册中心**：支持 Nacos 和 Zookeeper 注册中心
- 🎯 **泛化调用**：支持泛化调用和直接调用两种模式
- 💡 **智能参数**：自动为基础类型和复杂对象生成示例参数值
- 🎯 **完整类型显示**：方法签名显示完整包路径，包括返回类型和参数类型
- 📊 **方法信息**：详细显示方法返回类型、参数列表和完整签名信息

## 🔧 最新更新 (v1.0.0)

- ✅ **依赖打包修复**：解决了 `NoClassDefFoundError: org/apache/dubbo/config/ApplicationConfig` 错误
- 📦 **Fat JAR 打包**：所有第三方依赖（Dubbo、Nacos、Zookeeper等）已正确打包到插件中
- 🎯 **插件大小**：75MB，包含完整的 Dubbo 生态系统依赖
- 🚀 **即装即用**：无需额外配置，安装后即可使用
- ✅ **初始化错误修复**：解决了 `ExceptionInInitializerError` 错误，采用延迟初始化策略
- 🔧 **组件优化**：修复了DubboClientManager和DubboInvokeService的过早初始化问题

## 安装插件

1. 插件包位置：`build/distributions/dubbo-invoke-1.0.0.zip`
2. 在IntelliJ IDEA中：
   - 打开 `File` -> `Settings` -> `Plugins`
   - 点击齿轮图标 -> `Install Plugin from Disk...`
   - 选择 `dubbo-invoke-1.0.0.zip` 文件
   - 重启IDE

## 测试步骤

### 1. 准备测试环境

确保你有：
- 运行中的Dubbo服务
- 可访问的注册中心（Nacos或Zookeeper）
- 包含Dubbo接口定义的Java项目

### 2. 配置Dubbo连接

在IDE中打开 `File` -> `Settings` -> `Tools` -> `Dubbo Invoke`：

- **注册中心地址**：如 `nacos://127.0.0.1:8848` 或 `zookeeper://127.0.0.1:2181`
- **应用名称**：你的应用名
- **超时时间**：调用超时时间（毫秒）
- **服务地址**：直连地址（可选）
- **服务端口**：服务端口（默认20880）

### 3. 使用插件调用服务

1. **打开Java接口文件**
   - 找到包含Dubbo服务接口的Java文件
   - 定位到要调用的方法

2. **启动调用对话框**
   - 右键点击方法名
   - 选择 `Dubbo Invoke` 菜单项
   - 或使用快捷键 `Ctrl+Shift+D` (Windows/Linux) 或 `Cmd+Shift+D` (Mac)

3. **配置调用参数**
   - 在弹出的对话框中查看方法信息
   - 在右侧参数编辑区域输入JSON格式的参数
   - 例如：`["hello", 123, true]`

4. **执行调用**
   - 点击 `Invoke` 按钮
   - 观察状态栏显示调用进度
   - 查看底部结果区域的返回值

### 4. 验证功能点

#### ✅ UI布局测试
- [ ] 对话框正确显示（900x700像素）
- [ ] 三个面板正确布局：方法信息、命令/参数、结果
- [ ] 参数编辑区域可以输入和编辑JSON
- [ ] 状态栏显示当前状态
- [ ] 进度条在调用时正确显示

#### ✅ 方法解析测试
- [ ] 正确显示接口名称
- [ ] 正确显示方法名称
- [ ] 正确显示参数类型
- [ ] 正确显示返回值类型

#### ✅ 服务调用测试
- [ ] 成功连接到注册中心
- [ ] 正确发现服务提供者
- [ ] 参数正确传递
- [ ] 返回结果正确显示
- [ ] 错误信息正确处理

#### ✅ 状态监控测试
- [ ] 调用前状态显示"Ready"
- [ ] 调用中状态显示"Invoking..."
- [ ] 成功后状态显示"Invoke completed successfully"
- [ ] 失败后状态显示错误信息
- [ ] 进度条正确显示和隐藏

## 常见问题排查

### 连接问题
- 检查注册中心地址是否正确
- 确认网络连通性
- 验证服务是否已注册

### 调用失败
- 检查参数格式是否正确
- 确认方法签名匹配
- 查看详细错误信息

### UI问题
- 重启IDE
- 检查插件是否正确安装
- 查看IDE日志文件

## 测试用例示例

```java
// 示例接口
public interface UserService {
    String getUserName(Long userId);
    User getUserInfo(Long userId, String type);
    boolean updateUser(User user);
}
```

**测试参数：**
- `getUserName`: `[12345]`
- `getUserInfo`: `[12345, "detail"]`
- `updateUser`: `[{"id": 12345, "name": "张三", "age": 25}]`

## 完成测试

当所有功能点测试通过后，插件就可以正常使用了！

---

**注意**：这是一个开发版本，如遇到问题请查看IDE的日志文件获取详细错误信息。