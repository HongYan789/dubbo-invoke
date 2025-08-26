package com.hongyan.dubboinvoke.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.hongyan.dubboinvoke.client.DubboClientManager;
import com.hongyan.dubboinvoke.config.DubboConfig;
import com.intellij.openapi.project.Project;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Dubbo服务调用实现类
 * 负责执行真实的Dubbo服务调用，替换原有的命令生成功能
 */
public class DubboInvokeService {
    
    private final DubboClientManager clientManager;
    private final Project project;
    private String customServiceAddress; // 自定义服务地址（可能是注册中心地址或直连地址）
    
    public DubboInvokeService(Project project) {
        this.project = project;
        this.clientManager = DubboClientManager.getInstance();
    }
    
    /**
     * 设置自定义服务地址
     * @param serviceAddress 服务地址（注册中心地址或直连地址）
     */
    public void setServiceAddress(String serviceAddress) {
        this.customServiceAddress = serviceAddress;
    }
    
    /**
     * 执行Dubbo服务调用
     * 
     * @param serviceInterface 服务接口
     * @param methodName 方法名
     * @param parametersJson 参数JSON字符串
     * @return 调用结果
     */
    public InvokeResult invokeService(String serviceInterface, String methodName, String parametersJson) {
        try {
            // 获取Dubbo配置
            DubboConfig config = DubboConfig.getInstance(project);
            
            // 选择地址与模式
            String selectedAddress = customServiceAddress;
            String serviceUrl = null; // 直连模式时使用；注册中心模式下为null
            
            if (selectedAddress != null && !selectedAddress.trim().isEmpty()) {
                if (isRegistryAddress(selectedAddress)) {
                    // 注册中心模式
                    clientManager.updateRegistryConfig(selectedAddress);
                    serviceUrl = null; // 使用注册中心
                } else {
                    // 直连模式
                    clientManager.updateRegistryConfig(null); // 重置为不使用注册中心
                    serviceUrl = buildDirectUrl(config, selectedAddress);
                }
            } else {
                // 未传入自定义地址：优先直连地址（配置），否则走注册中心（配置）
                if (config.getServiceAddress() != null && !config.getServiceAddress().trim().isEmpty()) {
                    clientManager.updateRegistryConfig(null);
                    serviceUrl = buildDirectUrl(config, config.getServiceAddress());
                } else {
                    clientManager.updateRegistryConfig(config.getRegistryAddress());
                    serviceUrl = null;
                }
            }
            
            // 获取方法签名信息
            Class<?>[] expectedParameterTypes = getMethodParameterTypes(serviceInterface, methodName);
            
            // 如果无法获取方法签名，先解析参数以便进行推断
            ParsedParameters parsedParams;
            if (expectedParameterTypes == null) {
                // 先用null解析参数
                parsedParams = parseParameters(parametersJson, null);
                // 尝试根据方法名推断参数类型
                expectedParameterTypes = inferParameterTypes(methodName, parsedParams.getParameters());
                if (expectedParameterTypes != null) {
                    System.out.println("根据方法名推断参数类型: " + java.util.Arrays.toString(expectedParameterTypes));
                    // 重新解析参数以应用推断的类型
                    parsedParams = parseParameters(parametersJson, expectedParameterTypes);
                }
            } else {
                // 解析参数
                parsedParams = parseParameters(parametersJson, expectedParameterTypes);
            }
            
            // 执行调用
            String resultJson = clientManager.invokeServiceAsJson(
                serviceInterface, 
                serviceUrl, 
                methodName, 
                parsedParams.getParameterTypes(), 
                parsedParams.getParameters()
            );
            
            return InvokeResult.success(resultJson);
            
        } catch (Exception e) {
            return InvokeResult.error("调用失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 测试服务连接
     */
    public boolean testConnection(String serviceInterface) {
        try {
            DubboConfig config = DubboConfig.getInstance(project);
            
            String selectedAddress = customServiceAddress;
            String serviceUrl = null;
            
            if (selectedAddress != null && !selectedAddress.trim().isEmpty()) {
                if (isRegistryAddress(selectedAddress)) {
                    clientManager.updateRegistryConfig(selectedAddress);
                    serviceUrl = null;
                } else {
                    clientManager.updateRegistryConfig(null);
                    serviceUrl = buildDirectUrl(config, selectedAddress);
                }
            } else {
                if (config.getServiceAddress() != null && !config.getServiceAddress().trim().isEmpty()) {
                    clientManager.updateRegistryConfig(null);
                    serviceUrl = buildDirectUrl(config, config.getServiceAddress());
                } else {
                    clientManager.updateRegistryConfig(config.getRegistryAddress());
                    serviceUrl = null;
                }
            }
            
            return clientManager.testConnection(serviceInterface, serviceUrl);
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * 仅构建直连URL；如果传入注册中心地址则返回null
     */
    private String buildDirectUrl(DubboConfig config, String addressCandidate) {
        if (addressCandidate == null || addressCandidate.trim().isEmpty()) {
            return null;
        }
        String addr = addressCandidate.trim();
        String lower = addr.toLowerCase();
        
        // 避免把注册中心地址当作直连URL
        if (isRegistryAddress(lower)) {
            return null;
        }
        
        if (lower.startsWith("dubbo://")) {
            return addr;
        }
        
        // 如果没有协议，按 host[:port] 构建
        String hostPort = addr;
        if (!hostPort.contains(":")) {
            String port = config.getServicePort() != null ? config.getServicePort() : "20880";
            hostPort = hostPort + ":" + port;
        }
        return "dubbo://" + hostPort;
    }
    
    private boolean isRegistryAddress(String address) {
        if (address == null) return false;
        String lower = address.toLowerCase();
        return lower.startsWith("zookeeper://")
            || lower.startsWith("nacos://")
            || lower.startsWith("consul://")
            || lower.startsWith("redis://")
            || lower.startsWith("multicast://");
    }
    
    /**
     * 获取方法的参数类型
     */
    private Class<?>[] getMethodParameterTypes(String serviceInterface, String methodName) {
        try {
            // 尝试通过反射获取方法签名
            Class<?> serviceClass = Class.forName(serviceInterface);
            java.lang.reflect.Method[] methods = serviceClass.getMethods();
            
            for (java.lang.reflect.Method method : methods) {
                if (method.getName().equals(methodName)) {
                    Class<?>[] paramTypes = method.getParameterTypes();
                    System.out.println("成功获取方法签名: " + methodName + ", 参数类型: " + java.util.Arrays.toString(paramTypes));
                    return paramTypes;
                }
            }
            System.out.println("未找到方法: " + methodName + " 在接口 " + serviceInterface + " 中");
        } catch (Exception e) {
            // 如果无法获取方法签名，返回null
            System.err.println("无法获取方法签名: " + serviceInterface + "." + methodName + ", 错误: " + e.getMessage());
        }
        return null;
    }
    
    /**
     * 根据方法名和常见模式推断参数类型
     */
    private Class<?>[] inferParameterTypes(String methodName, Object[] parameters) {
        System.out.println("开始推断参数类型，方法名: " + methodName);
        
        // 基于方法名的常见模式推断
        if (methodName.contains("ById") || methodName.contains("CompanyId") || 
            methodName.contains("UserId") || methodName.contains("Id")) {
            // ID相关方法通常使用Long类型
            System.out.println("检测到ID相关方法，推断参数类型为Long");
            return new Class<?>[]{Long.class};
        }
        
        // 如果无法推断，返回null
        System.out.println("无法推断参数类型，返回null");
        return null;
    }
    
    /**
     * 智能类型转换：将字符串转换为对应的Java类型
     */
    private Object convertStringToAppropriateType(String str) {
        if (str == null) {
            return str;
        }
        
        // 去除首尾空格
        str = str.trim();
        
        // 检查是否为Long类型（以L或l结尾的数字）
        if (str.matches("^-?\\d+[Ll]$")) {
            try {
                return Long.parseLong(str.substring(0, str.length() - 1));
            } catch (NumberFormatException e) {
                return str; // 转换失败，保持原字符串
            }
        }
        
        // 检查是否为Float类型（以F或f结尾的数字）
        if (str.matches("^-?\\d*\\.\\d+[Ff]$")) {
            try {
                return Float.parseFloat(str.substring(0, str.length() - 1));
            } catch (NumberFormatException e) {
                return str;
            }
        }
        
        // 检查是否为Double类型（以D或d结尾的数字，或包含小数点的数字）
        if (str.matches("^-?\\d*\\.\\d+[Dd]?$")) {
            try {
                if (str.endsWith("D") || str.endsWith("d")) {
                    return Double.parseDouble(str.substring(0, str.length() - 1));
                } else {
                    return Double.parseDouble(str);
                }
            } catch (NumberFormatException e) {
                return str;
            }
        }
        
        // 检查是否为Integer类型（纯数字）
        if (str.matches("^-?\\d+$")) {
            try {
                return Integer.parseInt(str);
            } catch (NumberFormatException e) {
                // 如果Integer溢出，尝试Long
                try {
                    return Long.parseLong(str);
                } catch (NumberFormatException e2) {
                    return str;
                }
            }
        }
        
        // 检查是否为Boolean类型
        if ("true".equalsIgnoreCase(str) || "false".equalsIgnoreCase(str)) {
            return Boolean.parseBoolean(str);
        }
        
        // 默认保持字符串类型
        return str;
    }
    
    /**
     * 根据期望类型进行精确转换
     */
    private Object convertToExpectedType(String str, Class<?> expectedType) {
        if (str == null) {
            return null;
        }
        
        try {
            if (expectedType == Long.class || expectedType == long.class) {
                // 移除可能的L后缀
                String cleanStr = str.endsWith("L") || str.endsWith("l") 
                    ? str.substring(0, str.length() - 1) : str;
                return Long.parseLong(cleanStr);
            } else if (expectedType == Integer.class || expectedType == int.class) {
                return Integer.parseInt(str);
            } else if (expectedType == Double.class || expectedType == double.class) {
                // 移除可能的D后缀
                String cleanStr = str.endsWith("D") || str.endsWith("d") 
                    ? str.substring(0, str.length() - 1) : str;
                return Double.parseDouble(cleanStr);
            } else if (expectedType == Float.class || expectedType == float.class) {
                // 移除可能的F后缀
                String cleanStr = str.endsWith("F") || str.endsWith("f") 
                    ? str.substring(0, str.length() - 1) : str;
                return Float.parseFloat(cleanStr);
            } else if (expectedType == Boolean.class || expectedType == boolean.class) {
                return Boolean.parseBoolean(str);
            } else if (expectedType == String.class) {
                return str;
            } else {
                // 对于其他类型，尝试智能转换
                return convertStringToAppropriateType(str);
            }
        } catch (NumberFormatException e) {
            throw new RuntimeException("无法将 '" + str + "' 转换为 " + expectedType.getSimpleName() + " 类型: " + e.getMessage(), e);
        }
    }
    
    /**
     * 解析参数JSON
     */
    private ParsedParameters parseParameters(String parametersJson, Class<?>[] expectedParameterTypes) {
        if (parametersJson == null || parametersJson.trim().isEmpty() || "[]".equals(parametersJson.trim())) {
            return new ParsedParameters(new String[0], new Object[0]);
        }
        
        try {
            ObjectMapper mapper = new ObjectMapper();
            List<Object> paramList;
            
            // 检查是否是单个JSON对象（而不是数组）
            String trimmed = parametersJson.trim();
            if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
                // 单个JSON对象，包装成单元素列表
                Object singleObject = mapper.readValue(parametersJson, Object.class);
                paramList = new ArrayList<>();
                paramList.add(singleObject);
            } else {
                // 尝试解析为JSON数组
                paramList = mapper.readValue(parametersJson, new TypeReference<List<Object>>() {});
            }
            
            if (paramList == null) {
                return new ParsedParameters(new String[0], new Object[0]);
            }
            
            List<String> typeList = new ArrayList<>();
            List<Object> valueList = new ArrayList<>();
            
            for (int i = 0; i < paramList.size(); i++) {
                Object param = paramList.get(i);
                Class<?> expectedType = (expectedParameterTypes != null && i < expectedParameterTypes.length) 
                    ? expectedParameterTypes[i] : null;
                
                if (param instanceof Map) {
                    // 复杂对象参数
                    typeList.add(expectedType != null ? expectedType.getName() : "java.lang.Object");
                    valueList.add(param);
                } else if (param instanceof String) {
                    String strParam = (String) param;
                    if (expectedType != null) {
                        // 根据期望类型进行精确转换
                        Object convertedValue = convertToExpectedType(strParam, expectedType);
                        typeList.add(expectedType.getName());
                        valueList.add(convertedValue);
                    } else {
                        // 智能类型转换：检查字符串是否表示其他类型
                        Object convertedValue = convertStringToAppropriateType(strParam);
                        if (convertedValue != strParam) {
                            // 转换成功，使用转换后的类型和值
                            if (convertedValue instanceof Long) {
                                typeList.add("java.lang.Long");
                            } else if (convertedValue instanceof Integer) {
                                typeList.add("java.lang.Integer");
                            } else if (convertedValue instanceof Double) {
                                typeList.add("java.lang.Double");
                            } else if (convertedValue instanceof Float) {
                                typeList.add("java.lang.Float");
                            } else if (convertedValue instanceof Boolean) {
                                typeList.add("java.lang.Boolean");
                            } else {
                                typeList.add("java.lang.String");
                            }
                            valueList.add(convertedValue);
                        } else {
                            // 保持字符串类型
                            typeList.add("java.lang.String");
                            valueList.add(param);
                        }
                    }
                } else if (param instanceof Integer) {
                    typeList.add(expectedType != null ? expectedType.getName() : "java.lang.Integer");
                    valueList.add(expectedType != null && expectedType != Integer.class && expectedType != int.class 
                        ? convertToExpectedType(param.toString(), expectedType) : param);
                } else if (param instanceof Long) {
                    typeList.add(expectedType != null ? expectedType.getName() : "java.lang.Long");
                    valueList.add(expectedType != null && expectedType != Long.class && expectedType != long.class 
                        ? convertToExpectedType(param.toString(), expectedType) : param);
                } else if (param instanceof Double) {
                    typeList.add(expectedType != null ? expectedType.getName() : "java.lang.Double");
                    valueList.add(expectedType != null && expectedType != Double.class && expectedType != double.class 
                        ? convertToExpectedType(param.toString(), expectedType) : param);
                } else if (param instanceof Boolean) {
                    typeList.add(expectedType != null ? expectedType.getName() : "java.lang.Boolean");
                    valueList.add(expectedType != null && expectedType != Boolean.class && expectedType != boolean.class 
                        ? convertToExpectedType(param.toString(), expectedType) : param);
                } else {
                    // 默认作为Object处理
                    typeList.add(expectedType != null ? expectedType.getName() : "java.lang.Object");
                    valueList.add(param);
                }
            }
            
            return new ParsedParameters(
                typeList.toArray(new String[0]),
                valueList.toArray()
            );
            
        } catch (Exception e) {
            throw new RuntimeException("参数解析失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 参数解析结果
     */
    private static class ParsedParameters {
        private final String[] parameterTypes;
        private final Object[] parameters;
        
        public ParsedParameters(String[] parameterTypes, Object[] parameters) {
            this.parameterTypes = parameterTypes;
            this.parameters = parameters;
        }
        
        public String[] getParameterTypes() {
            return parameterTypes;
        }
        
        public Object[] getParameters() {
            return parameters;
        }
    }
    
    /**
     * 调用结果
     */
    public static class InvokeResult {
        private final boolean success;
        private final String result;
        private final String errorMessage;
        private final Throwable exception;
        
        private InvokeResult(boolean success, String result, String errorMessage, Throwable exception) {
            this.success = success;
            this.result = result;
            this.errorMessage = errorMessage;
            this.exception = exception;
        }
        
        public static InvokeResult success(String result) {
            return new InvokeResult(true, result, null, null);
        }
        
        public static InvokeResult error(String errorMessage, Throwable exception) {
            return new InvokeResult(false, null, errorMessage, exception);
        }
        
        public boolean isSuccess() {
            return success;
        }
        
        public String getResult() {
            return result;
        }
        
        public String getErrorMessage() {
            return errorMessage;
        }
        
        public Throwable getException() {
            return exception;
        }
    }
}