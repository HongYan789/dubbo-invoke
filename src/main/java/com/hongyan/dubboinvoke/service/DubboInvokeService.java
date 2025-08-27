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
            
            // 检查返回的JSON是否包含错误信息
            if (resultJson != null && isErrorResponse(resultJson)) {
                // 从错误响应中提取错误信息
                String errorMessage = extractErrorMessage(resultJson);
                return InvokeResult.error(errorMessage, new RuntimeException(errorMessage));
            }
            
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
        System.out.println("开始推断参数类型，方法名: " + methodName + ", 参数数量: " + (parameters != null ? parameters.length : 0));
        
        // 特殊处理：多参数方法的类型推断（问题2修复）
        if (parameters != null && parameters.length > 1) {
            return inferMultiParameterTypes(methodName, parameters);
        }
        if (parameters != null && parameters.length > 0) {
            Object firstParam = parameters[0];
            
            // 检查是否为包含class字段的复杂对象
            if (firstParam instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> paramMap = (Map<String, Object>) firstParam;
                if (paramMap.containsKey("class")) {
                    String className = paramMap.get("class").toString();
                    System.out.println("检测到包含class字段的复杂对象参数，类名: " + className);
                    try {
                        Class<?> paramClass = Class.forName(className);
                        System.out.println("成功解析复杂对象类型: " + className);
                        return new Class<?>[]{paramClass};
                    } catch (ClassNotFoundException e) {
                        System.out.println("无法加载类: " + className + ", 使用Object类型");
                        return new Class<?>[]{Object.class};
                    }
                } else {
                    // 不包含class字段的Map，可能是通用参数对象
                    System.out.println("检测到Map类型参数，推断为Object类型");
                    return new Class<?>[]{Object.class};
                }
            }
            
            // 如果第一个参数是List，推断为List类型
            if (firstParam instanceof List) {
                System.out.println("检测到List参数，推断参数类型为java.util.List");
                return new Class<?>[]{java.util.List.class};
            }
            
            // 如果参数看起来像数组格式且方法名包含List，推断为List类型
            if (methodName.toLowerCase().contains("list") && 
                (firstParam.toString().startsWith("[") || firstParam instanceof List)) {
                System.out.println("检测到List相关方法且参数为数组格式，推断参数类型为java.util.List");
                return new Class<?>[]{java.util.List.class};
            }
        }
        
        // 基于方法名的模式推断（降级策略）
        if (methodName.toLowerCase().contains("list")) {
            // List相关方法通常使用List类型  
            System.out.println("检测到List相关方法，推断参数类型为java.util.List");
            return new Class<?>[]{java.util.List.class};
        } else if (methodName.toLowerCase().contains("page") || 
                   methodName.toLowerCase().contains("bypage") ||
                   methodName.toLowerCase().contains("pageindex") ||
                   methodName.toLowerCase().contains("pagesize")) {
            // 分页相关方法通常使用复杂对象参数
            System.out.println("检测到分页相关方法，推断参数类型为Object（复杂对象）");
            return new Class<?>[]{Object.class};
        } else if ((methodName.contains("ById") || methodName.contains("ByCompanyId") || 
                   methodName.contains("ById") || methodName.toLowerCase().contains("byid")) && 
                  !methodName.toLowerCase().contains("page") &&
                  !methodName.toLowerCase().contains("list")) {
            // ID查询方法，支持多种格式: loadById, getByCompanyId, findByCompanyId, queryById等
            if (parameters != null && parameters.length > 0) {
                Object param = parameters[0];
                if (param instanceof Number) {
                    // 对于数字类型，优先使用Long（Dubbo中ID通常是Long）
                    System.out.println("检测到ID查询方法的数字参数，推断参数类型为Long");
                    return new Class<?>[]{Long.class};
                } else {
                    // 对于非数字类型，可能是字符串ID
                    System.out.println("检测到ID查询方法的非数字参数，推断参数类型为String");
                    return new Class<?>[]{String.class};
                }
            }
            // 默认使用Long类型
            System.out.println("检测到ID查询方法，默认推断参数类型为Long");
            return new Class<?>[]{Long.class};
        } else if (methodName.toLowerCase().contains("repair") || 
                   methodName.toLowerCase().contains("require")) {
            // repair和require相关方法可能期望字符串或字符串数组
            System.out.println("检测到特殊业务方法，推断参数类型为String或String数组");
            if (parameters != null && parameters.length > 0) {
                Object firstParam = parameters[0];
                if (firstParam instanceof List || firstParam.toString().startsWith("[")) {
                    return new Class<?>[]{String[].class};
                } else {
                    return new Class<?>[]{String.class};
                }
            }
            return new Class<?>[]{String.class};
        } else if (methodName.toLowerCase().contains("companyinfo") ||
                   methodName.toLowerCase().contains("init") ||
                   methodName.toLowerCase().contains("agg")) {
            // 复杂业务方法通常使用复杂对象参数
            System.out.println("检测到复杂业务方法，推断参数类型为Object（复杂对象）");
            return new Class<?>[]{Object.class};
        }
        
        // 如果无法通过方法名推断，尝试根据参数内容进行最终的类型推断
        if (parameters != null && parameters.length > 0) {
            Object param = parameters[0];
            if (param instanceof Number) {
                if (param instanceof Integer) {
                    // Integer转为Long，因为Dubbo中数字ID通常是Long类型
                    System.out.println("参数是Integer类型，转换为Long类型");
                    return new Class<?>[]{Long.class};
                } else if (param instanceof Long) {
                    System.out.println("参数是Long类型");
                    return new Class<?>[]{Long.class};
                } else if (param instanceof Double || param instanceof Float) {
                    System.out.println("参数是浮点数类型");
                    return new Class<?>[]{param.getClass()};
                }
            } else if (param instanceof String) {
                // 检查字符串是否为纯数字，如果是则推断为Long
                String strParam = param.toString().trim();
                if (strParam.matches("^-?\\d+$")) {
                    System.out.println("参数是数字字符串，推断为Long类型");
                    return new Class<?>[]{Long.class};
                } else {
                    System.out.println("参数是String类型");
                    return new Class<?>[]{String.class};
                }
            } else if (param instanceof Boolean) {
                System.out.println("参数是Boolean类型");
                return new Class<?>[]{Boolean.class};
            }
        }
        
        // 如果无法推断，返回null
        System.out.println("无法推断参数类型，返回null");
        return null;
    }
    
    /**
     * 智能推断null参数的类型（根据参数位置和常见模式）
     */
    private Class<?> inferNullParameterType(int parameterIndex, int totalParameters) {
        // 根据参数位置推断类型
        if (parameterIndex == 0 && totalParameters >= 2) {
            // 第一个参数往往是List类型（如ID列表、查询条件等）
            return java.util.List.class;
        } else if (parameterIndex == totalParameters - 1 && totalParameters >= 3) {
            // 最后一个参数往往是数值类型（如ID、状态等）
            return Long.class;
        } else {
            // 中间参数通常是字符串类型（如名称、编码等）
            return String.class;
        }
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
        
        // 特殊处理：如果字符串是数组格式，尝试解析为数组
        if (str.startsWith("[") && str.endsWith("]")) {
            try {
                // 尝试使用JSON解析器解析数组
                ObjectMapper mapper = new ObjectMapper();
                List<Object> list = mapper.readValue(str, new TypeReference<List<Object>>() {});
                return list;
            } catch (Exception e) {
                // 如果解析失败，返回原字符串
                System.out.println("数组格式解析失败，保持原字符串: " + e.getMessage());
            }
        }
        
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
        
        // 检查是否为纯数字（可能需要转换为Long而不是Integer）
        if (str.matches("^-?\\d+$")) {
            try {
                long longValue = Long.parseLong(str);
                // 对于ID类型，优先使用Long
                return longValue;
            } catch (NumberFormatException e) {
                // 如果Long也溢出，保持字符串
                return str;
            }
        }
        
        // 上面已经处理了纯数字的情况，这里不需要重复处理
        
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
            
            // 特殊处理：如果参数只有一个且是数组，但期望类型是List，则直接使用该数组作为List参数
            if (paramList.size() == 1 && paramList.get(0) instanceof List && 
                expectedParameterTypes != null && expectedParameterTypes.length == 1 && 
                java.util.List.class.isAssignableFrom(expectedParameterTypes[0])) {
                System.out.println("检测到嵌套数组参数，展开为单个List参数");
                List<?> nestedList = (List<?>) paramList.get(0);
                return new ParsedParameters(
                    new String[]{"java.util.List"},
                    new Object[]{nestedList}
                );
            }
            
            List<String> typeList = new ArrayList<>();
            List<Object> valueList = new ArrayList<>();
            
            for (int i = 0; i < paramList.size(); i++) {
                Object param = paramList.get(i);
                Class<?> expectedType = (expectedParameterTypes != null && i < expectedParameterTypes.length) 
                    ? expectedParameterTypes[i] : null;
                
                // 关键修复：添加null值处理逻辑
                if (param == null) {
                    if (expectedType != null) {
                        // 有期望类型，使用期望类型
                        typeList.add(expectedType.getName());
                        valueList.add(null);
                        System.out.println("参数" + i + ": null -> " + expectedType.getName() + " (使用期望类型)");
                    } else {
                        // 没有期望类型，根据方法名和参数位置智能推断
                        Class<?> inferredType = inferNullParameterType(i, paramList.size());
                        typeList.add(inferredType.getName());
                        valueList.add(null);
                        System.out.println("参数" + i + ": null -> " + inferredType.getName() + " (智能推断)");
                    }
                } else if (param instanceof List) {
                    // List参数处理
                    List<?> listParam = (List<?>) param;
                    if (expectedType != null) {
                        if (java.util.List.class.isAssignableFrom(expectedType)) {
                            typeList.add(expectedType.getName());
                            valueList.add(listParam);
                        } else if (expectedType.isArray()) {
                            // 期望的是数组类型，转换List为数组
                            typeList.add(expectedType.getName());
                            if (expectedType == String[].class) {
                                typeList.add("[Ljava.lang.String;"); // 使用JVM内部数组表示法
                                String[] arrayParam = listParam.stream()
                                    .map(Object::toString)
                                    .toArray(String[]::new);
                                valueList.add(arrayParam);
                            } else if (expectedType == Long[].class) {
                                typeList.add("[Ljava.lang.Long;"); // 使用JVM内部数组表示法
                                Long[] arrayParam = listParam.stream()
                                    .map(item -> Long.valueOf(item.toString()))
                                    .toArray(Long[]::new);
                                valueList.add(arrayParam);
                            } else {
                                // 其他数组类型，使用标准的数组类型名称
                                typeList.add(expectedType.getName());
                                Object[] arrayParam = listParam.toArray();
                                valueList.add(arrayParam);
                            }
                        } else {
                            // 期望类型不是List也不是数组，但参数是List，仍然作为List处理
                            typeList.add("java.util.List");
                            valueList.add(listParam);
                        }
                    } else {
                        // 没有期望类型，默认作为List处理
                        typeList.add("java.util.List");
                        valueList.add(listParam);
                    }
                } else if (param instanceof Map) {
                    // 复杂对象参数
                    @SuppressWarnings("unchecked")
                    Map<String, Object> paramMap = (Map<String, Object>) param;
                    
                    if (paramMap.containsKey("class")) {
                        // 包含class字段的复杂对象，使用指定的类型
                        String className = paramMap.get("class").toString();
                        System.out.println("检测到包含class字段的复杂对象：" + className);
                        typeList.add(className);
                        
                        // 从参数中移除class字段，保留其他属性
                        Map<String, Object> cleanedParam = new java.util.HashMap<>(paramMap);
                        cleanedParam.remove("class");
                        valueList.add(cleanedParam);
                    } else {
                        // 不包含class字段的Map，使用期望类型或默认Object
                        typeList.add(expectedType != null ? expectedType.getName() : "java.lang.Object");
                        valueList.add(param);
                    }
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
     * 检查返回的JSON是否为错误响应
     */
    private boolean isErrorResponse(String jsonResponse) {
        if (jsonResponse == null || jsonResponse.trim().isEmpty()) {
            return false;
        }
        
        try {
            // 检查是否包含error字段且值为true
            if (jsonResponse.contains("\"error\":true") || 
                jsonResponse.contains("\"error\": true")) {
                return true;
            }
            
            // 检查是否包含常见的异常信息关键词
            String lowerResponse = jsonResponse.toLowerCase();
            if (lowerResponse.contains("exception") && 
                (lowerResponse.contains("message") || lowerResponse.contains("type"))) {
                return true;
            }
            
            // 检查是否包含Dubbo特定的错误信息
            if (lowerResponse.contains("rpcexception") || 
                lowerResponse.contains("nosuchmethodexception") ||
                lowerResponse.contains("failed to invoke") ||
                lowerResponse.contains("hessian") ||
                lowerResponse.contains("classnotfound")) {
                return true;
            }
            
            return false;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * 从错误响应JSON中提取错误信息
     */
    private String extractErrorMessage(String errorJson) {
        if (errorJson == null || errorJson.trim().isEmpty()) {
            return "未知错误";
        }
        
        try {
            // 使用ObjectMapper解析JSON
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> errorResponse = mapper.readValue(errorJson, Map.class);
            
            String message = (String) errorResponse.get("message");
            String type = (String) errorResponse.get("type");
            
            if (message != null && type != null) {
                return type + ": " + message;
            } else if (message != null) {
                return message;
            } else {
                return "调用失败，无详细错误信息";
            }
        } catch (Exception e) {
            // 如果无法解析JSON，直接返回原始字符串
            return errorJson;
        }
    }
    
    /**
     * 多参数方法的类型推断（问题2修复：基于成功调用案例优化多参数调用的参数类型推断）
     */
    private Class<?>[] inferMultiParameterTypes(String methodName, Object[] parameters) {
        System.out.println("开始多参数类型推断，方法名: " + methodName + ", 参数数量: " + parameters.length);
        
        // 专门处理getCompanyInfoByCompanyIdsAndDanwBh类的方法
        // 根据成功调用案例: getCompanyInfoByCompanyIdsAndDanwBh(List<Long>, List<String>, Long)
        if (methodName.contains("CompanyIds") && methodName.contains("Danw")) {
            System.out.println("检测到CompanyIds和Danw相关的多参数方法，基于成功案例推断参数类型");
            
            Class<?>[] paramTypes = new Class<?>[parameters.length];
            for (int i = 0; i < parameters.length; i++) {
                Object param = parameters[i];
                
                if (param == null) {
                    // 根据成功调用案例的方法签名推断null参数类型
                    if (i == 0) {
                        // 第一个参数: List<Long> (CompanyIds)
                        paramTypes[i] = java.util.List.class;
                        System.out.println("参数" + i + ": null -> List<Long> (CompanyIds参数)");
                    } else if (i == 1) {
                        // 第二个参数: List<String> (DanwBhList)
                        paramTypes[i] = java.util.List.class;
                        System.out.println("参数" + i + ": null -> List<String> (DanwBhList参数)");
                    } else if (i == 2) {
                        // 第三个参数: Long (storeId)
                        paramTypes[i] = Long.class;
                        System.out.println("参数" + i + ": null -> Long (storeId参数)");
                    } else {
                        // 其他位置的参数，默认为Object
                        paramTypes[i] = Object.class;
                        System.out.println("参数" + i + ": null -> Object (未知位置参数)");
                    }
                } else if (param instanceof List || (param instanceof String && param.toString().startsWith("["))) {
                    // List参数或数组格式字符串
                    paramTypes[i] = java.util.List.class;
                    System.out.println("参数" + i + ": " + param + " -> List");
                } else if (param instanceof String) {
                    // 字符串参数 - 但要注意位置推断
                    if (i == 1 && param.toString().trim().isEmpty()) {
                        // 第二个位置的空字符串可能是空的DanwBhList
                        paramTypes[i] = java.util.List.class;
                        System.out.println("参数" + i + ": \"\" -> List (位置推断为DanwBhList)");
                    } else {
                        paramTypes[i] = String.class;
                        System.out.println("参数" + i + ": " + param + " -> String");
                    }
                } else if (param instanceof Number) {
                    // 数字参数
                    paramTypes[i] = Long.class;
                    System.out.println("参数" + i + ": " + param + " -> Long");
                } else {
                    // 其他类型默认为Object
                    paramTypes[i] = Object.class;
                    System.out.println("参数" + i + ": " + param + " -> Object");
                }
            }
            
            System.out.println("🎯 基于成功案例的多参数类型推断结果: " + java.util.Arrays.toString(paramTypes));
            return paramTypes;
        }
        
        // 通用多参数处理
        Class<?>[] paramTypes = new Class<?>[parameters.length];
        for (int i = 0; i < parameters.length; i++) {
            Object param = parameters[i];
            
            if (param == null) {
                paramTypes[i] = Object.class;
            } else if (param instanceof List) {
                paramTypes[i] = java.util.List.class;
            } else if (param instanceof String) {
                // 检查是否是数组格式
                String strParam = param.toString().trim();
                if (strParam.startsWith("[") && strParam.endsWith("]")) {
                    paramTypes[i] = java.util.List.class;
                } else if (strParam.matches("^-?\\d+$")) {
                    paramTypes[i] = Long.class;
                } else {
                    paramTypes[i] = String.class;
                }
            } else if (param instanceof Number) {
                paramTypes[i] = Long.class;
            } else if (param instanceof Boolean) {
                paramTypes[i] = Boolean.class;
            } else {
                paramTypes[i] = Object.class;
            }
        }
        
        System.out.println("通用多参数类型推断结果: " + java.util.Arrays.toString(paramTypes));
        return paramTypes;
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