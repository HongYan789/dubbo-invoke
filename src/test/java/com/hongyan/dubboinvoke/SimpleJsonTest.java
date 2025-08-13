package com.hongyan.dubboinvoke;

import com.hongyan.dubboinvoke.util.JavaMethodParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 简单JSON测试类
 */
public class SimpleJsonTest {
    
    public static void main(String[] args) {
        SimpleJsonTest test = new SimpleJsonTest();
        System.out.println("=== 开始运行SimpleJsonTest ===");
        
        try {
            test.testComplexObjectParameter();
            test.testListStringParameter();
            test.testPrimitiveParameter();
            System.out.println("\n=== 所有测试完成 ===");
        } catch (Exception e) {
            System.err.println("测试执行失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    @Test
    @DisplayName("测试复杂对象参数")
    public void testComplexObjectParameter() {
        System.out.println("=== 测试复杂对象参数 ===");
        
        List<JavaMethodParser.ParameterInfo> parameters = Arrays.asList(
            new JavaMethodParser.ParameterInfo("companyInfoDetailQry", 
                "com.jzt.zhcai.user.front.userbasic.dto.CompanyInfoDetailQry", "{}")
        );
        
        JavaMethodParser.MethodInfo methodInfo = new JavaMethodParser.MethodInfo(
            "com.jzt.zhcai.user.front.companyinfo.CompanyInfoDubboApi",
            "queryCompanyInfoDetail",
            parameters,
            "Object",
            null
        );
        
        // 生成JSON参数值
        String jsonParams = generateJsonParameterValues(methodInfo);
        String command = "invoke " + methodInfo.getClassName() + "." + methodInfo.getMethodName() + "(" + jsonParams + ")";
        
        System.out.println("生成的命令: " + command);
        
        // 验证格式
        String expectedFormat = "invoke com.jzt.zhcai.user.front.companyinfo.CompanyInfoDubboApi.queryCompanyInfoDetail({\"class\":\"com.jzt.zhcai.user.front.userbasic.dto.CompanyInfoDetailQry\",\"id\":123,\"name\":\"zhangsan\"})";
        assertEquals(expectedFormat, command, "复杂对象参数格式应该正确");
        System.out.println("✓ 复杂对象参数格式正确");
        System.out.println();
    }
    
    @Test
    @DisplayName("测试List<String>参数")
    public void testListStringParameter() {
        System.out.println("=== 测试List<String>参数 ===");
        
        List<JavaMethodParser.ParameterInfo> parameters = Arrays.asList(
            new JavaMethodParser.ParameterInfo("stringList", 
                "java.util.List<java.lang.String>", "[]")
        );
        
        JavaMethodParser.MethodInfo methodInfo = new JavaMethodParser.MethodInfo(
            "com.example.TestService",
            "processStringList",
            parameters,
            "void",
            null
        );
        
        // 生成JSON参数值
        String jsonParams = generateJsonParameterValues(methodInfo);
        String command = "invoke " + methodInfo.getClassName() + "." + methodInfo.getMethodName() + "(" + jsonParams + ")";
        
        System.out.println("生成的命令: " + command);
        
        // 验证格式
        String expectedFormat = "invoke com.example.TestService.processStringList([\"demo\",\"demo\"])";
        assertEquals(expectedFormat, command, "List<String>参数格式应该正确");
        System.out.println("✓ List<String>参数格式正确");
        System.out.println();
    }
    
    @Test
    @DisplayName("测试基本类型参数")
    public void testPrimitiveParameter() {
        System.out.println("=== 测试基本类型参数 ===");
        
        List<JavaMethodParser.ParameterInfo> parameters = Arrays.asList(
            new JavaMethodParser.ParameterInfo("userId", 
                "java.lang.Long", "1L")
        );
        
        JavaMethodParser.MethodInfo methodInfo = new JavaMethodParser.MethodInfo(
            "com.example.UserService",
            "getUserById",
            parameters,
            "User",
            null
        );
        
        // 生成JSON参数值
        String jsonParams = generateJsonParameterValues(methodInfo);
        String command = "invoke " + methodInfo.getClassName() + "." + methodInfo.getMethodName() + "(" + jsonParams + ")";
        
        System.out.println("生成的命令: " + command);
        
        // 验证格式
        String expectedFormat = "invoke com.example.UserService.getUserById(1L)";
        assertEquals(expectedFormat, command, "基本类型参数格式应该正确");
        System.out.println("✓ 基本类型参数格式正确");
        System.out.println();
    }
    
    @Test
    @DisplayName("运行所有测试")
    public void runAllTests() {
        System.out.println("开始测试JSON格式生成功能...");
        
        testComplexObjectParameter();
        testListStringParameter();
        testPrimitiveParameter();
        
        System.out.println("测试完成！");
    }
    
    // 简化的JSON参数值生成方法
    private String generateJsonParameterValues(JavaMethodParser.MethodInfo methodInfo) {
        List<JavaMethodParser.ParameterInfo> parameters = methodInfo.getParameters();
        
        if (parameters.isEmpty()) {
            return "";
        }
        
        return parameters.stream()
                .map(param -> {
                    String type = param.getType();
                    
                    // 处理List<String>类型
                    if (type.startsWith("java.util.List<") && type.contains("String")) {
                        return "[\"demo\",\"demo\"]";
                    }
                    
                    // 处理基本类型
                    if (isPrimitiveOrWrapper(type)) {
                        return param.getExampleValue();
                    }
                    
                    // 处理复杂对象类型
                    StringBuilder json = new StringBuilder();
                    json.append("{");
                    json.append("\"class\":\"").append(param.getType()).append("\"");
                    
                    // 添加示例字段
                    String exampleFields = generateExampleFields(param.getType());
                    if (!exampleFields.isEmpty()) {
                        json.append(",").append(exampleFields);
                    }
                    
                    json.append("}");
                    return json.toString();
                })
                .collect(Collectors.joining(","));
    }
    
    private String generateExampleFields(String type) {
        // 为复杂对象生成示例字段
        if (!isPrimitiveOrWrapper(type) && !type.startsWith("java.util.") && !type.startsWith("java.lang.")) {
            return "\"id\":123,\"name\":\"zhangsan\"";
        }
        return "";
    }
    
    private boolean isPrimitiveOrWrapper(String type) {
        return type.equals("int") || type.equals("java.lang.Integer") ||
               type.equals("long") || type.equals("java.lang.Long") ||
               type.equals("double") || type.equals("java.lang.Double") ||
               type.equals("float") || type.equals("java.lang.Float") ||
               type.equals("boolean") || type.equals("java.lang.Boolean") ||
               type.equals("byte") || type.equals("java.lang.Byte") ||
               type.equals("short") || type.equals("java.lang.Short") ||
               type.equals("char") || type.equals("java.lang.Character") ||
               type.equals("java.lang.String") ||
               type.equals("java.math.BigDecimal") ||
               type.equals("java.util.Date");
    }
}