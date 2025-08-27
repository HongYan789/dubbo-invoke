package com.hongyan.dubboinvoke;

import com.hongyan.dubboinvoke.client.DubboClientManager;

/**
 * 测试HTTP回退机制的工作
 */
public class HttpFallbackTest {
    
    public static void main(String[] args) {
        System.out.println("开始测试HTTP回退机制...");
        
        try {
            // 获取DubboClientManager实例
            DubboClientManager clientManager = DubboClientManager.getInstance();
            System.out.println("✅ DubboClientManager初始化成功");
            
            // 测试参数
            String serviceInterface = "com.jzt.zhcai.user.companyinfo.CompanyInfoDubboApi";
            String serviceUrl = "dubbo://10.7.8.50:16002";
            String methodName = "getCompanyInfoByCompanyId";
            String[] parameterTypes = {"java.lang.Long"};
            Object[] parameters = {1L};
            
            System.out.println("开始调用Dubbo服务（预期会回退到HTTP）...");
            System.out.println("服务接口: " + serviceInterface);
            System.out.println("服务地址: " + serviceUrl);
            System.out.println("方法名: " + methodName);
            
            try {
                // 执行调用 - 应该会先尝试标准Dubbo，然后回退到HTTP
                Object result = clientManager.invokeService(
                    serviceInterface, serviceUrl, methodName, parameterTypes, parameters
                );
                
                System.out.println("✅ 调用成功（通过HTTP回退机制）！");
                System.out.println("返回结果: " + result);
                System.out.println("结果类型: " + (result != null ? result.getClass().getName() : "null"));
                
            } catch (Exception callException) {
                System.out.println("调用异常: " + callException.getMessage());
                
                // 检查异常消息，确认是否正确尝试了HTTP回退
                if (callException.getMessage().contains("HTTP调用也失败") ||
                    callException.getMessage().contains("HTTP请求失败")) {
                    System.out.println("✅ 很好！已经正确尝试了HTTP回退机制");
                    System.out.println("这表明类加载器问题已被检测到并尝试了HTTP回退");
                } else if (callException.getMessage().contains("is not visible from class loader")) {
                    System.out.println("❌ 仍然有类加载器问题，HTTP回退可能未生效");
                    callException.printStackTrace();
                } else {
                    System.out.println("ℹ️ 其他类型异常: " + callException.getClass().getSimpleName());
                    System.out.println("异常消息: " + callException.getMessage());
                }
            }
            
            // 测试JSON格式调用
            System.out.println("\n开始测试JSON格式调用（HTTP回退）...");
            try {
                String jsonResult = clientManager.invokeServiceAsJson(
                    serviceInterface, serviceUrl, methodName, parameterTypes, parameters
                );
                System.out.println("✅ JSON调用成功（通过HTTP回退）！");
                System.out.println("JSON结果: " + jsonResult);
            } catch (Exception jsonException) {
                System.out.println("JSON调用异常: " + jsonException.getMessage());
            }
            
            // 测试连接
            System.out.println("\n开始测试连接（HTTP回退）...");
            boolean connected = clientManager.testConnection(serviceInterface, serviceUrl);
            System.out.println("连接测试结果: " + (connected ? "成功" : "失败"));
            
        } catch (Exception e) {
            System.out.println("❌ 测试失败: " + e.getMessage());
            e.printStackTrace();
        }
        
        System.out.println("\n测试完成！");
    }
}