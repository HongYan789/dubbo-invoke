package com.hongyan.dubboinvoke;

import com.hongyan.dubboinvoke.util.JavaClassFieldParser;
import com.hongyan.dubboinvoke.util.JavaClassFieldParser.FieldInfo;
import java.util.List;

/**
 * 测试嵌套对象的递归解析功能
 */
public class NestedObjectTest {
    
    public static void main(String[] args) {
        System.out.println("=== 开始测试嵌套对象递归解析 ===");
        
        // 测试包含嵌套对象的复杂类型
        testNestedObjectParsing();
        
        System.out.println("=== 嵌套对象测试完成 ===");
    }
    
    private static void testNestedObjectParsing() {
        System.out.println("\n1. 测试包含嵌套对象的类:");
        
        // 测试一个包含嵌套对象的类
        String className = "com.hongyan.dubboinvoke.dto.UserProfileRequest";
        List<FieldInfo> fields = JavaClassFieldParser.parseClassFields(className, null);
        
        System.out.println("  解析类: " + className);
        System.out.println("  字段数量: " + fields.size());
        
        for (FieldInfo field : fields) {
            System.out.println("    - " + field.getName() + ": " + field.getType() + " = " + field.getExampleValue());
        }
        
        // 生成JSON字符串
        String jsonFields = JavaClassFieldParser.fieldsToJsonString(fields);
        System.out.println("  生成的JSON字段: " + jsonFields);
        
        System.out.println("\n2. 测试另一个复杂对象:");
        
        // 测试另一个包含嵌套对象的类
        String className2 = "com.hongyan.dubboinvoke.dto.OrderDetailRequest";
        List<FieldInfo> fields2 = JavaClassFieldParser.parseClassFields(className2, null);
        
        System.out.println("  解析类: " + className2);
        System.out.println("  字段数量: " + fields2.size());
        
        for (FieldInfo field : fields2) {
            System.out.println("    - " + field.getName() + ": " + field.getType() + " = " + field.getExampleValue());
        }
        
        // 生成JSON字符串
        String jsonFields2 = JavaClassFieldParser.fieldsToJsonString(fields2);
        System.out.println("  生成的JSON字段: " + jsonFields2);
    }
}