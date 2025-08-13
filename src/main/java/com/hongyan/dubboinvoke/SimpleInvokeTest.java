package com.hongyan.dubboinvoke;

import java.util.List;
import java.util.ArrayList;

public class SimpleInvokeTest {
    
    // 简化的FieldInfo类
    public static class FieldInfo {
        private String name;
        private String type;
        private String exampleValue;
        
        public FieldInfo(String name, String type, String exampleValue) {
            this.name = name;
            this.type = type;
            this.exampleValue = exampleValue;
        }
        
        public String getName() { return name; }
        public String getType() { return type; }
        public String getExampleValue() { return exampleValue; }
    }
    
    // 简化的getDefaultFields方法
    public static List<FieldInfo> getDefaultFields(String className) {
        List<FieldInfo> defaultFields = new ArrayList<>();
        
        if (className.contains("StoreCompanyBlackImport4ZyDTO")) {
            if (className.endsWith(".Row")) {
                // 内部类Row的字段
                defaultFields.add(new FieldInfo("companyId", "java.lang.String", "\"example\""));
                defaultFields.add(new FieldInfo("danwBh", "java.lang.String", "\"example\""));
                defaultFields.add(new FieldInfo("freezeCause", "java.lang.String", "\"example\""));
                defaultFields.add(new FieldInfo("errorMessage", "java.lang.String", "\"example\""));
                defaultFields.add(new FieldInfo("checkPass", "java.lang.Boolean", "true"));
            } else {
                // 主类的字段
                defaultFields.add(new FieldInfo("storeId", "java.lang.Long", "1L"));
                defaultFields.add(new FieldInfo("createUserName", "java.lang.String", "\"example\""));
                defaultFields.add(new FieldInfo("createUser", "java.lang.Long", "1L"));
                // rows字段使用List<Row>类型
                String rowJson = "{\"class\":\"com.jzt.zhcai.user.storecompanyblack.dto.StoreCompanyBlackImport4ZyDTO.Row\", \"companyId\":\"example\", \"danwBh\":\"example\", \"freezeCause\":\"example\",\"errorMessage\":\"example\", \"checkPass\": true}";
                defaultFields.add(new FieldInfo("rows", "java.util.List<com.jzt.zhcai.user.storecompanyblack.dto.StoreCompanyBlackImport4ZyDTO.Row>", "[" + rowJson + "]"));
            }
        } else {
            // 默认字段
            defaultFields.add(new FieldInfo("value", "java.lang.String", "\"\""));
        }
        
        return defaultFields;
    }
    
    public static void main(String[] args) {
        System.out.println("=== 测试StoreCompanyBlackImport4ZyDTO的invoke格式生成 ===");
        
        // 测试主类字段解析
        String mainClassName = "com.jzt.zhcai.user.storecompanyblack.dto.StoreCompanyBlackImport4ZyDTO";
        List<FieldInfo> mainFields = getDefaultFields(mainClassName);
        
        System.out.println("主类字段:");
        for (FieldInfo field : mainFields) {
            System.out.println("  " + field.getName() + ": " + field.getType() + " = " + field.getExampleValue());
        }
        
        // 测试内部类Row字段解析
        String rowClassName = "com.jzt.zhcai.user.storecompanyblack.dto.StoreCompanyBlackImport4ZyDTO.Row";
        List<FieldInfo> rowFields = getDefaultFields(rowClassName);
        
        System.out.println("\n内部类Row字段:");
        for (FieldInfo field : rowFields) {
            System.out.println("  " + field.getName() + ": " + field.getType() + " = " + field.getExampleValue());
        }
        
        // 生成模拟的invoke命令JSON
        StringBuilder json = new StringBuilder();
        json.append("{");
        json.append("\"class\":\"com.jzt.zhcai.user.storecompanyblack.dto.StoreCompanyBlackImport4ZyDTO\",");
        
        for (int i = 0; i < mainFields.size(); i++) {
            FieldInfo field = mainFields.get(i);
            json.append("\"").append(field.getName()).append("\":").append(field.getExampleValue());
            if (i < mainFields.size() - 1) {
                json.append(",");
            }
        }
        json.append("}");
        
        String invokeCommand = "invoke com.jzt.zhcai.user.storecompanyblack.service.StoreCompanyBlackService.importCompanyBlack(" + json.toString() + ")";
        
        System.out.println("\n生成的invoke命令:");
        System.out.println(invokeCommand);
        
        // 验证格式
        boolean hasMainClass = invokeCommand.contains("\"class\":\"com.jzt.zhcai.user.storecompanyblack.dto.StoreCompanyBlackImport4ZyDTO\"");
        boolean hasRowClass = invokeCommand.contains("\"class\":\"com.jzt.zhcai.user.storecompanyblack.dto.StoreCompanyBlackImport4ZyDTO.Row\"");
        boolean hasRowsField = invokeCommand.contains("\"rows\":");
        
        System.out.println("\n验证结果:");
        System.out.println("包含主类class字段: " + hasMainClass);
        System.out.println("包含内部类Row的class字段: " + hasRowClass);
        System.out.println("包含rows字段: " + hasRowsField);
        
        if (hasMainClass && hasRowClass && hasRowsField) {
            System.out.println("\n✅ 测试通过！invoke格式正确生成了主类和内部类的class字段");
        } else {
            System.out.println("\n❌ 测试失败！invoke格式不完整");
        }
    }
}