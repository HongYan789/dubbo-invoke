package com.hongyan.dubboinvoke.util;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 操作日志记录器
 * 用于记录Dubbo调用过程中的详细操作和错误信息
 */
public final class OperationLogger {
    private static volatile OperationLogger INSTANCE;
    private final ConcurrentLinkedQueue<String> logBuffer = new ConcurrentLinkedQueue<>();
    private final AtomicInteger logCounter = new AtomicInteger(0);
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
    private final String logDir;
    private final String logFile;
    
    private OperationLogger() {
        // 获取用户主目录下的日志目录
        String userHome = System.getProperty("user.home");
        logDir = userHome + "/.dubbo-invoke-plugin/logs";
        logFile = logDir + "/operation-" + System.currentTimeMillis() + ".log";
        
        // 创建日志目录
        try {
            Files.createDirectories(Paths.get(logDir));
        } catch (IOException e) {
            System.err.println("无法创建日志目录: " + logDir);
        }
        
        // 启动定时刷新线程
        startFlushThread();
    }
    
    public static OperationLogger getInstance() {
        if (INSTANCE == null) {
            synchronized (OperationLogger.class) {
                if (INSTANCE == null) {
                    INSTANCE = new OperationLogger();
                }
            }
        }
        return INSTANCE;
    }
    
    /**
     * 记录普通日志
     */
    public void log(String message) {
        String logEntry = String.format("[%s] [%d] %s", 
            dateFormat.format(new Date()), 
            logCounter.incrementAndGet(), 
            message);
        
        logBuffer.offer(logEntry);
        System.out.println(logEntry); // 同时输出到控制台
    }
    
    /**
     * 记录异常信息
     */
    public void logException(Throwable throwable) {
        log("异常: " + throwable.getClass().getSimpleName() + " - " + throwable.getMessage());
        
        // 记录完整堆栈
        StringBuilder stackTrace = new StringBuilder();
        stackTrace.append("完整堆栈:\n");
        
        Throwable current = throwable;
        int depth = 0;
        while (current != null && depth < 10) {
            stackTrace.append("Caused by: ").append(current.getClass().getName())
                     .append(": ").append(current.getMessage()).append("\n");
            
            for (StackTraceElement element : current.getStackTrace()) {
                stackTrace.append("  at ").append(element.toString()).append("\n");
            }
            
            current = current.getCause();
            depth++;
        }
        
        logBuffer.offer(stackTrace.toString());
    }
    
    /**
     * 记录Dubbo调用信息
     */
    public void logDubboInvoke(String serviceInterface, String serviceUrl, 
                              String methodName, String[] parameterTypes, Object[] parameters) {
        StringBuilder sb = new StringBuilder();
        sb.append("Dubbo调用信息:\n");
        sb.append("  服务接口: ").append(serviceInterface).append("\n");
        sb.append("  服务地址: ").append(serviceUrl).append("\n");
        sb.append("  方法名: ").append(methodName).append("\n");
        sb.append("  参数类型: ");
        if (parameterTypes != null) {
            for (int i = 0; i < parameterTypes.length; i++) {
                if (i > 0) sb.append(", ");
                sb.append(parameterTypes[i]);
            }
        }
        sb.append("\n");
        sb.append("  参数值: ");
        if (parameters != null) {
            for (int i = 0; i < parameters.length; i++) {
                if (i > 0) sb.append(", ");
                sb.append(parameters[i] != null ? parameters[i].toString() : "null");
            }
        }
        sb.append("\n");
        
        log(sb.toString());
    }
    
    /**
     * 记录系统信息
     */
    public void logSystemInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append("系统信息:\n");
        sb.append("  Java版本: ").append(System.getProperty("java.version")).append("\n");
        sb.append("  Java供应商: ").append(System.getProperty("java.vendor")).append("\n");
        sb.append("  操作系统: ").append(System.getProperty("os.name")).append(" ").append(System.getProperty("os.version")).append("\n");
        sb.append("  用户目录: ").append(System.getProperty("user.home")).append("\n");
        sb.append("  工作目录: ").append(System.getProperty("user.dir")).append("\n");
        sb.append("  类路径: ").append(System.getProperty("java.class.path")).append("\n");
        
        log(sb.toString());
    }
    
    /**
     * 强制刷新日志到文件
     */
    public void flush() {
        if (logBuffer.isEmpty()) return;
        
        try (FileWriter writer = new FileWriter(logFile, true);
             PrintWriter pw = new PrintWriter(writer)) {
            
            while (!logBuffer.isEmpty()) {
                String entry = logBuffer.poll();
                if (entry != null) {
                    pw.println(entry);
                }
            }
            pw.flush();
        } catch (IOException e) {
            System.err.println("写入日志文件失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取日志文件路径
     */
    public String getLogFilePath() {
        return logFile;
    }
    
    /**
     * 启动定时刷新线程
     */
    private void startFlushThread() {
        Thread flushThread = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(5000); // 每5秒刷新一次
                    flush();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        flushThread.setDaemon(true);
        flushThread.setName("OperationLogger-Flush");
        flushThread.start();
    }
    
    /**
     * 关闭日志记录器
     */
    public void close() {
        flush();
    }
}