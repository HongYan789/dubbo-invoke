package com.hongyan.dubboinvoke;

import com.hongyan.dubboinvoke.client.DubboClientManager;
import com.hongyan.dubboinvoke.client.RegistryAwareDubboClient;



/**
 * 符合技术规范的Dubbo调用测试
 * 
 * 技术规范合规性：
 * 1. 使用标准Dubbo API：ReferenceConfig<GenericService>
 * 2. 禁止自定义Socket/Netty实现：已完全移除违规代码
 * 3. 三层回退机制：标准Dubbo -> 注册中心模式 -> HTTP回退
 * 4. 智能服务地址处理：支持直连和注册中心模式
 * 5. 增强的错误处理和诊断
 * 6. 完整的连接测试和验证
 */
public class FinalOptimizedDubboTest {
    
    public static void main(String[] args) {
        System.out.println("=== 开始符合技术规范的Dubbo调用测试 ===");
        
        try {
            // 获取DubboClientManager实例
            DubboClientManager clientManager = DubboClientManager.getInstance();
            System.out.println("✅ DubboClientManager初始化成功（符合技术规范版）");
            
            // 测试场景1：直连模式
            testDirectConnectionMode(clientManager);
            
            // 测试场景2：ZooKeeper注册中心模式
            testZooKeeperRegistryMode(clientManager);
            
            // 测试场景3：Nacos注册中心模式
            testNacosRegistryMode(clientManager);
            
            // 测试场景4：网络连接测试
            testNetworkConnectivity();
            
        } catch (Exception e) {
            System.out.println("❌ 测试失败: " + e.getMessage());
            e.printStackTrace();
        }
        
        System.out.println("\n=== 测试完成 ===");
    }
    
    /**
     * 测试直连模式
     */
    private static void testDirectConnectionMode(DubboClientManager clientManager) {
        System.out.println("\n=== 测试场景1：直连模式 ===");
        
        String serviceInterface = "com.jzt.zhcai.user.companyinfo.CompanyInfoDubboApi";
        String serviceUrl = "dubbo://10.7.8.50:16002";
        String methodName = "queryCompanyByCompanyId";
        String[] parameterTypes = {"java.lang.Long"};
        Object[] parameters = {1L};
        
        System.out.println("服务接口: " + serviceInterface);
        System.out.println("服务地址: " + serviceUrl);
        System.out.println("方法名: " + methodName);
        System.out.println("参数: " + java.util.Arrays.toString(parameters));
        
        // 连接测试
        System.out.println("\n--- 连接测试 ---");
        boolean connected = clientManager.testConnection(serviceInterface, serviceUrl);
        System.out.println("连接测试结果: " + (connected ? "✅ 成功" : "❌ 失败"));
        
        // 服务调用测试
        System.out.println("\n--- 服务调用测试 ---");
        try {
            long startTime = System.currentTimeMillis();
            
            Object result = clientManager.invokeService(
                serviceInterface, serviceUrl, methodName, parameterTypes, parameters
            );
            
            long duration = System.currentTimeMillis() - startTime;
            System.out.println("✅ 直连模式调用成功！耗时: " + duration + "ms");
            System.out.println("返回结果类型: " + (result != null ? result.getClass().getName() : "null"));
            
            if (result != null) {
                String resultStr = result.toString();
                if (resultStr.length() > 300) {
                    System.out.println("返回结果(前300字符): " + resultStr.substring(0, 300) + "...");
                } else {
                    System.out.println("返回结果: " + resultStr);
                }
            }
            
        } catch (Exception e) {
            System.out.println("❌ 直连模式调用失败: " + e.getMessage());
            analyzeFailure(e);
        }
    }
    
    /**
     * 测试ZooKeeper注册中心模式
     */
    private static void testZooKeeperRegistryMode(DubboClientManager clientManager) {
        System.out.println("\n=== 测试场景2：ZooKeeper注册中心模式 ===");
        
        String serviceInterface = "com.jzt.zhcai.user.companyinfo.CompanyInfoDubboApi";
        String registryUrl = "zookeeper://10.7.8.40:2181";
        String methodName = "queryCompanyByCompanyId";
        String[] parameterTypes = {"java.lang.Long"};
        Object[] parameters = {1L};
        
        System.out.println("服务接口: " + serviceInterface);
        System.out.println("注册中心地址: " + registryUrl);
        System.out.println("方法名: " + methodName);
        
        // 更新注册中心配置
        clientManager.updateRegistryConfig(registryUrl);
        
        // 注册中心连接测试
        System.out.println("\n--- 注册中心连接测试 ---");
        try {
            RegistryAwareDubboClient registryClient = RegistryAwareDubboClient.getInstance();
            boolean registryConnected = registryClient.testRegistryConnection(registryUrl);
            System.out.println("注册中心连接测试结果: " + (registryConnected ? "✅ 成功" : "❌ 失败"));
        } catch (Exception e) {
            System.out.println("❌ 注册中心连接测试失败: " + e.getMessage());
        }
        
        // 服务调用测试
        System.out.println("\n--- ZooKeeper模式服务调用测试 ---");
        try {
            long startTime = System.currentTimeMillis();
            
            Object result = clientManager.invokeService(
                serviceInterface, null, methodName, parameterTypes, parameters
            );
            
            long duration = System.currentTimeMillis() - startTime;
            System.out.println("✅ ZooKeeper模式调用成功！耗时: " + duration + "ms");
            System.out.println("返回结果类型: " + (result != null ? result.getClass().getName() : "null"));
            
        } catch (Exception e) {
            System.out.println("❌ ZooKeeper模式调用失败: " + e.getMessage());
            analyzeFailure(e);
        }
    }
    
    /**
     * 测试Nacos注册中心模式
     */
    private static void testNacosRegistryMode(DubboClientManager clientManager) {
        System.out.println("\n=== 测试场景3：Nacos注册中心模式 ===");
        
        String serviceInterface = "com.jzt.zhcai.user.companyinfo.CompanyInfoDubboApi";
        String registryUrl = "nacos://yjj-nacos.it.yyjzt.com:28848";
        String methodName = "queryCompanyByCompanyId";
        String[] parameterTypes = {"java.lang.Long"};
        Object[] parameters = {1L};
        
        System.out.println("服务接口: " + serviceInterface);
        System.out.println("注册中心地址: " + registryUrl);
        System.out.println("方法名: " + methodName);
        
        // 更新注册中心配置
        clientManager.updateRegistryConfig(registryUrl);
        
        // 注册中心连接测试
        System.out.println("\n--- Nacos连接测试 ---");
        try {
            RegistryAwareDubboClient registryClient = RegistryAwareDubboClient.getInstance();
            boolean registryConnected = registryClient.testRegistryConnection(registryUrl);
            System.out.println("Nacos连接测试结果: " + (registryConnected ? "✅ 成功" : "❌ 失败"));
        } catch (Exception e) {
            System.out.println("❌ Nacos连接测试失败: " + e.getMessage());
        }
        
        // 服务调用测试
        System.out.println("\n--- Nacos模式服务调用测试 ---");
        try {
            long startTime = System.currentTimeMillis();
            
            Object result = clientManager.invokeService(
                serviceInterface, null, methodName, parameterTypes, parameters
            );
            
            long duration = System.currentTimeMillis() - startTime;
            System.out.println("✅ Nacos模式调用成功！耗时: " + duration + "ms");
            System.out.println("返回结果类型: " + (result != null ? result.getClass().getName() : "null"));
            
        } catch (Exception e) {
            System.out.println("❌ Nacos模式调用失败: " + e.getMessage());
            analyzeFailure(e);
        }
    }
    
    /**
     * 测试网络连接性
     */
    private static void testNetworkConnectivity() {
        System.out.println("\n=== 测试场景4：网络连接测试 ===");
        
        // 测试各种网络连接
        testTcpConnection("10.7.8.50", 16002, "Dubbo服务端口");
        testTcpConnection("10.7.8.40", 2181, "ZooKeeper端口");
        testTcpConnection("yjj-nacos.it.yyjzt.com", 28848, "Nacos端口");
    }
    
    /**
     * TCP连接测试
     */
    private static void testTcpConnection(String host, int port, String description) {
        System.out.println("\n--- " + description + " 连接测试 ---");
        System.out.println("目标: " + host + ":" + port);
        
        try (java.net.Socket socket = new java.net.Socket()) {
            long startTime = System.currentTimeMillis();
            socket.connect(new java.net.InetSocketAddress(host, port), 5000);
            long duration = System.currentTimeMillis() - startTime;
            
            System.out.println("✅ " + description + "连接成功！耗时: " + duration + "ms");
        } catch (Exception e) {
            System.out.println("❌ " + description + "连接失败: " + e.getMessage());
            
            if (e instanceof java.net.ConnectException) {
                System.out.println("  原因分析: 连接被拒绝，可能是服务未启动或端口不可访问");
            } else if (e instanceof java.net.SocketTimeoutException) {
                System.out.println("  原因分析: 连接超时，可能是网络问题或防火墙阻止");
            } else if (e instanceof java.net.UnknownHostException) {
                System.out.println("  原因分析: 主机名无法解析，可能是DNS问题");
            }
        }
    }
    
    /**
     * 失败分析
     */
    private static void analyzeFailure(Exception e) {
        System.out.println("\n🔍 失败分析:");
        
        String message = e.getMessage();
        if (message != null) {
            if (message.contains("Read timed out")) {
                System.out.println("  • 问题类型: 读取超时");
                System.out.println("  • 可能原因: 服务响应时间过长或网络延迟");
                System.out.println("  • 建议: 检查服务性能和网络状况");
            } else if (message.contains("Connection refused") || message.contains("ConnectException")) {
                System.out.println("  • 问题类型: 连接被拒绝");
                System.out.println("  • 可能原因: 服务未启动或端口不可访问");
                System.out.println("  • 建议: 确认服务运行状态和端口配置");
            } else if (message.contains("is not visible from class loader")) {
                System.out.println("  • 问题类型: 类加载器可见性问题");
                System.out.println("  • 可能原因: IntelliJ插件环境中的类隔离");
                System.out.println("  • 解决方案: 已自动尝试多层回退机制");
            } else if (message.contains("UnknownHostException")) {
                System.out.println("  • 问题类型: 主机名解析失败");
                System.out.println("  • 可能原因: DNS解析问题或主机名错误");
                System.out.println("  • 建议: 检查网络配置和主机名");
            } else if (message.contains("所有调用方式都失败")) {
                System.out.println("  • 问题类型: 全部回退方式都失败");
                System.out.println("  • 可能原因: 服务完全不可用或严重网络问题");
                System.out.println("  • 建议: 检查服务状态和网络连通性");
            } else if (message.contains("服务地址不能为空")) {
                System.out.println("  • 问题类型: 服务地址配置问题");
                System.out.println("  • 可能原因: 直连地址和注册中心地址都未配置");
                System.out.println("  • 建议: 配置正确的服务地址或注册中心地址");
            } else {
                System.out.println("  • 问题类型: 其他错误");
                System.out.println("  • 错误信息: " + message);
            }
        }
        
        System.out.println("  • 异常类型: " + e.getClass().getSimpleName());
        
        // 检查是否有符合规范的三层回退错误信息
        if (message != null && message.contains("标准Dubbo") && message.contains("已尝试")) {
            System.out.println("  • 回退状态: 已尝试多种调用方式（符合技术规范）");
            if (message.contains("注册中心模式")) {
                System.out.println("    ✓ 注册中心模式已尝试");
            }
            if (message.contains("HTTP回退")) {
                System.out.println("    ✓ HTTP回退已尝试");
            }
            System.out.println("    ✅ 已移除违规的原生协议调用，符合技术规范要求");
        }
    }
}