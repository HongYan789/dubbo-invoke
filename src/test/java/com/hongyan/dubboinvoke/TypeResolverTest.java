package com.hongyan.dubboinvoke;

import com.hongyan.dubboinvoke.util.TypeResolver;
import com.hongyan.dubboinvoke.util.JavaClassFieldParser;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;

public class TypeResolverTest {
    
    public static void main(String[] args) {
        System.out.println("=== TypeResolver 测试 ===");
        
        // 测试基本类型
        testBasicTypes();
        
        // 测试复杂类型解析
        testComplexTypes();
        
        System.out.println("测试完成");
    }
    
    private static void testBasicTypes() {
        System.out.println("\n--- 基本类型测试 ---");
        
        String[] basicTypes = {
            "String", "int", "Long", "Boolean", "Double",
            "java.lang.String", "java.lang.Long", "java.util.List"
        };
        
        for (String type : basicTypes) {
            String resolved = TypeResolver.resolveFullTypeName(type, null, null);
            System.out.println(type + " -> " + resolved);
        }
    }
    
    private static void testComplexTypes() {
        System.out.println("\n--- 复杂类型测试 ---");
        
        String[] complexTypes = {
            "CompanyInfoDetailQry",
            "CompanyLngAndLatRequest",
            "UserBasicInfo"
        };
        
        for (String type : complexTypes) {
            String resolved = TypeResolver.resolveFullTypeName(type, null, null);
            System.out.println(type + " -> " + resolved);
            
            // 测试字段解析
            System.out.println("  字段解析结果:");
            java.util.List<JavaClassFieldParser.FieldInfo> fields = JavaClassFieldParser.parseClassFields(resolved, null);
            if (fields.isEmpty()) {
                System.out.println("    无法解析字段");
            } else {
                for (JavaClassFieldParser.FieldInfo field : fields) {
                    System.out.println("    " + field.getName() + ": " + field.getType() + " = " + field.getExampleValue());
                }
            }
        }
    }
}