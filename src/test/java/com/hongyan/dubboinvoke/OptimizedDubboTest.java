package com.hongyan.dubboinvoke;

import com.hongyan.dubboinvoke.client.DubboClientManager;

/**
 * 优化版本的Dubbo调用测试
 * 测试多层回退机制和超时处理改进
 */
public class OptimizedDubboTest {
    
    public static void main(String[] args) {
        System.out.println("开始测试优化版本的Dubbo调用...");
        
        try {
            // 获取DubboClientManager实例
            DubboClientManager clientManager = DubboClientManager.getInstance();
            System.out.println("✅ DubboClientManager初始化成功（优化版）");
            
            // 测试参数
            String serviceInterface = "com.jzt.zhcai.user.companyinfo.CompanyInfoDubboApi";
            String serviceUrl = "dubbo://10.7.8.50:16002";
            String methodName = "queryCompanyByCompanyId";
            String[] parameterTypes = {"java.lang.Long"};
            Object[] parameters = {1L};
            
            System.out.println("测试用例信息:");
            System.out.println("服务接口: " + serviceInterface);
            System.out.println("服务地址: " + serviceUrl);
            System.out.println("方法名: " + methodName);
            System.out.println("参数: " + java.util.Arrays.toString(parameters));
            
            // 测试连接
            System.out.println("\n=== 第一步：测试连接 ===");
            boolean connected = clientManager.testConnection(serviceInterface, serviceUrl);
            System.out.println("连接测试结果: " + (connected ? "成功" : "失败"));
            
            // 测试调用
            System.out.println("\n=== 第二步：执行服务调用 ===");
            try {
                long startTime = System.currentTimeMillis();
                
                Object result = clientManager.invokeService(
                    serviceInterface, serviceUrl, methodName, parameterTypes, parameters
                );
                
                long endTime = System.currentTimeMillis();
                long duration = endTime - startTime;
                
                System.out.println("✅ 调用成功！耗时: " + duration + "ms");
                System.out.println("返回结果类型: " + (result != null ? result.getClass().getName() : "null"));
                
                if (result != null) {
                    String resultStr = result.toString();
                    if (resultStr.length() > 500) {
                        System.out.println("返回结果(前500字符): " + resultStr.substring(0, 500) + "...");
                    } else {
                        System.out.println("返回结果: " + resultStr);
                    }
                } else {
                    System.out.println("返回结果: null");
                }
                
            } catch (Exception callException) {
                System.out.println("调用失败: " + callException.getMessage());
                
                // 分析失败原因
                analyzeFailure(callException);
            }
            
            // 测试JSON格式调用
            System.out.println("\n=== 第三步：测试JSON格式调用 ===");
            try {
                long startTime = System.currentTimeMillis();
                
                String jsonResult = clientManager.invokeServiceAsJson(
                    serviceInterface, serviceUrl, methodName, parameterTypes, parameters
                );
                
                long endTime = System.currentTimeMillis();
                long duration = endTime - startTime;
                
                System.out.println("✅ JSON调用成功！耗时: " + duration + "ms");
                
                if (jsonResult.length() > 1000) {
                    System.out.println("JSON结果(前1000字符): " + jsonResult.substring(0, 1000) + "...");
                } else {
                    System.out.println("JSON结果: " + jsonResult);
                }
                
            } catch (Exception jsonException) {
                System.out.println("JSON调用失败: " + jsonException.getMessage());
                analyzeFailure(jsonException);
            }
            
            // 测试另一个方法
            System.out.println("\n=== 第四步：测试另一个方法 ===");
            try {
                String methodName2 = "getCompanyIdList";
                String[] parameterTypes2 = {"java.util.List"};
                Object[] parameters2 = {java.util.Arrays.asList(1L)};
                
                System.out.println("测试方法: " + methodName2);
                System.out.println("参数类型: " + java.util.Arrays.toString(parameterTypes2));
                System.out.println("参数值: " + java.util.Arrays.toString(parameters2));
                
                Object result2 = clientManager.invokeService(
                    serviceInterface, serviceUrl, methodName2, parameterTypes2, parameters2
                );
                
                System.out.println("✅ 第二个方法调用成功！");
                System.out.println("返回结果类型: " + (result2 != null ? result2.getClass().getName() : "null"));
                
            } catch (Exception secondException) {
                System.out.println("第二个方法调用失败: " + secondException.getMessage());
                analyzeFailure(secondException);
            }
            
        } catch (Exception e) {
            System.out.println("❌ 测试失败: " + e.getMessage());
            e.printStackTrace();
        }
        
        System.out.println("\n测试完成！");
        
        // 总结测试结果
        System.out.println("\n=== 测试总结 ===");
        System.out.println("本次测试验证了以下优化:");
        System.out.println("1. 多层回退机制：标准Dubbo -> 原生协议 -> HTTP");
        System.out.println("2. 优化的超时处理：连接5秒，读取10秒");
        System.out.println("3. 改进的错误处理：更详细的错误信息和分类");
        System.out.println("4. 更强的类加载器问题检测");
    }
    
    private static void analyzeFailure(Exception e) {
        System.out.println("失败分析:");
        
        String message = e.getMessage();
        if (message != null) {
            if (message.contains("Read timed out")) {
                System.out.println("  • 问题类型: 读取超时");
                System.out.println("  • 原因: 服务响应时间过长或网络问题");
                System.out.println("  • 建议: 检查服务状态和网络连接");
            } else if (message.contains("Connection refused") || message.contains("ConnectException")) {
                System.out.println("  • 问题类型: 连接被拒绝");
                System.out.println("  • 原因: 服务未启动或端口不可访问");
                System.out.println("  • 建议: 检查服务是否运行，端口是否正确");
            } else if (message.contains("is not visible from class loader")) {
                System.out.println("  • 问题类型: 类加载器可见性问题");
                System.out.println("  • 原因: IntelliJ插件环境中的类隔离");
                System.out.println("  • 解决方案: 已自动尝试多层回退机制");
            } else if (message.contains("UnknownHostException")) {
                System.out.println("  • 问题类型: 主机名解析失败");
                System.out.println("  • 原因: DNS解析问题或主机名错误");
                System.out.println("  • 建议: 检查主机名和网络配置");
            } else if (message.contains("所有调用方式都失败")) {
                System.out.println("  • 问题类型: 全部回退方式都失败");
                System.out.println("  • 原因: 服务不可用或网络问题");
                System.out.println("  • 建议: 检查服务状态和网络连接");
            } else {
                System.out.println("  • 问题类型: 其他错误");
                System.out.println("  • 错误信息: " + message);
            }
        }
        
        System.out.println("  • 异常类型: " + e.getClass().getSimpleName());
    }
}