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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Dubbo客户端管理器
 * 负责管理Dubbo服务的连接和调用，使用简化的Dubbo 2.6 API
 */
public class DubboClientManager {
    
    private static volatile DubboClientManager INSTANCE;
    private static final OperationLogger logger = OperationLogger.getInstance();
    
    // 缓存已创建的服务引用
    private final Map<String, GenericService> serviceCache = new ConcurrentHashMap<>();
    
    // JSON序列化工具
    private final ObjectMapper objectMapper;
    
    // 应用配置
    private ApplicationConfig application;
    
    // 注册中心地址
    private volatile String registryAddress;
    
    private DubboClientManager() {
        logger.log("初始化DubboClientManager（简化版）");
        logger.logSystemInfo();
        
        // 初始化JSON序列化工具
        this.objectMapper = createOptimizedObjectMapper();
        
        // 提前尝试打开JDK模块（尽力而为）
        try { 
            ModuleOpener.openIfNeeded(); 
            logger.log("JDK模块打开尝试完成");
        } catch (Throwable e) {
            logger.log("JDK模块打开失败: " + e.getMessage());
            logger.logException(e);
        }
        
        // 初始化Dubbo应用配置
        initializeDubboApplication();
        
        logger.log("DubboClientManager初始化完成");
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
     * 初始化Dubbo应用配置
     */
    private void initializeDubboApplication() {
        try {
            logger.log("开始初始化简化的Dubbo应用配置");
            
            // 创建应用配置
            application = new ApplicationConfig();
            application.setName("dubbo-invoke-plugin");
            
            logger.log("简化的Dubbo应用配置初始化成功");
            
        } catch (Exception e) {
            logger.log("初始化Dubbo应用配置失败: " + e.getMessage());
            logger.logException(e);
            throw new RuntimeException("无法初始化Dubbo应用配置", e);
        }
    }
    
    /**
     * 更新注册中心配置
     */
    public void updateRegistryConfig(String registryAddress) {
        logger.log("更新注册中心配置: " + registryAddress);
        
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
     * 获取泛化服务引用
     */
    public GenericService getGenericService(String serviceInterface, String serviceUrl) {
        logger.log("开始获取泛化服务引用");
        logger.log("服务接口: " + serviceInterface);
        logger.log("服务地址: " + serviceUrl);
        
        // 处理注册中心模式：如果有注册中心地址但没有直连地址，使用注册中心
        final String actualServiceUrl;
        if ((serviceUrl == null || serviceUrl.trim().isEmpty()) && 
            registryAddress != null && !registryAddress.trim().isEmpty()) {
            actualServiceUrl = registryAddress;
            logger.log("使用注册中心地址作为服务地址: " + actualServiceUrl);
        } else {
            actualServiceUrl = serviceUrl;
        }
        
        String cacheKey = serviceInterface + "@" + actualServiceUrl;
        logger.log("缓存键: " + cacheKey);
        
        return serviceCache.computeIfAbsent(cacheKey, key -> {
            logger.log("创建新的服务引用: " + key);
            
            try {
                return createDubboGenericService(serviceInterface, actualServiceUrl);
            } catch (Exception e) {
                logger.log("创建服务引用失败: " + e.getMessage());
                logger.logException(e);
                throw new RuntimeException("无法创建服务引用: " + e.getMessage(), e);
            }
        });
    }
    
    /**
     * 创建Dubbo泛化服务引用（优化版，处理类加载器问题）
     */
    private GenericService createDubboGenericService(String serviceInterface, String serviceUrl) {
        logger.log("创建优化的Dubbo泛化服务引用: " + serviceInterface);
        
        // 保存当前线程的类加载器
        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        
        try {
            // 设置类加载器为插件类加载器
            ClassLoader pluginClassLoader = this.getClass().getClassLoader();
            Thread.currentThread().setContextClassLoader(pluginClassLoader);
            logger.log("已设置类加载器为插件类加载器");
            
            // 创建引用配置，避免使用复杂的扩展机制
            ReferenceConfig<GenericService> reference = new ReferenceConfig<GenericService>();
            
            // 设置应用配置
            reference.setApplication(application);
            
            // 设置基本属性
            reference.setInterface(serviceInterface);
            reference.setGeneric(true);
            reference.setCheck(false);
            reference.setTimeout(30000);
            reference.setRetries(0);
            reference.setConnections(1); // 限制连接数
            reference.setLazy(true);     // 延迟初始化
            
            // 设置服务地址
            if (serviceUrl != null && !serviceUrl.trim().isEmpty()) {
                if (serviceUrl.startsWith("dubbo://")) {
                    logger.log("使用直连模式: " + serviceUrl);
                    reference.setUrl(serviceUrl);
                } else {
                    logger.log("使用注册中心模式: " + serviceUrl);
                    RegistryConfig registry = new RegistryConfig();
                    registry.setAddress(serviceUrl);
                    registry.setCheck(false);
                    registry.setTimeout(10000); // 设置超时
                    reference.setRegistry(registry);
                }
            } else {
                throw new IllegalArgumentException("服务地址不能为空");
            }
            
            // 尝试获取服务引用
            GenericService genericService = reference.get();
            logger.log("优化的Dubbo泛化服务引用创建成功");
            return genericService;
            
        } catch (Exception e) {
            logger.log("创建优化的Dubbo泛化服务引用失败: " + e.getMessage());
            logger.logException(e);
            
            // 对类加载器问题提供更好的提示
            if (e.getMessage() != null && e.getMessage().contains("is not visible from class loader")) {
                throw new RuntimeException("插件环境下的Dubbo类加载器可见性问题，建议使用注册中心模式: " + e.getMessage(), e);
            }
            
            throw new RuntimeException("无法创建Dubbo服务引用: " + e.getMessage(), e);
        } finally {
            // 恢复原有的类加载器
            Thread.currentThread().setContextClassLoader(originalClassLoader);
            logger.log("已恢复原有的类加载器");
        }
    }
    
    /**
     * 执行泛化调用（简化版，移除多层回退机制）
     */
    public Object invokeService(String serviceInterface, String serviceUrl, 
                               String methodName, String[] parameterTypes, Object[] parameters) {
        logger.log("开始执行Dubbo调用");
        logger.logDubboInvoke(serviceInterface, serviceUrl, methodName, parameterTypes, parameters);
        
        // 确定实际的服务地址
        final String actualServiceUrl;
        if ((serviceUrl == null || serviceUrl.trim().isEmpty()) && 
            registryAddress != null && !registryAddress.trim().isEmpty()) {
            actualServiceUrl = registryAddress;
            logger.log("使用注册中心地址作为服务地址: " + actualServiceUrl);
        } else {
            actualServiceUrl = serviceUrl;
        }
        
        // 根据地址类型选择调用方式
        if (actualServiceUrl != null && isRegistryAddress(actualServiceUrl)) {
            // 注册中心模式调用
            return invokeViaRegistry(serviceInterface, actualServiceUrl, methodName, parameterTypes, parameters);
        } else if (actualServiceUrl != null && actualServiceUrl.startsWith("dubbo://")) {
            // 直连模式调用
            return invokeViaDirect(serviceInterface, actualServiceUrl, methodName, parameterTypes, parameters);
        } else {
            throw new RuntimeException("不支持的服务地址格式: " + actualServiceUrl + "，请使用zookeeper://、nacos://或dubbo://格式");
        }
    }
    
    /**
     * 通过注册中心调用服务
     */
    private Object invokeViaRegistry(String serviceInterface, String registryUrl,
                                    String methodName, String[] parameterTypes, Object[] parameters) {
        logger.log("使用注册中心模式调用: " + registryUrl);
        
        try {
            RegistryAwareDubboClient registryClient = RegistryAwareDubboClient.getInstance();
            Object result = registryClient.invokeService(serviceInterface, registryUrl, methodName, parameterTypes, parameters);
            logger.log("注册中心模式调用成功，返回结果类型: " + (result != null ? result.getClass().getName() : "null"));
            return result;
        } catch (Exception e) {
            logger.log("注册中心模式调用失败: " + e.getMessage());
            logger.logException(e);
            throw new RuntimeException("注册中心调用失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 通过直连调用服务
     */
    private Object invokeViaDirect(String serviceInterface, String serviceUrl,
                                  String methodName, String[] parameterTypes, Object[] parameters) {
        logger.log("使用直连模式调用: " + serviceUrl);
        
        try {
            GenericService genericService = getGenericService(serviceInterface, serviceUrl);
            logger.log("泛化服务获取成功，开始调用方法: " + methodName);
            
            Object result = genericService.$invoke(methodName, parameterTypes, parameters);
            logger.log("直连模式调用成功，返回结果类型: " + (result != null ? result.getClass().getName() : "null"));
            return result;
        } catch (Exception e) {
            logger.log("直连模式调用失败: " + e.getMessage());
            logger.logException(e);
            
            // 对Hessian序列化错误的特殊处理
            if (e.getCause() instanceof ExceptionInInitializerError || 
                e.getMessage() != null && e.getMessage().contains("ExceptionInInitializerError")) {
                logger.log("检测到Hessian序列化初始化错误，尝试替代方案");
                throw new RuntimeException("远程服务返回数据序列化失败，可能是类加载或环境配置问题: " + e.getMessage(), e);
            }
            
            // 针对类加载器可见性问题，提供更好的错误信息
            if (e.getMessage() != null && e.getMessage().contains("is not visible from class loader")) {
                throw new RuntimeException("Dubbo类加载器可见性问题，建议使用注册中心模式或检查插件依赖配置: " + e.getMessage(), e);
            }
            
            throw new RuntimeException("直连调用失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 判断是否为注册中心地址
     */
    private boolean isRegistryAddress(String address) {
        if (address == null) return false;
        String lower = address.toLowerCase();
        return lower.startsWith("zookeeper://") ||
               lower.startsWith("nacos://") ||
               lower.startsWith("consul://") ||
               lower.startsWith("redis://") ||
               lower.startsWith("multicast://");
    }
    
    /**
     * 执行泛化调用并返回JSON格式结果
     */
    public String invokeServiceAsJson(String serviceInterface, String serviceUrl,
                                     String methodName, String[] parameterTypes, Object[] parameters) {
        logger.log("开始执行JSON格式的Dubbo调用");
        
        try {
            Object result = invokeService(serviceInterface, serviceUrl, methodName, parameterTypes, parameters);
            logger.log("原始调用成功，开始序列化为JSON，结果类型: " + (result != null ? result.getClass().getName() : "null"));
            
            // 处理和清理返回结果
            Object cleanedResult = cleanResult(result);
            logger.log("结果清理完成，清理后类型: " + (cleanedResult != null ? cleanedResult.getClass().getName() : "null"));
            
            // 使用优化的ObjectMapper进行序列化
            String jsonResult = objectMapper.writeValueAsString(cleanedResult);
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
    
    /**
     * 清理返回结果，移除Dubbo内部对象
     */
    private Object cleanResult(Object result) {
        if (result == null) {
            return null;
        }
        
        // 如果是Map，递归清理
        if (result instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> mapResult = (Map<String, Object>) result;
            Map<String, Object> cleanedMap = new HashMap<>();
            
            for (Map.Entry<String, Object> entry : mapResult.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();
                
                // 过滤掉Dubbo内部字段（包括class属性）
                if (!isDubboInternalField(key)) {
                    cleanedMap.put(key, cleanResult(value));
                }
            }
            
            return cleanedMap;
        }
        
        // 如果是List，递归清理元素
        if (result instanceof List) {
            @SuppressWarnings("unchecked")
            List<Object> listResult = (List<Object>) result;
            List<Object> cleanedList = new ArrayList<>();
            
            for (Object item : listResult) {
                cleanedList.add(cleanResult(item));
            }
            
            return cleanedList;
        }
        
        // 如果是数组，递归清理元素
        if (result.getClass().isArray()) {
            Object[] arrayResult = (Object[]) result;
            Object[] cleanedArray = new Object[arrayResult.length];
            
            for (int i = 0; i < arrayResult.length; i++) {
                cleanedArray[i] = cleanResult(arrayResult[i]);
            }
            
            return cleanedArray;
        }
        
        // 其他类型直接返回
        return result;
    }
    
    /**
     * 判断是否为Dubbo内部字段
     */
    private boolean isDubboInternalField(String fieldName) {
        if (fieldName == null) {
            return true;
        }
        
        // 移除class相关字段（用户需求1：剔除返回数据中的'class'属性）
        return fieldName.equals("class") || 
               fieldName.startsWith("class") || 
               fieldName.startsWith("$") ||
               fieldName.equals("@type") ||
               fieldName.equals("@class");
    }
    
    /**
     * 清空服务缓存
     */
    public void clearServiceCache() {
        serviceCache.clear();
        logger.log("服务缓已清空");
    }
    
    /**
     * 检查是否为类加载器可见性错误
     */
    private boolean isClassLoaderVisibilityError(Exception e) {
        if (e == null) {
            return false;
        }
        
        String message = e.getMessage();
        if (message != null) {
            // 检查异常消息
            if (message.contains("is not visible from class loader") ||
                message.contains("ClassLoader") ||
                message.contains("NoClassDefFoundError") ||
                message.contains("IllegalArgumentException")) {
                return true;
            }
        }
        
        // 检查异常类型
        if (e instanceof IllegalArgumentException ||
            e instanceof ClassNotFoundException) {
            return true;
        }
        
        // 检查Error类型（NoClassDefFoundError继承于Error，不是Exception）
        Throwable throwable = e;
        if (throwable instanceof NoClassDefFoundError) {
            return true;
        }
        
        // 检查原因异常
        Throwable cause = e.getCause();
        if (cause != null) {
            String causeMessage = cause.getMessage();
            if (causeMessage != null && 
                (causeMessage.contains("is not visible from class loader") ||
                 causeMessage.contains("ClassLoader"))) {
                return true;
            }
            
            if (cause instanceof IllegalArgumentException ||
                cause instanceof ClassNotFoundException) {
                return true;
            }
            
            // 检查Error类型
            if (cause instanceof NoClassDefFoundError) {
                return true;
            }
        }
        
        return false;
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
     * 测试连接（简化版，移除多层回退机制）
     */
    public boolean testConnection(String serviceInterface, String serviceUrl) {
        logger.log("开始测试连接");
        logger.log("服务接口: " + serviceInterface);
        logger.log("服务地址: " + serviceUrl);
        
        // 确定实际的服务地址
        String actualServiceUrl = serviceUrl;
        if ((serviceUrl == null || serviceUrl.trim().isEmpty()) && 
            registryAddress != null && !registryAddress.trim().isEmpty()) {
            actualServiceUrl = registryAddress;
            logger.log("使用注册中心地址作为服务地址: " + actualServiceUrl);
        }
        
        if (actualServiceUrl == null) {
            logger.log("测试失败：服务地址为空");
            return false;
        }
        
        // 根据地址类型选择测试方式
        try {
            if (isRegistryAddress(actualServiceUrl)) {
                // 注册中心模式测试
                return testRegistryConnection(actualServiceUrl);
            } else if (actualServiceUrl.startsWith("dubbo://")) {
                // 直连模式测试
                return testDirectConnection(serviceInterface, actualServiceUrl);
            } else {
                logger.log("不支持的服务地址格式: " + actualServiceUrl);
                return false;
            }
        } catch (Exception e) {
            logger.log("测试连接失败: " + e.getMessage());
            logger.logException(e);
            return false;
        }
    }
    
    /**
     * 测试注册中心连接
     */
    private boolean testRegistryConnection(String registryUrl) {
        logger.log("测试注册中心连接: " + registryUrl);
        
        try {
            RegistryAwareDubboClient registryClient = RegistryAwareDubboClient.getInstance();
            boolean result = registryClient.testRegistryConnection(registryUrl);
            logger.log("注册中心连接测试" + (result ? "成功" : "失败"));
            return result;
        } catch (Exception e) {
            logger.log("注册中心连接测试失败: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 测试直连连接
     */
    private boolean testDirectConnection(String serviceInterface, String serviceUrl) {
        logger.log("测试直连连接: " + serviceUrl);
        
        try {
            GenericService service = getGenericService(serviceInterface, serviceUrl);
            if (service != null) {
                logger.log("直连连接测试成功");
                return true;
            } else {
                logger.log("直连连接测试失败: 服务引用为null");
                return false;
            }
        } catch (Exception e) {
            logger.log("直连连接测试失败: " + e.getMessage());
            return false;
        }
    }
}