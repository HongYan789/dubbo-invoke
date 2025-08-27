package com.hongyan.dubboinvoke.client;

import com.hongyan.dubboinvoke.util.OperationLogger;
import com.alibaba.dubbo.config.ApplicationConfig;
import com.alibaba.dubbo.config.ReferenceConfig;
import com.alibaba.dubbo.config.RegistryConfig;
import com.alibaba.dubbo.rpc.service.GenericService;

import java.net.Socket;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 支持注册中心的Dubbo客户端
 * 能够处理ZooKeeper和Nacos注册中心，自动发现服务提供者
 */
public class RegistryAwareDubboClient {
    
    private static volatile RegistryAwareDubboClient INSTANCE;
    private static final OperationLogger logger = OperationLogger.getInstance();
    
    private final ApplicationConfig application;
    private final Map<String, GenericService> serviceCache = new ConcurrentHashMap<>();
    
    private RegistryAwareDubboClient() {
        logger.log("初始化支持注册中心的Dubbo客户端");
        
        this.application = new ApplicationConfig();
        this.application.setName("dubbo-invoke-registry-client");
        
        logger.log("支持注册中心的Dubbo客户端初始化完成");
    }
    
    public static RegistryAwareDubboClient getInstance() {
        if (INSTANCE == null) {
            synchronized (RegistryAwareDubboClient.class) {
                if (INSTANCE == null) {
                    INSTANCE = new RegistryAwareDubboClient();
                }
            }
        }
        return INSTANCE;
    }
    
    /**
     * 使用注册中心调用服务
     */
    public Object invokeService(String serviceInterface, String registryUrl, 
                               String methodName, String[] parameterTypes, Object[] parameters) {
        logger.log("开始执行注册中心模式的Dubbo调用");
        logger.log("服务接口: " + serviceInterface);
        logger.log("注册中心地址: " + registryUrl);
        logger.log("方法名: " + methodName);
        
        try {
            GenericService genericService = getGenericServiceFromRegistry(serviceInterface, registryUrl);
            
            logger.log("从注册中心获取服务引用成功，开始调用方法");
            
            // 增加调用超时控制
            Object result;
            try {
                result = genericService.$invoke(methodName, parameterTypes, parameters);
            } catch (Exception invokeException) {
                // 对调用异常进行分类处理
                String errorMsg = invokeException.getMessage();
                if (errorMsg != null) {
                    if (errorMsg.contains("timeout") || errorMsg.contains("Timeout")) {
                        throw new RuntimeException("服务调用超时，请检查服务提供者状态: " + errorMsg, invokeException);
                    } else if (errorMsg.contains("NoSuchMethodException")) {
                        throw new RuntimeException("方法签名不匹配，请检查参数类型: " + errorMsg, invokeException);
                    } else if (errorMsg.contains("No provider")) {
                        throw new RuntimeException("服务提供者不可用，请检查服务是否正常运行: " + errorMsg, invokeException);
                    }
                }
                throw invokeException;
            }
            
            logger.log("注册中心模式调用成功，结果类型: " + (result != null ? result.getClass().getName() : "null"));
            return result;
            
        } catch (Exception e) {
            logger.log("注册中心模式调用失败: " + e.getMessage());
            logger.logException(e);
            throw new RuntimeException("注册中心模式调用失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 从注册中心获取泛化服务引用
     */
    private GenericService getGenericServiceFromRegistry(String serviceInterface, String registryUrl) {
        String cacheKey = serviceInterface + "@registry:" + registryUrl;
        
        return serviceCache.computeIfAbsent(cacheKey, key -> {
            logger.log("从注册中心创建新的服务引用: " + serviceInterface);
            
            try {
                return createRegistryGenericService(serviceInterface, registryUrl);
            } catch (Exception e) {
                logger.log("从注册中心创建服务引用失败: " + e.getMessage());
                logger.logException(e);
                throw new RuntimeException("无法从注册中心创建服务引用: " + e.getMessage(), e);
            }
        });
    }
    
    /**
     * 创建基于注册中心的泛化服务引用（优化版，处理类加载器问题）
     */
    private GenericService createRegistryGenericService(String serviceInterface, String registryUrl) {
        logger.log("创建基于注册中心的Dubbo泛化服务引用: " + serviceInterface);
        logger.log("注册中心URL: " + registryUrl);
        
        // 保存当前线程的类加载器
        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        
        try {
            // 设置类加载器为插件类加载器
            ClassLoader pluginClassLoader = this.getClass().getClassLoader();
            Thread.currentThread().setContextClassLoader(pluginClassLoader);
            logger.log("已设置注册中心模式的类加载器");
            
            // 创建引用配置
            ReferenceConfig<GenericService> reference = new ReferenceConfig<GenericService>();
            
            // 设置应用配置
            reference.setApplication(application);
            
            // 设置服务接口
            reference.setInterface(serviceInterface);
            reference.setGeneric(true);
            reference.setCheck(false);
            reference.setTimeout(10000);  // 优化：减少调用超时为10秒
            reference.setRetries(0);
            reference.setConnections(1); // 限制连接数
            reference.setLazy(true);     // 延迟初始化
            
            // 设置注册中心配置
            RegistryConfig registry = new RegistryConfig();
            registry.setAddress(registryUrl);
            registry.setCheck(false);
            registry.setTimeout(3000); // 优化：减少注册中心连接超时为3秒
            
            // 根据注册中心类型设置特定参数
            if (registryUrl.startsWith("zookeeper://")) {
                logger.log("配置ZooKeeper注册中心");
                registry.setProtocol("zookeeper");
                // ZooKeeper特定的超时和重试配置
                reference.setTimeout(8000); // ZooKeeper调用超时优化为8秒
            } else if (registryUrl.startsWith("nacos://")) {
                logger.log("配置Nacos注册中心");
                registry.setProtocol("nacos");
            } else {
                logger.log("使用默认注册中心配置");
            }
            
            reference.setRegistry(registry);
            
            // 获取服务引用（增加超时控制）
            logger.log("开始从注册中心获取服务引用...");
            GenericService genericService;
            try {
                // 使用超时控制获取服务引用
                genericService = reference.get();
                if (genericService == null) {
                    throw new RuntimeException("服务引用获取失败：返回空引用");
                }
                logger.log("基于注册中心的Dubbo泛化服务引用创建成功");
            } catch (Exception e) {
                logger.log("从注册中心获取服务引用失败: " + e.getMessage());
                
                // 对常见问题提供更好的错误提示
                if (e.getMessage() != null) {
                    if (e.getMessage().contains("timeout") || e.getMessage().contains("Timeout")) {
                        throw new RuntimeException("注册中心连接超时，请检查网络和注册中心状态: " + registryUrl, e);
                    } else if (e.getMessage().contains("Connection refused") || e.getMessage().contains("connect")) {
                        throw new RuntimeException("无法连接到注册中心，请检查地址和端口: " + registryUrl, e);
                    } else if (e.getMessage().contains("No provider")) {
                        throw new RuntimeException("注册中心中未找到服务提供者: " + serviceInterface, e);
                    }
                }
                throw new RuntimeException("注册中心服务引用创建失败: " + e.getMessage(), e);
            }
            
            return genericService;
            
        } catch (Exception e) {
            logger.log("创建基于注册中心的Dubbo泛化服务引用失败: " + e.getMessage());
            logger.logException(e);
            
            // 对类加载器问题提供更好的提示
            if (e.getMessage() != null && e.getMessage().contains("is not visible from class loader")) {
                throw new RuntimeException("注册中心模式下的Dubbo类加载器可见性问题，请检查插件依赖配置: " + e.getMessage(), e);
            }
            
            throw new RuntimeException("无法创建基于注册中心的Dubbo服务引用: " + e.getMessage(), e);
        } finally {
            // 恢复原有的类加载器
            Thread.currentThread().setContextClassLoader(originalClassLoader);
            logger.log("注册中心模式已恢复原有的类加载器");
        }
    }
    
    /**
     * 测试注册中心连接
     */
    public boolean testRegistryConnection(String registryUrl) {
        logger.log("开始测试注册中心连接: " + registryUrl);
        
        try {
            if (registryUrl.startsWith("zookeeper://")) {
                return testZooKeeperConnection(registryUrl);
            } else if (registryUrl.startsWith("nacos://")) {
                return testNacosConnection(registryUrl);
            } else {
                logger.log("不支持的注册中心类型: " + registryUrl);
                return false;
            }
        } catch (Exception e) {
            logger.log("测试注册中心连接失败: " + e.getMessage());
            logger.logException(e);
            return false;
        }
    }
    
    /**
     * 测试ZooKeeper连接
     */
    private boolean testZooKeeperConnection(String zookeeperUrl) {
        logger.log("测试ZooKeeper连接: " + zookeeperUrl);
        
        try {
            // 解析ZooKeeper地址
            String address = zookeeperUrl.replace("zookeeper://", "");
            String[] parts = address.split(":");
            String host = parts[0];
            int port = parts.length > 1 ? Integer.parseInt(parts[1]) : 2181;
            
            logger.log("ZooKeeper地址: " + host + ":" + port);
            
            // 简单的TCP连接测试
            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress(host, port), 5000);
                logger.log("ZooKeeper连接测试成功");
                return true;
            }
        } catch (Exception e) {
            logger.log("ZooKeeper连接测试失败: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 测试Nacos连接
     */
    private boolean testNacosConnection(String nacosUrl) {
        logger.log("测试Nacos连接: " + nacosUrl);
        
        try {
            // 解析Nacos地址
            String address = nacosUrl.replace("nacos://", "");
            String[] parts = address.split(":");
            String host = parts[0];
            int port = parts.length > 1 ? Integer.parseInt(parts[1]) : 8848;
            
            logger.log("Nacos地址: " + host + ":" + port);
            
            // 简单的TCP连接测试
            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress(host, port), 5000);
                logger.log("Nacos连接测试成功");
                return true;
            }
        } catch (Exception e) {
            logger.log("Nacos连接测试失败: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 清理资源
     */
    public void cleanup() {
        logger.log("开始清理注册中心客户端资源");
        serviceCache.clear();
        logger.log("注册中心客户端资源清理完成");
    }
}