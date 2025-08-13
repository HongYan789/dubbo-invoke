package com.hongyan.dubboinvoke;

import com.hongyan.dubboinvoke.generator.DubboCommandGenerator;
import com.hongyan.dubboinvoke.util.JavaMethodParser;

import java.util.Arrays;

public class TestInvokeFormat {
    
    public static void main(String[] args) {
        System.out.println("Testing invoke format generation...");
        
        // 创建模拟的MethodInfo（不使用Project）
        JavaMethodParser.ParameterInfo paramInfo = new JavaMethodParser.ParameterInfo(
            "request", 
            "com.jzt.zhcai.user.storecompanyblack.dto.StoreCompanyBlackImport4ZyDTO",
            ""
        );
        
        JavaMethodParser.MethodInfo methodInfo = new JavaMethodParser.MethodInfo(
            "com.jzt.zhcai.user.storecompanyblack.StoreCompanyBlackDubboApi",
            "fileImportBlack4Zy",
            Arrays.asList(paramInfo),
            "void",
            null  // 不使用PsiClass
        );
        
        try {
            // 使用不带Project的方法
            String result = DubboCommandGenerator.generateCommand(methodInfo);
            
            System.out.println("\nGenerated invoke command:");
            System.out.println(result);
            
            // 检查基本格式
            boolean hasInvokePrefix = result.startsWith("invoke ");
            boolean hasMethodName = result.contains("fileImportBlack4Zy");
            boolean hasClassName = result.contains("StoreCompanyBlackDubboApi");
            boolean hasJsonFormat = result.contains("{") && result.contains("}");
            
            System.out.println("\nValidation:");
            System.out.println("Has 'invoke' prefix: " + hasInvokePrefix);
            System.out.println("Contains method name: " + hasMethodName);
            System.out.println("Contains class name: " + hasClassName);
            System.out.println("Has JSON format: " + hasJsonFormat);
            
            // 检查是否包含class字段
            boolean hasClassField = result.contains("\"class\":");
            System.out.println("Contains 'class' field: " + hasClassField);
            
            if (hasInvokePrefix && hasMethodName && hasClassName && hasJsonFormat) {
                System.out.println("\n✅ Basic format test PASSED!");
            } else {
                System.out.println("\n❌ Basic format test FAILED!");
            }
            
        } catch (Exception e) {
            System.err.println("Error generating command: " + e.getMessage());
            e.printStackTrace();
        }
    }
}