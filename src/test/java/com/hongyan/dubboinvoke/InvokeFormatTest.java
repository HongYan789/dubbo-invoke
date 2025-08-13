package com.hongyan.dubboinvoke;

import com.hongyan.dubboinvoke.generator.DubboCommandGenerator;
import com.hongyan.dubboinvoke.util.JavaMethodParser;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

public class InvokeFormatTest {

    @Test
    public void testStoreCompanyBlackImport4ZyDTOFormat() {
        // 创建模拟的MethodInfo
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
            null
        );
        
        String result = DubboCommandGenerator.generateCommand(methodInfo, null);
        
        System.out.println("Generated invoke command:");
        System.out.println(result);
        
        // 期望的格式应该包含:
        // 1. 主类的class字段: "class":"com.jzt.zhcai.user.storecompanyblack.dto.StoreCompanyBlackImport4ZyDTO"
        // 2. 内部类Row的class字段: "class":"com.jzt.zhcai.user.storecompanyblack.dto.StoreCompanyBlackImport4ZyDTO.Row"
        
        // 验证是否包含正确的class字段格式
        assert result.contains("\"class\":\"com.jzt.zhcai.user.storecompanyblack.dto.StoreCompanyBlackImport4ZyDTO\"");
        assert result.contains("\"class\":\"com.jzt.zhcai.user.storecompanyblack.dto.StoreCompanyBlackImport4ZyDTO.Row\"");
    }
}