# 双向联动功能测试

## 测试场景

### 1. 默认填充测试
- 打开对话框时，Generated Dubbo Command中的参数应该自动解析并填充到Parameters面板
- 例如：`invoke("methodName", "paramTypes", ["value1", "value2"])` 应该将 value1, value2 填充到对应的参数输入框

### 2. 参数面板到命令文本的联动
- 在Parameters面板中修改参数值
- Generated Dubbo Command应该实时更新显示新的参数值

### 3. 命令文本到参数面板的联动
- 在Generated Dubbo Command中手动修改参数
- Parameters面板应该实时更新显示新的参数值

## 当前问题分析

根据代码检查，发现以下问题：

1. `extractParametersFromCommand` 方法只是简单地按逗号分割，没有正确解析JSON数组
2. `updateParameterInputsFromJson` 方法可能没有正确处理参数映射
3. 初始化时机可能不正确，UI组件可能还没有完全创建

## 需要修复的关键点

1. 改进参数解析逻辑
2. 确保参数输入组件的映射正确
3. 添加更多调试信息来跟踪问题
4. 验证DocumentListener是否正确触发