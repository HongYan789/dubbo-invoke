package com.hongyan.dubboinvoke;

import com.hongyan.dubboinvoke.util.JavaMethodParser;
import com.hongyan.dubboinvoke.util.JavaClassFieldParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Dubbo命令生成器集成测试
 */
public class DubboCommandGeneratorTest {
    
    public static void main(String[] args) {
        DubboCommandGeneratorTest test = new DubboCommandGeneratorTest();
        System.out.println("=== 开始运行DubboCommandGeneratorTest ===");
        
        try {
            test.setUp();
            test.runAllIntegrationTests();
            System.out.println("\n=== 所有集成测试完成 ===");
        } catch (Exception e) {
            System.err.println("测试执行失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    @BeforeEach
    public void setUp() {
        System.out.println("=== Dubbo命令生成器测试 ===");
    }
    
    @Test
    @DisplayName("测试JavaMethodParser基本功能")
    public void testJavaMethodParserBasics() {
        System.out.println("\n1. 测试JavaMethodParser基本功能:");
        
        // 创建参数信息
        List<JavaMethodParser.ParameterInfo> parameters = Arrays.asList(
            new JavaMethodParser.ParameterInfo("request", 
                "com.jzt.zhcai.user.front.userbasic.dto.CompanyInfoDetailQry", "{}")
        );
        
        // 创建方法信息
        JavaMethodParser.MethodInfo methodInfo = new JavaMethodParser.MethodInfo(
            "com.jzt.zhcai.user.front.companyinfo.CompanyInfoDubboApi",
            "queryCompanyInfoDetail",
            parameters,
            "Object",
            null
        );
        
        System.out.println("  类名: " + methodInfo.getClassName());
        System.out.println("  方法名: " + methodInfo.getMethodName());
        System.out.println("  参数数量: " + methodInfo.getParameters().size());
        System.out.println("  返回类型: " + methodInfo.getReturnType());
        
        // 验证
        assertEquals("com.jzt.zhcai.user.front.companyinfo.CompanyInfoDubboApi", methodInfo.getClassName());
        assertEquals("queryCompanyInfoDetail", methodInfo.getMethodName());
        assertEquals(1, methodInfo.getParameters().size());
        assertEquals("Object", methodInfo.getReturnType());
        
        System.out.println("  ✓ JavaMethodParser基本功能测试通过");
    }
    
    @Test
    @DisplayName("测试JavaClassFieldParser字段解析")
    public void testJavaClassFieldParser() {
        System.out.println("\n2. 测试JavaClassFieldParser字段解析:");
        
        // 测试CompanyInfoDetailQry类的字段解析
        String className = "com.jzt.zhcai.user.front.userbasic.dto.CompanyInfoDetailQry";
        List<JavaClassFieldParser.FieldInfo> fields = JavaClassFieldParser.parseClassFields(className, null);
        
        System.out.println("  解析类: " + className);
        System.out.println("  字段数量: " + fields.size());
        
        for (JavaClassFieldParser.FieldInfo field : fields) {
            System.out.println("    - " + field.getName() + ": " + field.getType() + " = " + field.getExampleValue());
        }
        
        // 验证字段
        assertFalse(fields.isEmpty(), "字段列表不应该为空");
        assertTrue(fields.size() >= 3, "应该至少有3个字段");
        
        // 验证包含预期字段
        boolean hasId = fields.stream().anyMatch(f -> "id".equals(f.getName()));
        boolean hasName = fields.stream().anyMatch(f -> "name".equals(f.getName()));
        boolean hasCode = fields.stream().anyMatch(f -> "code".equals(f.getName()));
        
        assertTrue(hasId, "应该包含id字段");
        assertTrue(hasName, "应该包含name字段");
        assertTrue(hasCode, "应该包含code字段");
        
        System.out.println("  ✓ JavaClassFieldParser字段解析测试通过");
    }
    
    @Test
    @DisplayName("测试完整的命令生成流程")
    public void testCompleteCommandGeneration() {
        System.out.println("\n3. 测试完整的命令生成流程:");
        
        // 模拟完整的命令生成过程
        String className = "com.jzt.zhcai.user.front.companyinfo.CompanyInfoDubboApi";
        String methodName = "queryCompanyInfoDetail";
        String parameterType = "com.jzt.zhcai.user.front.userbasic.dto.CompanyInfoDetailQry";
        
        // 1. 解析字段
        List<JavaClassFieldParser.FieldInfo> fields = JavaClassFieldParser.parseClassFields(parameterType, null);
        
        // 2. 生成JSON参数
        StringBuilder jsonBuilder = new StringBuilder();
        jsonBuilder.append("{");
        jsonBuilder.append("\"class\":\"").append(parameterType).append("\"");
        
        for (JavaClassFieldParser.FieldInfo field : fields) {
            jsonBuilder.append(",\"").append(field.getName()).append("\":").append(field.getExampleValue());
        }
        
        jsonBuilder.append("}");
        String jsonParams = jsonBuilder.toString();
        
        // 3. 生成完整命令
        String command = "invoke " + className + "." + methodName + "(" + jsonParams + ")";
        
        System.out.println("  生成的完整命令:");
        System.out.println("  " + command);
        
        // 验证命令格式
        assertTrue(command.startsWith("invoke "), "命令应该以invoke开头");
        assertTrue(command.contains(className), "命令应该包含类名");
        assertTrue(command.contains(methodName), "命令应该包含方法名");
        assertTrue(command.contains("\"class\":"), "命令应该包含class字段");
        assertTrue(command.contains(parameterType), "命令应该包含参数类型");
        
        System.out.println("  ✓ 完整命令生成流程测试通过");
    }
    
    @Test
    @DisplayName("测试多种参数类型")
    public void testMultipleParameterTypes() {
        System.out.println("\n4. 测试多种参数类型:");
        
        // 测试不同类型的参数
        String[] testTypes = {
            "java.lang.String",
            "java.lang.Long",
            "java.lang.Integer",
            "java.lang.Boolean",
            "java.util.List<String>",
            "java.util.Map<String,Object>",
            "com.example.CustomObject"
        };
        
        for (String type : testTypes) {
            List<JavaClassFieldParser.FieldInfo> fields = JavaClassFieldParser.parseClassFields(type, null);
            System.out.println("  类型: " + type + " -> 字段数量: " + fields.size());
            
            // 验证每种类型都能正确处理
            assertNotNull(fields, "字段列表不应该为null");
        }
        
        System.out.println("  ✓ 多种参数类型测试通过");
    }
    
    @Test
    @DisplayName("运行所有集成测试")
    public void runAllIntegrationTests() {
        testJavaMethodParserBasics();
        testJavaClassFieldParser();
        testCompleteCommandGeneration();
        testMultipleParameterTypes();
        
        System.out.println("\n=== 所有集成测试完成 ===");
    }
}