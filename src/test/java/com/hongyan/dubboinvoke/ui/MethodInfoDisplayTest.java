package com.hongyan.dubboinvoke.ui;

import java.util.Arrays;
import java.util.List;

/**
 * Method Information显示测试（简化版，不依赖IntelliJ PSI API）
 */
public class MethodInfoDisplayTest {
    
    // 简化的参数信息类
    public static class SimpleParameterInfo {
        private final String name;
        private final String type;
        private final String exampleValue;
        
        public SimpleParameterInfo(String name, String type, String exampleValue) {
            this.name = name;
            this.type = type;
            this.exampleValue = exampleValue;
        }
        
        public String getName() { return name; }
        public String getType() { return type; }
        public String getExampleValue() { return exampleValue; }
    }
    
    // 简化的方法信息类
    public static class SimpleMethodInfo {
        private final String className;
        private final String methodName;
        private final List<SimpleParameterInfo> parameters;
        private final String returnType;
        
        public SimpleMethodInfo(String className, String methodName, 
                              List<SimpleParameterInfo> parameters, String returnType) {
            this.className = className;
            this.methodName = methodName;
            this.parameters = parameters;
            this.returnType = returnType;
        }
        
        public String getClassName() { return className; }
        public String getMethodName() { return methodName; }
        public List<SimpleParameterInfo> getParameters() { return parameters; }
        public String getReturnType() { return returnType; }
    }
    
    public static void main(String[] args) {
        System.out.println("=== Method Information 显示测试 ===");
        
        // 测试1: 有参数的方法
        testMethodWithParameters();
        
        System.out.println("\n" + "=".repeat(50) + "\n");
        
        // 测试2: 无参数的方法
        testMethodWithoutParameters();
        
        System.out.println("\n=== 测试完成 ===");
    }
    
    /**
     * 测试有参数的方法信息显示
     */
    private static void testMethodWithParameters() {
        System.out.println("测试1: 有参数的方法");
        
        // 创建参数信息
        List<SimpleParameterInfo> parameters = Arrays.asList(
            new SimpleParameterInfo("request", 
                "com.jzt.zhcai.user.front.userbasic.dto.CompanyInfoDetailQry", "{}"),
            new SimpleParameterInfo("userId", 
                "java.lang.Long", "1L"),
            new SimpleParameterInfo("options", 
                "java.util.List<java.lang.String>", "[]")
        );
        
        // 创建方法信息
        SimpleMethodInfo methodInfo = new SimpleMethodInfo(
            "com.jzt.zhcai.user.front.companyinfo.CompanyInfoDubboApi",
            "queryCompanyInfoDetail",
            parameters,
            "com.jzt.zhcai.common.dto.Result<com.jzt.zhcai.user.front.userbasic.dto.CompanyInfoDetailDTO>"
        );
        
        // 模拟生成详细方法信息
        String methodInfoText = generateDetailedMethodInfo(methodInfo);
        System.out.println(methodInfoText);
    }
    
    /**
     * 测试无参数的方法信息显示
     */
    private static void testMethodWithoutParameters() {
        System.out.println("测试2: 无参数的方法");
        
        // 创建方法信息（无参数）
        SimpleMethodInfo methodInfo = new SimpleMethodInfo(
            "com.jzt.zhcai.system.api.SystemConfigApi",
            "getSystemInfo",
            Arrays.asList(), // 空参数列表
            "com.jzt.zhcai.system.dto.SystemInfoDTO"
        );
        
        // 模拟生成详细方法信息
        String methodInfoText = generateDetailedMethodInfo(methodInfo);
        System.out.println(methodInfoText);
    }
    
    /**
     * 生成详细的方法信息（模拟DubboInvokeDialog中的方法）
     */
    private static String generateDetailedMethodInfo(SimpleMethodInfo methodInfo) {
        StringBuilder info = new StringBuilder();
        
        // 类名
        info.append("类名 (Class): ").append(methodInfo.getClassName()).append("\n");
        
        // 方法名
        info.append("方法名 (Method): ").append(methodInfo.getMethodName()).append("\n");
        
        // 返回类型
        info.append("返回类型 (Return Type): ").append(methodInfo.getReturnType()).append("\n");
        
        // 方法全路径 (使用完整类型名称的Signature)
        String signature = generateFullMethodSignature(methodInfo);
        info.append("方法全路径 (Full Path): ").append(signature).append("\n");
        
        // 参数信息
        info.append("参数 (Parameters): ");
        if (methodInfo.getParameters().isEmpty()) {
            info.append("无参数");
        } else {
            info.append("\n");
            for (int i = 0; i < methodInfo.getParameters().size(); i++) {
                SimpleParameterInfo param = methodInfo.getParameters().get(i);
                info.append("  ").append(i + 1).append(". ")
                    .append(param.getType()).append(" ").append(param.getName()).append("\n");
            }
            // 移除最后一个换行符
            info.setLength(info.length() - 1);
        }
        
        return info.toString();
    }
    
    /**
     * 生成包含完整类型名称的方法签名
     */
    private static String generateFullMethodSignature(SimpleMethodInfo methodInfo) {
        StringBuilder signature = new StringBuilder();
        
        // 返回类型（完整类型名称）
        String returnType = methodInfo.getReturnType();
        signature.append(returnType).append(" ");
        
        // 方法名
        signature.append(methodInfo.getMethodName()).append("(");
        
        // 参数（完整类型名称）
        List<SimpleParameterInfo> parameters = methodInfo.getParameters();
        if (!parameters.isEmpty()) {
            String paramString = parameters.stream()
                    .map(param -> param.getType() + " " + param.getName())
                    .collect(java.util.stream.Collectors.joining(", "));
            signature.append(paramString);
        }
        
        signature.append(")");
        
        return signature.toString();
    }
    

}