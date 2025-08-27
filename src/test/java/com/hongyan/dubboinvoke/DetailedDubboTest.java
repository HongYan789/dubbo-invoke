package com.hongyan.dubboinvoke;

import com.hongyan.dubboinvoke.client.DubboClientManager;
import com.alibaba.dubbo.rpc.service.GenericService;

/**
 * 详细的Dubbo调用测试
 * 用于分析返回结果为null的问题
 */
public class DetailedDubboTest {
    public static void main(String[] args) {
        String serviceInterface = "com.jzt.zhcai.user.companyinfo.CompanyInfoDubboApi";
        String serviceUrl = "dubbo://10.7.8.50:16002";
        String methodName = "queryCompanyInfoByCompanyId";
        
        DubboClientManager clientManager = DubboClientManager.getInstance();
        
        System.out.println("=== 开始详细测试 ===");
        
        try {
            // 1. 测试连接
            System.out.println("1. 测试连接...");
            boolean connected = clientManager.testConnection(serviceInterface, serviceUrl);
            System.out.println("连接结果: " + connected);
            
            if (!connected) {
                System.err.println("连接失败，停止测试");
                return;
            }
            
            // 2. 获取泛化服务
            System.out.println("2. 获取泛化服务...");
            GenericService genericService = clientManager.getGenericService(serviceInterface, serviceUrl);
            System.out.println("泛化服务: " + genericService);
            
            // 3. 测试不同的参数类型
            System.out.println("3. 测试不同参数类型...");
            
            // 3.1 测试 Long 类型
            testWithParameters(genericService, methodName, "java.lang.Long", 1L);
            
            // 3.2 测试基本类型 long
            testWithParameters(genericService, methodName, "long", 1L);
            
            // 3.3 测试 Integer 类型
            testWithParameters(genericService, methodName, "java.lang.Integer", 1);
            
            // 3.4 测试基本类型 int
            testWithParameters(genericService, methodName, "int", 1);
            
            // 3.5 测试 String 类型
            testWithParameters(genericService, methodName, "java.lang.String", "1");
            
            // 4. 尝试调用其他可能的方法
            System.out.println("4. 尝试调用其他方法...");
            try {
                Object result = genericService.$invoke("toString", new String[0], new Object[0]);
                System.out.println("toString() 调用结果: " + result);
            } catch (Exception e) {
                System.out.println("toString() 调用失败: " + e.getMessage());
            }
            
        } catch (Exception e) {
            System.err.println("测试过程中发生异常: " + e.getMessage());
            e.printStackTrace();
        }
        
        System.out.println("=== 测试完成 ===");
    }
    
    private static void testWithParameters(GenericService service, String methodName, String paramType, Object paramValue) {
        System.out.println("测试参数类型: " + paramType + ", 值: " + paramValue);
        try {
            Object result = service.$invoke(methodName, new String[]{paramType}, new Object[]{paramValue});
            System.out.println("  结果: " + result);
            System.out.println("  结果类型: " + (result != null ? result.getClass().getName() : "null"));
            if (result != null) {
                System.out.println("  结果详情: " + result.toString());
            }
        } catch (Exception e) {
            System.err.println("  调用失败: " + e.getMessage());
            e.printStackTrace();
        }
        System.out.println();
    }
}