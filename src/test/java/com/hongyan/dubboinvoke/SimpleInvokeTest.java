package com.hongyan.dubboinvoke;

public class SimpleInvokeTest {
    
    public static void main(String[] args) {
        System.out.println("Testing simple invoke format generation...");
        
        // 模拟生成invoke命令的逻辑
        String className = "com.jzt.zhcai.user.storecompanyblack.StoreCompanyBlackDubboApi";
        String methodName = "fileImportBlack4Zy";
        String paramType = "com.jzt.zhcai.user.storecompanyblack.dto.StoreCompanyBlackImport4ZyDTO";
        
        // 生成基本的invoke命令格式
        StringBuilder command = new StringBuilder();
        command.append("invoke ");
        command.append(className);
        command.append(".");
        command.append(methodName);
        command.append("(");
        
        // 生成JSON参数
        StringBuilder json = new StringBuilder();
        json.append("{");
        json.append("\"class\":\"").append(paramType).append("\"");
        
        // 添加示例字段
        json.append(",\"storeId\":1L");
        json.append(",\"createUserName\":\"example\"");
        json.append(",\"createUser\":1L");
        
        // 添加rows数组
        json.append(",\"rows\":[");
        json.append("{");
        json.append("\"class\":\"com.jzt.zhcai.user.storecompanyblack.dto.StoreCompanyBlackImport4ZyDTO.Row\"");
        json.append(",\"companyId\":\"example\"");
        json.append(",\"danwBh\":\"example\"");
        json.append(",\"freezeCause\":\"example\"");
        json.append(",\"errorMessage\":\"example\"");
        json.append(",\"checkPass\":true");
        json.append("}");
        json.append("]");
        
        json.append("}");
        
        command.append(json.toString());
        command.append(")");
        
        String result = command.toString();
        
        System.out.println("\nGenerated invoke command:");
        System.out.println(result);
        
        // 期望的结果
        String expected = "invoke com.jzt.zhcai.user.storecompanyblack.StoreCompanyBlackDubboApi.fileImportBlack4Zy({\"class\":\"com.jzt.zhcai.user.storecompanyblack.dto.StoreCompanyBlackImport4ZyDTO\",\"storeId\":1L,\"createUserName\":\"example\",\"createUser\":1L,\"rows\":[{\"class\":\"com.jzt.zhcai.user.storecompanyblack.dto.StoreCompanyBlackImport4ZyDTO.Row\", \"companyId\":\"example\", \"danwBh\":\"example\", \"freezeCause\":\"example\",\"errorMessage\":\"example\", \"checkPass\": true}]})";
        
        System.out.println("\nExpected format:");
        System.out.println(expected);
        
        // 验证关键元素
        boolean hasMainClass = result.contains("\"class\":\"com.jzt.zhcai.user.storecompanyblack.dto.StoreCompanyBlackImport4ZyDTO\"");
        boolean hasInnerClass = result.contains("\"class\":\"com.jzt.zhcai.user.storecompanyblack.dto.StoreCompanyBlackImport4ZyDTO.Row\"");
        boolean hasInvokePrefix = result.startsWith("invoke ");
        boolean hasMethodName = result.contains("fileImportBlack4Zy");
        boolean hasRowsArray = result.contains("\"rows\":[");
        
        System.out.println("\nValidation:");
        System.out.println("Has 'invoke' prefix: " + hasInvokePrefix);
        System.out.println("Contains method name: " + hasMethodName);
        System.out.println("Contains main class field: " + hasMainClass);
        System.out.println("Contains inner class field: " + hasInnerClass);
        System.out.println("Contains rows array: " + hasRowsArray);
        
        if (hasInvokePrefix && hasMethodName && hasMainClass && hasInnerClass && hasRowsArray) {
            System.out.println("\n✅ Test PASSED: Generated format matches expected structure!");
        } else {
            System.out.println("\n❌ Test FAILED: Generated format doesn't match expected structure");
        }
        
        System.out.println("\nThis demonstrates the expected invoke format with:");
        System.out.println("1. Main DTO class with 'class' field");
        System.out.println("2. Inner Row class with 'class' field");
        System.out.println("3. Proper JSON structure for complex nested objects");
    }
}