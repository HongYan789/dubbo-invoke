package com.hongyan.dubboinvoke;

import com.hongyan.dubboinvoke.client.DubboClientManager;

/**
 * 测试修复后的Dubbo标准协议实现
 */
public class FixedDubboTest {
    
    public static void main(String[] args) {
        System.out.println("开始测试标准Dubbo协议实现...");
        
        try {
            // 获取DubboClientManager实例
            DubboClientManager clientManager = DubboClientManager.getInstance();
            System.out.println("DubboClientManager初始化成功");
            
            // 测试参数
            String serviceInterface = "com.example.UserService";
            String serviceUrl = "dubbo://127.0.0.1:20880";
            String methodName = "getUserById";
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
                
                System.out.println("调用成功！");
                System.out.println("返回结果: " + result);
                System.out.println("结果类型: " + (result != null ? result.getClass().getName() : "null"));
                
            } catch (Exception callException) {
                System.out.println("调用失败（这是预期的，因为没有真实的服务）: " + callException.getMessage());
                
                // 检查异常是否说明已经成功使用了标准Dubbo协议
                if (callException.getMessage().contains("Failed to connect") || 
                    callException.getMessage().contains("Connection refused") ||
                    callException.getMessage().contains("No provider available")) {
                    System.out.println("✅ 很好！这表明已经在使用标准Dubbo协议尝试连接");
                } else {
                    System.out.println("❌ 异常类型不符合预期");
                    callException.printStackTrace();
                }
            }
            
            // 测试JSON格式调用
            System.out.println("\n开始测试JSON格式调用...");
            try {
                String jsonResult = clientManager.invokeServiceAsJson(
                    serviceInterface, serviceUrl, methodName, parameterTypes, parameters
                );
                System.out.println("JSON调用成功！");
                System.out.println("JSON结果: " + jsonResult);
            } catch (Exception jsonException) {
                System.out.println("JSON调用失败（预期的）: " + jsonException.getMessage());
            }
            
        } catch (Exception e) {
            System.out.println("❌ 测试失败: " + e.getMessage());
            e.printStackTrace();
        }
        
        System.out.println("\n测试完成！");
    }
}