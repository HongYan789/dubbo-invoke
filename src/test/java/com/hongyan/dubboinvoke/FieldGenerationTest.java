package com.hongyan.dubboinvoke;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 字段生成测试类
 */
public class FieldGenerationTest {
    
    public static void main(String[] args) {
        FieldGenerationTest test = new FieldGenerationTest();
        System.out.println("=== 开始运行FieldGenerationTest ===");
        
        try {
            test.testCompanyInfoDetailQryFields();
            test.testDubboCommandGeneration();
            test.testExampleValueGeneration();
            System.out.println("\n=== 所有测试完成 ===");
        } catch (Exception e) {
            System.err.println("测试执行失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 字段信息类
     */
    public static class FieldInfo {
        private final String name;
        private final String type;
        private final String exampleValue;
        
        public FieldInfo(String name, String type, String exampleValue) {
            this.name = name;
            this.type = type;
            this.exampleValue = exampleValue;
        }
        
        public String getName() { return name; }
        public String getType() { return type; }
        public String getExampleValue() { return exampleValue; }
    }
    
    @Test
    @DisplayName("测试CompanyInfoDetailQry字段生成")
    public void testCompanyInfoDetailQryFields() {
        System.out.println("=== 测试字段生成功能 ===");
        System.out.println();
        System.out.println("1. 测试CompanyInfoDetailQry字段生成:");
        
        List<FieldInfo> fields = getCompanyInfoDetailQryFields();
        
        System.out.println("字段列表:");
        for (FieldInfo field : fields) {
            System.out.println("  - " + field.getName() + ": " + field.getType() + " = " + field.getExampleValue());
        }
        
        String jsonFields = fieldsToJsonString(fields);
        System.out.println("JSON字段: " + jsonFields);
        System.out.println();
        
        // 验证字段数量和内容
        assertEquals(3, fields.size(), "应该有3个字段");
        assertEquals("id", fields.get(0).getName());
        assertEquals("name", fields.get(1).getName());
        assertEquals("code", fields.get(2).getName());
    }
    
    @Test
    @DisplayName("测试完整的Dubbo命令生成")
    public void testDubboCommandGeneration() {
        System.out.println("2. 测试完整的Dubbo命令生成:");
        
        String command = generateDubboCommand(
            "com.jzt.zhcai.user.front.userbasic.api.CompanyInfoApi",
            "queryCompanyInfoDetail",
            "com.jzt.zhcai.user.front.userbasic.dto.CompanyInfoDetailQry"
        );
        
        System.out.println("生成的命令: " + command);
        System.out.println();
        
        // 验证命令格式
        assertTrue(command.startsWith("invoke "), "命令应该以invoke开头");
        assertTrue(command.contains("CompanyInfoApi.queryCompanyInfoDetail"), "命令应该包含正确的方法名");
        assertTrue(command.contains("\"class\":"), "命令应该包含class字段");
    }
    
    @Test
    @DisplayName("测试不同类型的示例值生成")
    public void testExampleValueGeneration() {
        System.out.println("3. 测试不同类型的示例值:");
        
        String[] types = {
            "java.lang.String",
            "java.lang.Long", 
            "java.lang.Integer",
            "java.lang.Boolean",
            "java.util.List<String>",
            "java.util.Map<String,Object>"
        };
        
        for (String type : types) {
            String value = generateExampleValue(type);
            System.out.println("  " + type + " -> " + value);
            assertNotNull(value, "示例值不应该为null");
        }
        
        System.out.println();
        System.out.println("=== 测试完成 ===");
    }
    
    @Test
    @DisplayName("运行所有字段生成测试")
    public void runAllFieldGenerationTests() {
        testCompanyInfoDetailQryFields();
        testDubboCommandGeneration();
        testExampleValueGeneration();
    }
    
    // 获取CompanyInfoDetailQry的字段信息
    private List<FieldInfo> getCompanyInfoDetailQryFields() {
        List<FieldInfo> fields = new ArrayList<>();
        fields.add(new FieldInfo("id", "Long", "123L"));
        fields.add(new FieldInfo("name", "String", "\"测试公司\""));
        fields.add(new FieldInfo("code", "String", "\"TEST001\""));
        return fields;
    }
    
    // 将字段列表转换为JSON字符串
    private String fieldsToJsonString(List<FieldInfo> fields) {
        return fields.stream()
                .map(field -> "\"" + field.getName() + "\":" + field.getExampleValue())
                .collect(Collectors.joining(","));
    }
    
    // 生成完整的Dubbo命令
    private String generateDubboCommand(String className, String methodName, String parameterType) {
        List<FieldInfo> fields = getCompanyInfoDetailQryFields();
        String jsonFields = fieldsToJsonString(fields);
        
        return "invoke " + className + "." + methodName + 
               "({\"class\":\"" + parameterType + "\"," + jsonFields + "})";
    }
    
    // 根据类型生成示例值
    private String generateExampleValue(String type) {
        switch (type) {
            case "java.lang.String":
            case "String":
                return "\"demo\"";
            case "int":
            case "java.lang.Integer":
            case "Integer":
                return "1";
            case "long":
            case "java.lang.Long":
            case "Long":
                return "1L";
            case "double":
            case "java.lang.Double":
            case "Double":
                return "1.0";
            case "float":
            case "java.lang.Float":
            case "Float":
                return "1.0f";
            case "boolean":
            case "java.lang.Boolean":
            case "Boolean":
                return "true";
            case "byte":
            case "java.lang.Byte":
            case "Byte":
                return "(byte)1";
            case "short":
            case "java.lang.Short":
            case "Short":
                return "(short)1";
            case "char":
            case "java.lang.Character":
            case "Character":
                return "'a'";
            default:
                if (type.startsWith("java.util.List") || type.contains("List")) {
                    return "[]";
                } else if (type.startsWith("java.util.Map") || type.contains("Map")) {
                    return "{}";
                } else if (type.endsWith("[]")) {
                    return "[]";
                } else {
                    return "{}";
                }
        }
    }
}