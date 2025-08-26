package com.hongyan.dubboinvoke.client;

import com.hongyan.dubboinvoke.util.OperationLogger;
import org.apache.dubbo.common.serialize.hessian2.Hessian2ObjectInput;
import org.apache.dubbo.common.serialize.hessian2.Hessian2ObjectOutput;
import java.io.*;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicLong;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

/**
 * 基于原生Socket的直接Dubbo协议客户端
 * 完全绕过Dubbo扩展机制，直接进行Socket通信
 * 
 * 基于Dubbo官方最佳实践:
 * - 使用UTF-8编码处理字符串
 * - 参考Dubbo 2.6.6 hessian-lite 2.3.5版本的修复
 * - 增强递归深度限制和字符编码处理
 */
public class NettyDubboClient {
    private static final OperationLogger logger = OperationLogger.getInstance();
    
    // Dubbo协议魔数
    private static final short MAGIC = (short) 0xdabb;
    private static final byte FLAG_REQUEST = (byte) 0x80;
    private static final byte FLAG_TWOWAY = (byte) 0x40;
    private static final byte SERIALIZATION_HESSIAN2 = 2;
    
    private static final AtomicLong REQUEST_ID = new AtomicLong(1);
    
    // 基于Dubbo官方建议的递归深度限制
    private static final int MAX_RECURSION_DEPTH = 15;
    private static final int MAX_CLASS_DEF_DEPTH = 8;
    
    private String host;
    private int port;
    
    public NettyDubboClient(String host, int port) {
        this.host = host;
        this.port = port;
    }
    
    /**
     * 调用远程Dubbo服务
     */
    public Object invoke(String serviceName, String methodName, Class<?>[] parameterTypes, Object[] arguments) throws Exception {
        logger.log("开始调用Dubbo服务: " + serviceName + "." + methodName);
        
        try (Socket socket = new Socket(host, port)) {
            socket.setSoTimeout(30000); // 30秒超时
            
            // 构建请求
            byte[] requestData = buildDubboRequest(serviceName, methodName, parameterTypes, arguments);
            
            // 发送请求
            OutputStream out = socket.getOutputStream();
            out.write(requestData);
            out.flush();
            
            // 接收响应
            InputStream in = socket.getInputStream();
            Object result = parseResponse(in);
            
            logger.log("Dubbo调用完成，结果类型: " + (result != null ? result.getClass().getSimpleName() : "null"));
            
            // 格式化返回结果，只保留业务数据
            return formatInvokeResult(result);
            
        } catch (Exception e) {
            logger.logException(e);
            throw e;
        }
    }
    
    /**
     * 格式化调用结果，基于Dubbo最佳实践只返回业务数据
     */
    private Object formatInvokeResult(Object rawResult) {
        if (rawResult == null) {
            return null;
        }
        
        // 如果是Map类型，提取业务数据
        if (rawResult instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> resultMap = (Map<String, Object>) rawResult;
            
            // 创建只包含业务数据的新Map
            Map<String, Object> businessData = new HashMap<>();
            
            for (Map.Entry<String, Object> entry : resultMap.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();
                
                // 过滤掉系统字段，只保留业务数据
                if (!isSystemField(key)) {
                    // 递归清理嵌套的业务数据
                    if (value instanceof String) {
                        businessData.put(key, cleanStringData((String) value));
                    } else if (value instanceof Map) {
                        businessData.put(key, formatInvokeResult(value));
                    } else if (value instanceof List) {
                        @SuppressWarnings("unchecked")
                        List<Object> list = (List<Object>) value;
                        List<Object> cleanList = new ArrayList<>();
                        for (Object item : list) {
                            cleanList.add(formatInvokeResult(item));
                        }
                        businessData.put(key, cleanList);
                    } else {
                        businessData.put(key, value);
                    }
                }
            }
            
            return businessData.isEmpty() ? rawResult : businessData;
        }
        
        // 如果是字符串，清理特殊字符
        if (rawResult instanceof String) {
            return cleanStringData((String) rawResult);
        }
        
        // 如果是List，递归处理每个元素
        if (rawResult instanceof List) {
            @SuppressWarnings("unchecked")
            List<Object> list = (List<Object>) rawResult;
            List<Object> cleanList = new ArrayList<>();
            for (Object item : list) {
                cleanList.add(formatInvokeResult(item));
            }
            return cleanList;
        }
        
        return rawResult;
    }
    
    /**
     * 判断是否为系统字段（基于Dubbo协议规范）
     */
    private boolean isSystemField(String fieldName) {
        if (fieldName == null) {
            return true;
        }
        
        // Dubbo系统字段
        if (fieldName.startsWith("dubbo.") || fieldName.startsWith("_")) {
            return true;
        }
        
        // 常见的系统字段
        switch (fieldName.toLowerCase()) {
            case "class":
            case "hashcode":
            case "tostring":
            case "serialversionuid":
            case "type":
            case "ref":
            case "objectref":
            case "classdef":
            case "typeref":
                return true;
            default:
                return false;
        }
    }
    
    /**
     * 清理字符串数据中的特殊字符（基于UTF-8最佳实践）
     */
    private String cleanStringData(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        
        // 移除BOM字符和控制字符
        StringBuilder cleaned = new StringBuilder();
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            // 保留可打印字符和常用空白字符
            if (c >= 32 && c <= 126) { // ASCII可打印字符
                cleaned.append(c);
            } else if (c == '\t' || c == '\n' || c == '\r') { // 保留制表符和换行符
                cleaned.append(c);
            } else if (c > 127) { // Unicode字符
                cleaned.append(c);
            }
        }
        
        return cleaned.toString();
    }
    
    /**
     * 构建Dubbo请求数据包
     */
    private byte[] buildDubboRequest(String serviceName, String methodName, Class<?>[] parameterTypes, Object[] arguments) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        
        // Dubbo协议头（16字节）
        baos.write((MAGIC >>> 8) & 0xFF); // 魔数高位
        baos.write(MAGIC & 0xFF);         // 魔数低位
        baos.write(FLAG_REQUEST | FLAG_TWOWAY | SERIALIZATION_HESSIAN2); // 标志位
        baos.write(0); // 状态位
        
        // 请求ID（8字节）
        long requestId = REQUEST_ID.getAndIncrement();
        for (int i = 7; i >= 0; i--) {
            baos.write((int) (requestId >>> (i * 8)) & 0xFF);
        }
        
        // 构建请求体
        ByteArrayOutputStream bodyStream = new ByteArrayOutputStream();
        
        // Hessian2序列化请求体
        writeHessianString(bodyStream, "2.0.2"); // Dubbo版本
        writeHessianString(bodyStream, serviceName); // 服务名
        writeHessianString(bodyStream, "0.0.0"); // 服务版本
        writeHessianString(bodyStream, methodName); // 方法名
        
        // 参数类型描述
        if (parameterTypes != null && parameterTypes.length > 0) {
            StringBuilder typeDesc = new StringBuilder();
            for (Class<?> type : parameterTypes) {
                typeDesc.append(getTypeDescriptor(type));
            }
            writeHessianString(bodyStream, typeDesc.toString());
        } else {
            writeHessianString(bodyStream, "");
        }
        
        // 参数值
        if (arguments != null) {
            for (Object arg : arguments) {
                writeHessianObject(bodyStream, arg);
            }
        }
        
        // 附加参数（空Map）
        writeHessianMap(bodyStream);
        
        byte[] bodyData = bodyStream.toByteArray();
        
        // 写入数据长度（4字节）
        int length = bodyData.length;
        baos.write((length >>> 24) & 0xFF);
        baos.write((length >>> 16) & 0xFF);
        baos.write((length >>> 8) & 0xFF);
        baos.write(length & 0xFF);
        
        // 写入请求体
        baos.write(bodyData);
        
        return baos.toByteArray();
    }
    
    /**
     * 写入Hessian字符串（基于UTF-8编码）
     */
    private void writeHessianString(OutputStream out, String str) throws IOException {
        if (str == null) {
            out.write('N'); // null
            return;
        }
        
        byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
        int length = bytes.length;
        
        if (length <= 31) {
            out.write(0x00 + length); // 短字符串
        } else if (length <= 1023) {
            out.write(0x30 + (length >>> 8)); // 中等字符串高位
            out.write(length & 0xFF);         // 中等字符串低位
        } else {
            out.write('S'); // 长字符串标记
            writeInt(out, length);
        }
        
        out.write(bytes);
    }
    
    /**
     * 写入32位整数
     */
    private void writeInt(OutputStream out, int value) throws IOException {
        out.write((value >>> 24) & 0xFF);
        out.write((value >>> 16) & 0xFF);
        out.write((value >>> 8) & 0xFF);
        out.write(value & 0xFF);
    }
    
    /**
     * 写入Hessian对象
     */
    private void writeHessianObject(OutputStream out, Object obj) throws IOException {
        if (obj == null) {
            out.write('N'); // null
        } else if (obj instanceof String) {
            writeHessianString(out, (String) obj);
        } else if (obj instanceof Integer) {
            int value = (Integer) obj;
            if (value >= -16 && value <= 47) {
                out.write(0x90 + value); // 紧凑整数
            } else if (value >= -2048 && value <= 2047) {
                out.write(0xc8 + (value >>> 8));
                out.write(value & 0xFF);
            } else if (value >= -262144 && value <= 262143) {
                out.write(0xd4 + (value >>> 16));
                out.write((value >>> 8) & 0xFF);
                out.write(value & 0xFF);
            } else {
                out.write('I');
                writeInt(out, value);
            }
        } else if (obj instanceof Long) {
            out.write('L');
            long value = (Long) obj;
            for (int i = 7; i >= 0; i--) {
                out.write((int) (value >>> (i * 8)) & 0xFF);
            }
        } else if (obj instanceof Boolean) {
            out.write((Boolean) obj ? 'T' : 'F');
        } else {
            // 其他类型暂时写为null
            out.write('N');
        }
    }
    
    /**
     * 写入空的Hessian Map
     */
    private void writeHessianMap(OutputStream out) throws IOException {
        out.write('H'); // Map开始
        out.write('Z'); // Map结束
    }
    
    /**
     * 获取类型描述符
     */
    private String getTypeDescriptor(Class<?> type) {
        if (type == String.class) return "Ljava/lang/String;";
        if (type == int.class || type == Integer.class) return "I";
        if (type == long.class || type == Long.class) return "J";
        if (type == boolean.class || type == Boolean.class) return "Z";
        if (type == double.class || type == Double.class) return "D";
        if (type == float.class || type == Float.class) return "F";
        if (type == byte.class || type == Byte.class) return "B";
        if (type == char.class || type == Character.class) return "C";
        if (type == short.class || type == Short.class) return "S";
        
        // 对象类型
        return "L" + type.getName().replace('.', '/') + ";";
    }
    
    /**
     * 解析Dubbo响应
     */
    private Object parseResponse(InputStream in) throws IOException {
        // 读取协议头
        byte[] header = new byte[16];
        int bytesRead = 0;
        while (bytesRead < 16) {
            int read = in.read(header, bytesRead, 16 - bytesRead);
            if (read == -1) {
                throw new IOException("连接意外关闭");
            }
            bytesRead += read;
        }
        
        // 验证魔数
        short magic = (short) (((header[0] & 0xFF) << 8) | (header[1] & 0xFF));
        if (magic != MAGIC) {
            throw new IOException("无效的Dubbo协议魔数: " + Integer.toHexString(magic));
        }
        
        // 检查响应状态
        byte flag = header[2];
        byte status = header[3];
        
        if (status != 20) { // OK状态
            logger.log("响应状态异常: " + status);
        }
        
        // 读取数据长度
        int length = ((header[12] & 0xFF) << 24) |
                    ((header[13] & 0xFF) << 16) |
                    ((header[14] & 0xFF) << 8) |
                    (header[15] & 0xFF);
        
        logger.log("响应数据长度: " + length);
        
        if (length <= 0) {
            return null;
        }
        
        // 读取响应体
        byte[] bodyData = new byte[length];
        bytesRead = 0;
        while (bytesRead < length) {
            int read = in.read(bodyData, bytesRead, length - bytesRead);
            if (read == -1) {
                throw new IOException("响应数据读取不完整");
            }
            bytesRead += read;
        }
        
        // 解析Hessian2响应体
        ByteArrayInputStream bis = new ByteArrayInputStream(bodyData);
        
        try {
            // 读取响应类型标记
            int responseType = bis.read();
            logger.log("响应类型标记: 0x" + Integer.toHexString(responseType));
            
            if (responseType == -1) {
                return null;
            }
            
            // 解析响应值
            Object result = readHessianValue(bis);
            logger.log("解析结果类型: " + (result != null ? result.getClass().getSimpleName() : "null"));
            
            return result;
            
        } catch (Exception e) {
            logger.logException(e);
            return parseExceptionInfo(new String(bodyData, StandardCharsets.UTF_8));
        }
    }
    
    /**
     * 读取Hessian字符串（修复UTF-8字符长度计算）
     */
    private String readHessianString(InputStream in) throws IOException {
        int type = in.read();
        if (type == -1) {
            throw new IOException("流意外结束");
        }
        
        if (type == 'N') {
            return null;
        }
        
        int charLength; // 字符长度，不是字节长度
        if (type >= 0x00 && type <= 0x1f) {
            // 短字符串 (0-31字符)
            charLength = type;
        } else if (type >= 0x30 && type <= 0x33) {
            // 中等字符串 (0-1023字符)
            int high = type - 0x30;
            int low = in.read();
            if (low == -1) throw new IOException("流意外结束");
            charLength = (high << 8) + low;
        } else if (type == 'S') {
            // 长字符串
            charLength = readInt(in);
        } else {
            throw new IOException("无效的字符串类型标记: 0x" + Integer.toHexString(type));
        }
        
        return readStringByCharLength(in, charLength);
    }
    
    /**
     * 读取UTF-8字符（处理多字节字符）
     */
    private String readUtf8Character(InputStream in, int firstByte) throws IOException {
        try {
            if (firstByte >= 0xe0 && firstByte <= 0xef) {
                // 3字节UTF-8字符
                int byte2 = in.read();
                int byte3 = in.read();
                if (byte2 == -1 || byte3 == -1) {
                    return "[UTF-8解码失败]";
                }
                byte[] bytes = {(byte) firstByte, (byte) byte2, (byte) byte3};
                return new String(bytes, StandardCharsets.UTF_8);
            } else if (firstByte >= 0xc0 && firstByte <= 0xdf) {
                // 2字节UTF-8字符
                int byte2 = in.read();
                if (byte2 == -1) {
                    return "[UTF-8解码失败]";
                }
                byte[] bytes = {(byte) firstByte, (byte) byte2};
                return new String(bytes, StandardCharsets.UTF_8);
            } else {
                // 单字节字符
                byte[] bytes = {(byte) firstByte};
                return new String(bytes, StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            logger.log("UTF-8字符解码异常: " + e.getMessage());
            return "[UTF-8解码异常]";
        }
    }

    /**
     * 读取UTF-8多字节字符串
     */
    private String readUtf8MultiByteString(InputStream in, int firstByte) throws IOException {
        // 检查是否为UTF-8多字节字符的开始
        if (firstByte >= 0xe0 && firstByte <= 0xef) {
            // 三字节UTF-8字符（如中文）
            int byte2 = in.read();
            int byte3 = in.read();
            if (byte2 == -1 || byte3 == -1) {
                return "[未知类型:0x" + Integer.toHexString(firstByte) + "]";
            }
            
            // 验证是否为有效的UTF-8序列
            if ((byte2 & 0x80) == 0x80 && (byte3 & 0x80) == 0x80) {
                byte[] utf8Bytes = {(byte) firstByte, (byte) byte2, (byte) byte3};
                try {
                    String result = new String(utf8Bytes, StandardCharsets.UTF_8);
                    // 检查是否包含替换字符，如果包含说明不是有效的UTF-8
                    if (!result.contains("\uFFFD")) {
                        return result;
                    }
                } catch (Exception e) {
                    // 解码失败
                }
            }
        } else if (firstByte >= 0xc0 && firstByte <= 0xdf) {
            // 两字节UTF-8字符
            int byte2 = in.read();
            if (byte2 == -1) {
                return "[未知类型:0x" + Integer.toHexString(firstByte) + "]";
            }
            
            if ((byte2 & 0x80) == 0x80) {
                byte[] utf8Bytes = {(byte) firstByte, (byte) byte2};
                try {
                    String result = new String(utf8Bytes, StandardCharsets.UTF_8);
                    if (!result.contains("\uFFFD")) {
                        return result;
                    }
                } catch (Exception e) {
                    // 解码失败
                }
            }
        }
        
        // 如果不是有效的UTF-8序列，返回未知类型
        return "[未知类型:0x" + Integer.toHexString(firstByte) + "]";
    }

    /**
     * 根据字符长度读取Hessian2字符串（修复中文乱码问题）
     */
    private String readStringByCharLength(InputStream in, int charLength) throws IOException {
        if (charLength < 0) {
            throw new IOException("字符长度不能为负数: " + charLength);
        }
        
        if (charLength == 0) {
            return "";
        }
        
        // 预估字节长度（中文字符通常3字节，英文1字节）
        int estimatedByteLength = charLength * 3;
        if (estimatedByteLength > 10 * 1024 * 1024) { // 10MB限制
            throw new IOException("字符串长度过大: " + charLength);
        }
        
        StringBuilder result = new StringBuilder(charLength);
        int charsRead = 0;
        
        while (charsRead < charLength) {
            int firstByte = in.read();
            if (firstByte == -1) {
                throw new IOException("字符串数据读取不完整，期望字符数: " + charLength + ", 实际读取: " + charsRead);
            }
            
            String character;
            if (firstByte < 0x80) {
                // ASCII字符 (0-127)
                character = String.valueOf((char) firstByte);
            } else if ((firstByte & 0xE0) == 0xC0) {
                // 2字节UTF-8字符
                int secondByte = in.read();
                if (secondByte == -1) {
                    throw new IOException("UTF-8字符读取不完整");
                }
                byte[] bytes = {(byte) firstByte, (byte) secondByte};
                character = new String(bytes, StandardCharsets.UTF_8);
            } else if ((firstByte & 0xF0) == 0xE0) {
                // 3字节UTF-8字符（中文等）
                int secondByte = in.read();
                int thirdByte = in.read();
                if (secondByte == -1 || thirdByte == -1) {
                    throw new IOException("UTF-8字符读取不完整");
                }
                byte[] bytes = {(byte) firstByte, (byte) secondByte, (byte) thirdByte};
                character = new String(bytes, StandardCharsets.UTF_8);
            } else if ((firstByte & 0xF8) == 0xF0) {
                // 4字节UTF-8字符
                int secondByte = in.read();
                int thirdByte = in.read();
                int fourthByte = in.read();
                if (secondByte == -1 || thirdByte == -1 || fourthByte == -1) {
                    throw new IOException("UTF-8字符读取不完整");
                }
                byte[] bytes = {(byte) firstByte, (byte) secondByte, (byte) thirdByte, (byte) fourthByte};
                character = new String(bytes, StandardCharsets.UTF_8);
            } else {
                // 无效的UTF-8序列，作为单字节处理
                character = String.valueOf((char) (firstByte & 0xFF));
            }
            
            result.append(character);
            charsRead++;
        }
        
        return result.toString();
    }
    
    /**
     * 读取指定长度的字符串字节（基于UTF-8编码和Dubbo最佳实践）
     */
    private String readStringBytes(InputStream in, int length) throws IOException {
        if (length < 0) {
            throw new IOException("字符串长度不能为负数: " + length);
        }
        
        if (length == 0) {
            return "";
        }
        
        // 限制最大字符串长度，防止内存溢出
        if (length > 10 * 1024 * 1024) { // 10MB限制
            throw new IOException("字符串长度过大: " + length);
        }
        
        byte[] bytes = new byte[length];
        int bytesRead = 0;
        while (bytesRead < length) {
            int read = in.read(bytes, bytesRead, length - bytesRead);
            if (read == -1) {
                throw new IOException("字符串数据读取不完整，期望: " + length + ", 实际: " + bytesRead);
            }
            bytesRead += read;
        }
        
        // 使用UTF-8解码，符合Dubbo官方建议
        String result = new String(bytes, StandardCharsets.UTF_8);
        
        // 移除BOM字符（如果存在）
        if (result.length() > 0 && result.charAt(0) == '\uFEFF') {
            result = result.substring(1);
        }
        
        return result;
    }
    
    /**
     * 读取32位整数
     */
    private int readInt(InputStream in) throws IOException {
        int b1 = in.read();
        int b2 = in.read();
        int b3 = in.read();
        int b4 = in.read();
        
        if (b1 == -1 || b2 == -1 || b3 == -1 || b4 == -1) {
            throw new IOException("整数读取不完整");
        }
        
        return (b1 << 24) | (b2 << 16) | (b3 << 8) | b4;
    }
    
    /**
     * 读取Hessian值（入口方法）
     */
    private Object readHessianValue(InputStream in) throws IOException {
        return readHessianValue(in, 0);
    }
    
    /**
     * 读取Hessian值（基于Dubbo官方递归深度限制）
     */
    private Object readHessianValue(InputStream in, int depth) throws IOException {
        // 基于Dubbo 2.6.6+的递归深度限制
        if (depth > MAX_RECURSION_DEPTH) {
            logger.log("达到最大递归深度限制: " + depth);
            return "[递归深度超限]";
        }
        
        int type = in.read();
        if (type == -1) {
            return null;
        }
        
        logger.log("读取Hessian值，类型: 0x" + Integer.toHexString(type) + ", 深度: " + depth);
        
        switch (type) {
            case 'N': // Null
                return null;
                
            case 'T': // Boolean true
                return Boolean.TRUE;
                
            case 'F': // Boolean false
                return Boolean.FALSE;
                
            case 'I': // 32位整数
                return readInt(in);
                
            case 'L': // 64位长整数
                return readLong(in);
                
            case 'D': // 64位双精度浮点数
                return readDouble(in);
                
            case 'S': // 字符串
                int charLength = readInt(in);
                return readStringByCharLength(in, charLength);
                
            case 'O': // 对象
                return readHessianObject(in, type, depth + 1);
                
            case 'C': // 类定义或异常
                return readClassDefinitionOrException(in, Math.min(depth + 1, MAX_CLASS_DEF_DEPTH));
                
            case 'H': // Map
                return readHessianMap(in);
                
            case 'V': // List/Array
                return readHessianList(in, type);
                
            case 'R': // 对象引用
                int refId = readInt(in);
                logger.log("对象引用ID: " + refId);
                return "[对象引用:" + refId + "]";
                
            default:
                // 处理紧凑编码
                if (type >= 0x00 && type <= 0x1f) {
                    // 短字符串（字符长度，不是字节长度）
                    return readStringByCharLength(in, type);
                } else if (type >= 0x20 && type <= 0x2f) {
                    // 二进制数据
                    int binLength = type - 0x20;
                    byte[] binData = new byte[binLength];
                    in.read(binData);
                    return new String(binData, StandardCharsets.UTF_8);
                } else if (type >= 0x30 && type <= 0x33) {
                    // 中等长度字符串（字符长度，不是字节长度）
                    int high = type - 0x30;
                    int low = in.read();
                    int strLength = (high << 8) + low;
                    return readStringByCharLength(in, strLength);
                } else if (type >= 0x80 && type <= 0xbf) {
                    // 紧凑整数 (-16 到 47)
                    return type - 0x90;
                } else if (type >= 0xc0 && type <= 0xcf) {
                    // 两字节整数
                    int low = in.read();
                    return ((type - 0xc8) << 8) + low;
                } else if (type >= 0xd0 && type <= 0xd7) {
                    // 三字节整数
                    int mid = in.read();
                    int low = in.read();
                    return ((type - 0xd4) << 16) + (mid << 8) + low;
                } else if (type >= 0xe0 && type <= 0xe9) {
                    // 长整数的紧凑编码 (Hessian2协议)
                    long value = type - 0xe0;
                    return value;
                } else if (type >= 0xf0 && type <= 0xff) {
                    // 长整数的单字节编码 (Hessian2协议)
                    long value = type - 0x100;
                    return value;
                } else {
                    // 对于其他字节，尝试作为单字节字符串处理
                    logger.log("尝试将字节 0x" + Integer.toHexString(type) + " 作为字符处理");
                    byte[] singleByte = {(byte) type};
                    String result = new String(singleByte, StandardCharsets.UTF_8);
                    // 如果解码结果是有效字符，返回它；否则标记为未知类型
                    if (result.length() > 0 && !result.equals("\uFFFD")) {
                        return result;
                    } else {
                        logger.log("未知类型标记: 0x" + Integer.toHexString(type));
                        return "[未知类型:0x" + Integer.toHexString(type) + "]";
                    }
                }
        }
    }
    
    /**
     * 读取64位长整数
     */
    private long readLong(InputStream in) throws IOException {
        long result = 0;
        for (int i = 0; i < 8; i++) {
            int b = in.read();
            if (b == -1) {
                throw new IOException("长整数读取不完整");
            }
            result = (result << 8) | (b & 0xFF);
        }
        return result;
    }
    
    /**
     * 读取64位双精度浮点数
     */
    private double readDouble(InputStream in) throws IOException {
        long bits = readLong(in);
        return Double.longBitsToDouble(bits);
    }
    
    /**
     * 读取Hessian对象（入口方法）
     */
    private Object readHessianObject(InputStream in, int type) throws IOException {
        return readHessianObject(in, type, 0);
    }
    
    /**
     * 读取Hessian对象（带递归深度控制）
     */
    private Object readHessianObject(InputStream in, int type, int depth) throws IOException {
        if (depth > MAX_RECURSION_DEPTH) {
            logger.log("对象解析达到最大递归深度: " + depth);
            return "[对象递归超限]";
        }
        
        logger.log("读取Hessian对象，类型: 0x" + Integer.toHexString(type) + ", 深度: " + depth);
        
        // 读取类型引用
        int typeRef = readInt(in);
        logger.log("对象类型引用: " + typeRef);
        
        // 尝试解析对象数据
        Map<String, Object> objectData = new HashMap<>();
        
        try {
            // 读取剩余的流数据
            ByteArrayOutputStream remaining = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int bytesRead;
            
            while (in.available() > 0 && (bytesRead = in.read(buffer)) != -1) {
                remaining.write(buffer, 0, bytesRead);
            }
            
            byte[] remainingData = remaining.toByteArray();
            if (remainingData.length > 0) {
                // 尝试解析为结构化数据
                Map<String, Object> parsedData = parseHessianObjectData(remainingData, typeRef);
                if (parsedData != null && !parsedData.isEmpty()) {
                    objectData.putAll(parsedData);
                } else {
                    // 如果结构化解析失败，尝试作为字符串处理
                    String dataStr = cleanBinaryData(remainingData);
                    if (!dataStr.trim().isEmpty()) {
                        objectData.put("data", dataStr);
                    }
                }
            }
            
        } catch (Exception e) {
            logger.log("对象数据解析异常: " + e.getMessage());
            objectData.put("error", "解析失败: " + e.getMessage());
        }
        
        return objectData.isEmpty() ? "[空对象]" : objectData;
    }
    
    /**
     * 解析Hessian对象数据（改进版）
     */
    private Map<String, Object> parseHessianObjectData(byte[] data, int typeRef) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            ByteArrayInputStream bis = new ByteArrayInputStream(data);
            
            // 尝试读取字段
            int fieldIndex = 0;
            while (bis.available() > 0 && fieldIndex < 50) { // 增加字段数量限制
                try {
                    Object value = readHessianValueSafe(bis, fieldIndex);
                    if (value != null) {
                        String fieldName = inferFieldName(value, fieldIndex);
                        if (!isSystemField(fieldName)) {
                            result.put(fieldName, value);
                        }
                    }
                    fieldIndex++;
                } catch (Exception e) {
                    logger.log("字段解析失败[" + fieldIndex + "]: " + e.getMessage());
                    // 尝试跳过一些字节继续解析
                    if (bis.available() > 0) {
                        bis.skip(1);
                    } else {
                        break;
                    }
                }
            }
            
        } catch (Exception e) {
            logger.log("对象数据解析异常: " + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 安全的Hessian值读取方法
     */
    private Object readHessianValueSafe(InputStream in, int fieldIndex) throws IOException {
        try {
            return readHessianValue(in, 0);
        } catch (Exception e) {
            logger.log("字段[" + fieldIndex + "]读取异常，尝试恢复: " + e.getMessage());
            // 尝试读取剩余字节作为字符串
            if (in.available() > 0) {
                byte[] remaining = new byte[Math.min(in.available(), 100)];
                int bytesRead = in.read(remaining);
                if (bytesRead > 0) {
                    String recovered = new String(remaining, 0, bytesRead, StandardCharsets.UTF_8)
                        .replaceAll("[\\x00-\\x1F\\x7F-\\x9F]", ""); // 移除控制字符
                    if (!recovered.trim().isEmpty()) {
                        return recovered.trim();
                    }
                }
            }
            return "[解析失败]";
        }
    }
    
    /**
     * 推断字段名称
     */
    private String inferFieldName(Object value, int index) {
        if (value == null) {
            return "field" + index;
        }
        
        String valueStr = value.toString();
        
        // 基于值的内容推断字段名
        if (valueStr.matches("\\d+")) {
            return "id";
        } else if (valueStr.contains("@")) {
            return "email";
        } else if (valueStr.matches("\\d{4}-\\d{2}-\\d{2}.*")) {
            return "date";
        } else if (valueStr.toLowerCase().contains("name")) {
            return "name";
        } else if (valueStr.toLowerCase().contains("code")) {
            return "code";
        } else if (valueStr.toLowerCase().contains("msg") || valueStr.toLowerCase().contains("message")) {
            return "message";
        } else if (valueStr.toLowerCase().contains("status")) {
            return "status";
        } else if (valueStr.toLowerCase().contains("result")) {
            return "result";
        } else if (valueStr.toLowerCase().contains("data")) {
            return "data";
        } else if (valueStr.toLowerCase().contains("success")) {
            return "success";
        } else if (valueStr.toLowerCase().contains("error")) {
            return "error";
        } else if (value instanceof Number) {
            return "number" + index;
        } else if (value instanceof Boolean) {
            return "flag" + index;
        } else {
            return "field" + index;
        }
    }
    
    /**
     * 清理二进制数据为可读字符串
     */
    private String cleanBinaryData(byte[] data) {
        if (data == null || data.length == 0) {
            return "";
        }
        
        // 尝试UTF-8解码
        try {
            String utf8Str = new String(data, StandardCharsets.UTF_8);
            
            // 过滤掉不可打印字符
            StringBuilder cleaned = new StringBuilder();
            for (int i = 0; i < utf8Str.length(); i++) {
                char c = utf8Str.charAt(i);
                if (c >= 32 && c <= 126) { // ASCII可打印字符
                    cleaned.append(c);
                } else if (c == '\t' || c == '\n' || c == '\r') {
                    cleaned.append(c);
                } else if (c > 127) { // Unicode字符
                    cleaned.append(c);
                } else {
                    cleaned.append('?'); // 替换不可打印字符
                }
            }
            
            String result = cleaned.toString().trim();
            return result.isEmpty() ? "[二进制数据]" : result;
            
        } catch (Exception e) {
            // UTF-8解码失败，返回十六进制表示
            StringBuilder hex = new StringBuilder();
            for (int i = 0; i < Math.min(data.length, 100); i++) { // 限制长度
                hex.append(String.format("%02x ", data[i] & 0xFF));
            }
            return "[HEX: " + hex.toString().trim() + (data.length > 100 ? "..." : "") + "]";
        }
    }
    
    /**
     * 读取Hessian Map
     */
    private Object readHessianMap(InputStream in) throws IOException {
        Map<String, Object> map = new HashMap<>();
        
        while (true) {
            int keyType = in.read();
            if (keyType == -1 || keyType == 'Z') { // Map结束标记
                break;
            }
            
            // 回退一个字节
            if (in.markSupported()) {
                in.reset();
            }
            
            Object key = readHessianValue(in);
            Object value = readHessianValue(in);
            
            if (key != null) {
                map.put(key.toString(), value);
            }
        }
        
        return map;
    }
    
    /**
     * 读取Hessian List
     */
    private Object readHessianList(InputStream in, int type) throws IOException {
        List<Object> list = new ArrayList<>();
        
        // 读取长度（如果有）
        int length = -1;
        if (type == 'V') {
            // 可变长度列表，读取到结束标记
        } else {
            // 固定长度列表
            length = readInt(in);
        }
        
        int count = 0;
        while (count < (length > 0 ? length : 1000)) { // 限制最大元素数量
            int elementType = in.read();
            if (elementType == -1 || elementType == 'Z') {
                break;
            }
            
            // 回退一个字节
            if (in.markSupported()) {
                in.reset();
            }
            
            Object element = readHessianValue(in);
            list.add(element);
            count++;
        }
        
        return list;
    }
    
    /**
     * 读取类定义或异常（ByteBuffer版本）
     */
    private Object readClassDefinitionOrException(ByteBuffer buffer) throws IOException {
        return readClassDefinitionOrException(new ByteArrayInputStream(buffer.array()), 0);
    }
    
    /**
     * 读取类定义或异常（InputStream版本，入口方法）
     */
    private Object readClassDefinitionOrException(InputStream in) throws IOException {
        return readClassDefinitionOrException(in, 0);
    }
    
    /**
     * 读取类定义或异常（基于Dubbo官方最佳实践，增强递归控制）
     */
    private Object readClassDefinitionOrException(InputStream in, int depth) throws IOException {
        // 基于Dubbo官方建议的类定义递归深度限制
        if (depth > MAX_CLASS_DEF_DEPTH) {
            logger.log("类定义解析达到最大递归深度: " + depth);
            return "[类定义递归超限]";
        }
        
        logger.log("开始解析类定义或异常，深度: " + depth);
        
        try {
            // 读取类名
            Object className = readHessianValue(in, depth + 1);
            logger.log("类名: " + className);
            
            // 读取字段数量
            Object fieldCountObj = readHessianValue(in, depth + 1);
            int fieldCount = 0;
            if (fieldCountObj instanceof Number) {
                fieldCount = ((Number) fieldCountObj).intValue();
            }
            logger.log("字段数量: " + fieldCount);
            
            // 限制字段数量，防止恶意数据
            if (fieldCount > 100) {
                logger.log("字段数量过多，限制为100: " + fieldCount);
                fieldCount = 100;
            }
            
            // 先读取所有字段名（类定义阶段）
            String[] fieldNames = new String[fieldCount];
            for (int i = 0; i < fieldCount; i++) {
                Object fieldName = readHessianValue(in, depth + 1);
                fieldNames[i] = fieldName != null ? fieldName.toString() : "field" + i;
                logger.log("字段名[" + i + "]: " + fieldNames[i]);
            }
            
            // 创建对象映射来存储业务数据
            Map<String, Object> objectMap = new HashMap<>();
            
            // 读取对象实例数据（按字段名顺序读取字段值）
            for (int i = 0; i < fieldCount; i++) {
                try {
                    Object fieldValue = readHessianValue(in, depth + 1);
                    // 只保存非系统字段
                    if (!isSystemField(fieldNames[i])) {
                        objectMap.put(fieldNames[i], fieldValue);
                        logger.log("Hessian值[" + fieldNames[i] + "]: " + fieldValue);
                    }
                } catch (Exception e) {
                    logger.log("读取字段值[" + fieldNames[i] + "]失败: " + e.getMessage());
                    // 继续处理下一个字段
                }
            }
            
            // 如果还有剩余数据，尝试继续解析
            if (in.available() > 0) {
                logger.log("还有剩余数据，继续解析...");
                try {
                    Object additionalData = readHessianValue(in, depth + 1);
                    if (additionalData != null) {
                        objectMap.put("additionalData", additionalData);
                    }
                } catch (Exception e) {
                    logger.log("解析剩余数据失败: " + e.getMessage());
                }
            }
            
            logger.log("类定义解析完成，返回对象: " + objectMap);
            return objectMap.isEmpty() ? "[空类定义]" : objectMap;
            
        } catch (Exception e) {
            logger.log("结构化解析失败，尝试简单解析: " + e.getMessage());
            
            // 如果结构化解析失败，尝试简单解析
            try {
                // 读取剩余数据作为字符串
                ByteArrayOutputStream remaining = new ByteArrayOutputStream();
                byte[] buffer = new byte[1024];
                int bytesRead;
                
                while (in.available() > 0 && (bytesRead = in.read(buffer)) != -1) {
                    remaining.write(buffer, 0, bytesRead);
                }
                
                String dataStr = new String(remaining.toByteArray(), StandardCharsets.UTF_8);
                
                // 尝试提取异常信息
                Object exceptionInfo = parseExceptionInfo(dataStr);
                if (exceptionInfo != null) {
                    return exceptionInfo;
                }
                
                // 清理并返回数据
                String cleanedData = cleanStringData(dataStr);
                return cleanedData.isEmpty() ? "[解析失败]" : cleanedData;
                
            } catch (Exception ex) {
                logger.log("简单解析也失败: " + ex.getMessage());
                return "[解析异常: " + ex.getMessage() + "]";
            }
        }
    }
    
    /**
     * 解析异常信息
     */
    private Object parseExceptionInfo(String dataStr) {
        if (dataStr == null || dataStr.trim().isEmpty()) {
            return null;
        }
        
        Map<String, Object> exceptionInfo = new HashMap<>();
        
        // 查找常见的异常模式
        if (dataStr.contains("Exception") || dataStr.contains("Error")) {
            exceptionInfo.put("type", "exception");
            
            // 提取异常类型
            String[] lines = dataStr.split("\n");
            for (String line : lines) {
                line = line.trim();
                if (line.contains("Exception") || line.contains("Error")) {
                    exceptionInfo.put("message", line);
                    break;
                }
            }
        }
        
        // 查找错误码和消息
        if (dataStr.contains("code") || dataStr.contains("message")) {
            String[] parts = dataStr.split("[,;\n]");
            for (String part : parts) {
                part = part.trim();
                if (part.toLowerCase().contains("code")) {
                    exceptionInfo.put("errorCode", part);
                } else if (part.toLowerCase().contains("message") || part.toLowerCase().contains("msg")) {
                    exceptionInfo.put("errorMessage", part);
                }
            }
        }
        
        // 如果没有找到异常信息，返回清理后的原始数据
        if (exceptionInfo.isEmpty()) {
            String cleaned = cleanStringData(dataStr);
            if (!cleaned.isEmpty()) {
                exceptionInfo.put("data", cleaned);
            }
        }
        
        return exceptionInfo.isEmpty() ? null : exceptionInfo;
    }
}