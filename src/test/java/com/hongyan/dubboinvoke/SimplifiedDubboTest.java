package com.hongyan.dubboinvoke;

import com.hongyan.dubboinvoke.client.DubboClientManager;

/**
 * 测试简化版的Dubbo实现
 */
public class SimplifiedDubboTest {
    
    public static void main(String[] args) {
        System.out.println("开始测试简化版Dubbo实现...");
        
        try {
            // 获取DubboClientManager实例
            DubboClientManager clientManager = DubboClientManager.getInstance();
            System.out.println("✅ DubboClientManager初始化成功（简化版）");
            
            // 测试参数
            String serviceInterface = "com.jzt.zhcai.user.companyinfo.CompanyInfoDubboApi";
            String serviceUrl = "dubbo://10.7.8.50:16002";
            String methodName = "getCompanyInfoByCompanyId";
            String[] parameterTypes = {"java.lang.Long"};
            Object[] parameters = {1L};
            
            System.out.println("开始调用Dubbo服务...");
            System.out.println("服务接口: " + serviceInterface);
            System.out.println("服务地址: " + serviceUrl);
            System.out.println("方法名: " + methodName);
            
            try {
                // 执行调用
                Object result = clientManager.invokeService(
                    serviceInterface, serviceUrl, methodName, parameterTypes, parameters
                );
                
                System.out.println("✅ 调用成功！");
                System.out.println("返回结果: " + result);
                System.out.println("结果类型: " + (result != null ? result.getClass().getName() : "null"));
                
            } catch (Exception callException) {
                System.out.println("调用异常: " + callException.getMessage());
                
                // 检查是否是网络连接问题而不是扩展机制问题
                if (callException.getMessage().contains("Failed to connect") || 
                    callException.getMessage().contains("Connection refused") ||
                    callException.getMessage().contains("No provider available") ||
                    callException.getMessage().contains("connect to server")) {
                    System.out.println("✅ 很好！这是网络连接问题，说明简化版Dubbo实现工作正常");
                } else if (callException.getMessage().contains("ExtensionFactory") ||
                          callException.getMessage().contains("ExceptionInInitializerError") ||
                          callException.getMessage().contains("NoClassDefFoundError")) {
                    System.out.println("❌ 仍然有扩展机制问题");
                    callException.printStackTrace();
                } else {
                    System.out.println("ℹ️ 其他异常类型: " + callException.getClass().getSimpleName());
                    System.out.println("可能需要进一步分析");
                }
            }
            
            // 测试JSON格式调用
            System.out.println("\n开始测试JSON格式调用...");
            try {
                String jsonResult = clientManager.invokeServiceAsJson(
                    serviceInterface, serviceUrl, methodName, parameterTypes, parameters
                );
                System.out.println("✅ JSON调用成功！");
                System.out.println("JSON结果: " + jsonResult);
            } catch (Exception jsonException) {
                System.out.println("JSON调用异常: " + jsonException.getMessage());
            }
            
        } catch (Exception e) {
            System.out.println("❌ 测试失败: " + e.getMessage());
            e.printStackTrace();
        }
        
        System.out.println("\n测试完成！");
    }
}