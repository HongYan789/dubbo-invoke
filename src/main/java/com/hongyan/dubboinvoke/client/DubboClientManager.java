package com.hongyan.dubboinvoke.client;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.alibaba.dubbo.config.ApplicationConfig;
import com.alibaba.dubbo.config.ReferenceConfig;
import com.alibaba.dubbo.config.RegistryConfig;
import com.alibaba.dubbo.rpc.service.GenericService;
import com.hongyan.dubboinvoke.util.OperationLogger;
import com.hongyan.dubboinvoke.util.ModuleOpener;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Dubbo客户端管理器
 * 负责管理Dubbo服务的连接和调用，支持HTTP备选方案
 */
public class DubboClientManager {
    
    private static volatile DubboClientManager INSTANCE;
    private static final OperationLogger logger = OperationLogger.getInstance();
    
    // 缓存已创建的服务引用
    private final Map<String, GenericService> serviceCache = new ConcurrentHashMap<>();
    
    // Socket客户端（备选方案）
    private final ObjectMapper objectMapper;
    
    // 应用配置
    private ApplicationConfig application;
    
    // 注册中心地址
    private volatile String registryAddress;
    
    // 是否使用Socket备选方案
    private volatile boolean useSocketFallback = false;
    
    private DubboClientManager() {
        logger.log("初始化DubboClientManager");
        logger.logSystemInfo();
        
        // 初始化HTTP客户端作为备选方案
        this.objectMapper = createOptimizedObjectMapper();
        
        // 提前尝试打开JDK模块（尽力而为）
        try { 
            ModuleOpener.openIfNeeded(); 
            logger.log("JDK模块打开尝试完成");
        } catch (Throwable e) {
            logger.log("JDK模块打开失败: " + e.getMessage());
            logger.logException(e);
        }
    }
    
    public static DubboClientManager getInstance() {
        if (INSTANCE == null) {
            synchronized (DubboClientManager.class) {
                if (INSTANCE == null) {
                    INSTANCE = new DubboClientManager();
                }
            }
        }
        return INSTANCE;
    }
    
    /**
     * 初始化Dubbo系统属性（完全避免创建Dubbo对象）
     */
    private void initializeConfigs() {
        try {
            logger.log("开始设置Dubbo系统属性");
            
            // 只设置系统属性，不创建任何Dubbo对象
            System.setProperty("dubbo.application.logger", "slf4j");
            System.setProperty("dubbo.reference.check", "false");
            System.setProperty("dubbo.consumer.check", "false");
            System.setProperty("dubbo.registry.check", "false");
            System.setProperty("dubbo.application.metadata-type", "local");
            System.setProperty("dubbo.metadata-report.check", "false");
            System.setProperty("dubbo.config-center.check", "false");
            System.setProperty("dubbo.application.auto-register", "false");
            
            logger.log("Dubbo系统属性设置完成");
            
        } catch (Exception e) {
            logger.log("设置Dubbo系统属性失败: " + e.getMessage());
            // 不抛出异常，让程序继续运行
        }
    }
    
    /**
     * 更新注册中心配置（完全避免创建RegistryConfig对象）
     */
    public void updateRegistryConfig(String registryAddress) {
        logger.log("更新注册中心配置: " + registryAddress);
        
        // 确保系统属性已设置
        initializeConfigs();
        
        // 不创建RegistryConfig对象，只记录地址用于后续使用
        this.registryAddress = registryAddress;
        
        if (registryAddress != null && !registryAddress.trim().isEmpty()) {
            logger.log("注册中心地址已记录: " + registryAddress);
        } else {
            logger.log("注册中心地址为空，将使用直连模式");
        }
        
        // 清空缓存，强制重新创建连接
        serviceCache.clear();
        logger.log("服务缓存已清空");
    }
    
    /**
     * 获取泛化服务引用（优先使用HTTP备选方案）
     */
    public GenericService getGenericService(String serviceInterface, String serviceUrl) {
        logger.log("开始获取泛化服务引用");
        logger.log("服务接口: " + serviceInterface);
        logger.log("服务地址: " + serviceUrl);
        
        // 检查是否为注册中心模式
        if (serviceUrl == null || serviceUrl.trim().isEmpty()) {
            logger.log("检测到注册中心模式，但当前插件仅支持直连模式");
            throw new RuntimeException("当前插件仅支持直连模式调用，请在Service Address中输入具体的服务地址（如：dubbo://10.7.8.50:16002）而不是注册中心地址");
        }
        
        String cacheKey = serviceInterface + "@" + serviceUrl;
        logger.log("缓存键: " + cacheKey);
        
        return serviceCache.computeIfAbsent(cacheKey, key -> {
            logger.log("创建新的服务引用: " + key);
            
            // 直接使用Socket备选方案，避免Dubbo扩展机制问题
            try {
                logger.log("使用Socket备选方案创建服务引用");
                useSocketFallback = true;
                GenericService socketService = createSocketGenericService(serviceInterface, serviceUrl);
                logger.log("Socket备选方案成功");
                return socketService;
            } catch (Exception e) {
                logger.log("Socket备选方案失败: " + e.getMessage());
                logger.logException(e);
                throw new RuntimeException("无法创建服务引用: " + e.getMessage(), e);
            }
        });
    }
    
    /**
     * 创建Socket代理的GenericService
     */
    private GenericService createSocketGenericService(String serviceInterface, String serviceUrl) {
        logger.log("创建Socket代理GenericService: " + serviceInterface);
        
        return new GenericService() {
            @Override
            public Object $invoke(String method, String[] parameterTypes, Object[] args) {
                logger.log("Socket代理调用: " + method);
                try {
                    return invokeViaSocket(serviceUrl, serviceInterface, method, parameterTypes, args);
                } catch (Exception e) {
                    logger.log("Socket调用异常: " + e.getMessage());
                    logger.logException(e);
                    throw new RuntimeException("Socket调用失败: " + e.getMessage(), e);
                }
            }
        };
    }
    
    /**
     * Socket备选方案调用
     */
    private Object invokeViaSocket(String serviceUrl, String serviceInterface, 
                                  String methodName, String[] parameterTypes, 
                                  Object[] parameters) throws Exception {
        logger.log("启用Socket备选方案调用Dubbo服务");
        logger.log("服务URL: " + serviceUrl);
        logger.log("服务接口: " + serviceInterface);
        logger.log("方法名: " + methodName);
        
        // 解析服务URL获取host和port
        String[] hostPort = parseServiceUrl(serviceUrl);
        String host = hostPort[0];
        int port = Integer.parseInt(hostPort[1]);
        
        try {
            NettyDubboClient socketClient = new NettyDubboClient(host, port);
            
            // 转换参数类型
            Class<?>[] paramTypes = null;
            if (parameterTypes != null) {
                paramTypes = new Class<?>[parameterTypes.length];
                for (int i = 0; i < parameterTypes.length; i++) {
                    paramTypes[i] = Class.forName(parameterTypes[i]);
                }
            }
            
            return socketClient.invoke(serviceInterface, methodName, paramTypes, parameters);
        } catch (Exception e) {
            logger.log("Socket调用失败: " + e.getMessage());
            logger.logException(e);
            throw new RuntimeException("Socket调用失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 解析服务URL获取host和port
     */
    private String[] parseServiceUrl(String serviceUrl) {
        // 处理null或空字符串的情况
        if (serviceUrl == null || serviceUrl.trim().isEmpty()) {
            logger.log("服务URL为空，无法使用Socket方案，需要通过注册中心发现服务");
            throw new IllegalArgumentException("Socket方案需要明确的服务地址，注册中心模式下无法使用Socket调用");
        }
        
        String hostPort;
        
        // 解析dubbo://协议的URL
        if (serviceUrl.startsWith("dubbo://")) {
            hostPort = serviceUrl.substring(8); // 移除"dubbo://"
            // 移除路径部分，只保留host:port
            if (hostPort.contains("/")) {
                hostPort = hostPort.substring(0, hostPort.indexOf("/"));
            }
        } else {
            // 默认假设是host:port格式
            hostPort = serviceUrl;
        }
        
        if (!hostPort.contains(":")) {
            return new String[]{hostPort, "20880"}; // 默认Dubbo端口
        }
        
        String[] parts = hostPort.split(":");
        return new String[]{parts[0], parts[1]};
    }
    
    /**
     * 执行泛化调用
     */
    public Object invokeService(String serviceInterface, String serviceUrl, 
                               String methodName, String[] parameterTypes, Object[] parameters) {
        logger.log("开始执行Dubbo调用");
        logger.logDubboInvoke(serviceInterface, serviceUrl, methodName, parameterTypes, parameters);
        
        try {
            GenericService genericService = getGenericService(serviceInterface, serviceUrl);
            logger.log("泛化服务获取成功，开始调用方法: " + methodName);
            
            Object result = genericService.$invoke(methodName, parameterTypes, parameters);
            logger.log("Dubbo调用成功，返回结果类型: " + (result != null ? result.getClass().getName() : "null"));
            return result;
        } catch (Exception e) {
            logger.log("Dubbo调用失败: " + e.getMessage());
            logger.logException(e);
            throw new RuntimeException("Dubbo调用失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 执行泛化调用并返回JSON格式结果
     */
    /**
     * 创建优化的ObjectMapper，用于处理复杂类型和集合
     */
    private ObjectMapper createOptimizedObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        
        // 配置序列化特性
        mapper.enable(SerializationFeature.INDENT_OUTPUT); // 格式化输出
        mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS); // 允许空对象
        mapper.enable(SerializationFeature.WRITE_ENUMS_USING_TO_STRING); // 枚举使用toString
        mapper.enable(SerializationFeature.WRITE_NULL_MAP_VALUES); // 写入null值的Map
        
        // 配置反序列化特性
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES); // 忽略未知属性
        mapper.enable(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT); // 空字符串作为null
        
        // 配置生成器特性
        mapper.getFactory().enable(JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN); // BigDecimal不使用科学计数法
        
        logger.log("ObjectMapper配置完成，支持复杂类型序列化");
        return mapper;
    }
    
    public String invokeServiceAsJson(String serviceInterface, String serviceUrl,
                                     String methodName, String[] parameterTypes, Object[] parameters) {
        logger.log("开始执行JSON格式的Dubbo调用");
        
        try {
            Object result = invokeService(serviceInterface, serviceUrl, methodName, parameterTypes, parameters);
            logger.log("原始调用成功，开始序列化为JSON，结果类型: " + (result != null ? result.getClass().getName() : "null"));
            
            // 使用优化的ObjectMapper进行序列化
            String jsonResult = objectMapper.writeValueAsString(result);
            logger.log("JSON序列化成功，结果长度: " + jsonResult.length());
            return jsonResult;
        } catch (Exception e) {
            logger.log("JSON格式调用失败: " + e.getMessage());
            logger.logException(e);
            
            // 返回错误信息的JSON格式
            Map<String, Object> errorResult = Map.of(
                "error", true,
                "message", e.getMessage(),
                "type", e.getClass().getSimpleName()
            );
            try {
                String errorJson = objectMapper.writeValueAsString(errorResult);
                logger.log("错误信息JSON序列化成功");
                return errorJson;
            } catch (Exception jsonException) {
                logger.log("错误信息JSON序列化失败: " + jsonException.getMessage());
                return "{\"error\": true, \"message\": \"Failed to serialize error response\"}";
            }
        }
    }
    
    /**
     * 清理资源
     */
    public void cleanup() {
        logger.log("开始清理DubboClientManager资源");
        serviceCache.clear();
        logger.log("服务缓存已清理");
        logger.flush(); // 确保日志写入文件
    }
    
    /**
     * 测试连接
     */
    public boolean testConnection(String serviceInterface, String serviceUrl) {
        logger.log("开始测试连接");
        logger.log("服务接口: " + serviceInterface);
        logger.log("服务地址: " + serviceUrl);
        
        try {
            getGenericService(serviceInterface, serviceUrl);
            logger.log("连接测试成功");
            return true;
        } catch (Exception e) {
            logger.log("连接测试失败: " + e.getMessage());
            logger.logException(e);
            return false;
        }
    }
}