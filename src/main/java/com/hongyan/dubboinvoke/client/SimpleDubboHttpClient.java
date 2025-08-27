package com.hongyan.dubboinvoke.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hongyan.dubboinvoke.util.OperationLogger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 简化的Dubbo HTTP调用客户端
 * 通过HTTP协议调用Dubbo服务，避免复杂的代理机制和类加载器问题
 */
public class SimpleDubboHttpClient {
    
    private static volatile SimpleDubboHttpClient INSTANCE;
    private static final OperationLogger logger = OperationLogger.getInstance();
    
    private final ObjectMapper objectMapper;
    private final Map<String, String> connectionCache = new ConcurrentHashMap<>();
    
    private SimpleDubboHttpClient() {
        logger.log("初始化SimpleDubboHttpClient");
        this.objectMapper = new ObjectMapper();
    }
    
    public static SimpleDubboHttpClient getInstance() {
        if (INSTANCE == null) {
            synchronized (SimpleDubboHttpClient.class) {
                if (INSTANCE == null) {
                    INSTANCE = new SimpleDubboHttpClient();
                }
            }
        }
        return INSTANCE;
    }
    
    /**
     * 执行Dubbo服务调用
     */
    public Object invokeService(String serviceInterface, String serviceUrl, 
                               String methodName, String[] parameterTypes, Object[] parameters) {
        logger.log("开始执行HTTP方式的Dubbo调用");
        logger.log("服务接口: " + serviceInterface);
        logger.log("服务地址: " + serviceUrl);
        logger.log("方法名: " + methodName);
        
        try {
            // 构建HTTP调用URL
            String httpUrl = convertToHttpUrl(serviceUrl);
            logger.log("转换为HTTP URL: " + httpUrl);
            
            // 构建请求体
            Map<String, Object> requestBody = buildRequestBody(serviceInterface, methodName, parameterTypes, parameters);
            String jsonRequest = objectMapper.writeValueAsString(requestBody);
            logger.log("请求JSON: " + jsonRequest);
            
            // 发送HTTP请求
            String response = sendHttpRequest(httpUrl, jsonRequest);
            logger.log("收到响应: " + response);
            
            // 解析响应
            return parseResponse(response);
            
        } catch (Exception e) {
            logger.log("HTTP调用失败: " + e.getMessage());
            logger.logException(e);
            throw new RuntimeException("HTTP调用失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 将Dubbo URL转换为HTTP URL
     */
    private String convertToHttpUrl(String dubboUrl) {
        if (dubboUrl.startsWith("dubbo://")) {
            // dubbo://host:port -> http://host:port/dubbo
            // 改为更通用的路径，因为/invoke可能不存在
            String baseUrl = dubboUrl.replace("dubbo://", "http://");
            // 先尝试直接访问，不添加路径
            return baseUrl;
        } else if (dubboUrl.startsWith("http://") || dubboUrl.startsWith("https://")) {
            return dubboUrl;
        } else {
            // 默认假设是注册中心地址，这里简化处理
            return "http://" + dubboUrl;
        }
    }
    
    /**
     * 构建HTTP请求体
     */
    private Map<String, Object> buildRequestBody(String serviceInterface, String methodName, 
                                                String[] parameterTypes, Object[] parameters) {
        Map<String, Object> request = new HashMap<>();
        request.put("service", serviceInterface);
        request.put("method", methodName);
        request.put("parameterTypes", parameterTypes);
        request.put("parameters", parameters);
        request.put("version", "1.0.0");
        request.put("group", "");
        return request;
    }
    
    /**
     * 发送HTTP请求
     */
    private String sendHttpRequest(String urlString, String jsonRequest) throws Exception {
        URL url = new URL(urlString);
        HttpURLConnection connection = null;
        
        try {
            connection = (HttpURLConnection) url.openConnection();
            
            // 设置请求方法和头部
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("User-Agent", "Dubbo-Invoke-Plugin/1.0");
            connection.setDoOutput(true);
            connection.setDoInput(true);
            
            // 优化超时时间
            connection.setConnectTimeout(5000);  // 连接超时5秒
            connection.setReadTimeout(10000);     // 读取超时10秒
            
            // 发送请求体
            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = jsonRequest.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
                os.flush();
            }
            
            // 获取响应码
            int responseCode = connection.getResponseCode();
            logger.log("HTTP响应码: " + responseCode);
            
            // 读取响应
            InputStream inputStream = null;
            try {
                if (responseCode >= 200 && responseCode < 300) {
                    inputStream = connection.getInputStream();
                } else {
                    inputStream = connection.getErrorStream();
                    if (inputStream == null) {
                        // 如果错误流也为null，尝试输入流
                        inputStream = connection.getInputStream();
                    }
                }
                
                // 防止空流导致NullPointerException
                if (inputStream == null) {
                    throw new RuntimeException("无法获取响应流，响应码: " + responseCode);
                }
                
                StringBuilder response = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line).append("\n");
                    }
                }
                
                String responseText = response.toString().trim();
                logger.log("收到响应内容: " + (responseText.length() > 200 ? 
                    responseText.substring(0, 200) + "..." : responseText));
                
                if (responseCode >= 200 && responseCode < 300) {
                    return responseText;
                } else {
                    throw new RuntimeException("HTTP请求失败，响应码: " + responseCode + 
                                             ", 响应: " + responseText);
                }
                
            } finally {
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (IOException ignored) {
                        // 忽略关闭流时的异常
                    }
                }
            }
            
        } catch (java.net.SocketTimeoutException e) {
            logger.log("HTTP请求超时: " + e.getMessage());
            throw new RuntimeException("HTTP请求超时，请检查服务是否可用: " + urlString, e);
        } catch (java.net.ConnectException e) {
            logger.log("HTTP连接失败: " + e.getMessage());
            throw new RuntimeException("无法连接到服务: " + urlString + ", 错误: " + e.getMessage(), e);
        } catch (java.net.UnknownHostException e) {
            logger.log("主机名解析失败: " + e.getMessage());
            throw new RuntimeException("无法解析主机名: " + urlString, e);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
    
    /**
     * 解析HTTP响应
     */
    private Object parseResponse(String response) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> responseMap = objectMapper.readValue(response, Map.class);
            
            if (responseMap.containsKey("error")) {
                throw new RuntimeException("服务端返回错误: " + responseMap.get("error"));
            }
            
            return responseMap.get("result");
            
        } catch (Exception e) {
            logger.log("解析响应失败: " + e.getMessage());
            // 如果解析失败，返回原始字符串
            return response;
        }
    }
    
    /**
     * 执行调用并返回JSON格式结果
     */
    public String invokeServiceAsJson(String serviceInterface, String serviceUrl,
                                     String methodName, String[] parameterTypes, Object[] parameters) {
        logger.log("开始执行JSON格式的HTTP调用");
        
        try {
            Object result = invokeService(serviceInterface, serviceUrl, methodName, parameterTypes, parameters);
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
     * 测试连接
     */
    public boolean testConnection(String serviceInterface, String serviceUrl) {
        logger.log("开始测试HTTP连接");
        logger.log("服务接口: " + serviceInterface);
        logger.log("服务地址: " + serviceUrl);
        
        try {
            String httpUrl = convertToHttpUrl(serviceUrl);
            URL url = new URL(httpUrl);
            HttpURLConnection connection = null;
            
            try {
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("HEAD"); // 使用HEAD方法减少网络开销
                connection.setConnectTimeout(3000);  // 3秒连接超时
                connection.setReadTimeout(3000);     // 3秒读取超时
                
                int responseCode = connection.getResponseCode();
                boolean success = responseCode < 500; // 5xx是服务器错误，其他都可以认为连接成功
                logger.log("连接测试" + (success ? "成功" : "失败") + 
                          "，响应码: " + responseCode);
                return success;
                
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
            
        } catch (java.net.SocketTimeoutException e) {
            logger.log("连接测试超时: " + e.getMessage());
            return false;
        } catch (java.net.ConnectException e) {
            logger.log("连接测试失败 - 拒绝连接: " + e.getMessage());
            return false;
        } catch (java.net.UnknownHostException e) {
            logger.log("连接测试失败 - 主机名解析: " + e.getMessage());
            return false;
        } catch (Exception e) {
            logger.log("连接测试失败: " + e.getMessage());
            logger.logException(e);
            return false;
        }
    }
}