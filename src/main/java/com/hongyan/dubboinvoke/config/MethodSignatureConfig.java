package com.hongyan.dubboinvoke.config;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 方法签名缓存配置管理
 * 用于存储用户手动配置的方法参数类型，避免重复的类型推导
 */
@Service(Service.Level.PROJECT)
@State(
    name = "DubboMethodSignatureConfig", 
    storages = @Storage("dubbo-method-signatures.xml")
)
public final class MethodSignatureConfig implements PersistentStateComponent<MethodSignatureConfig> {
    
    /**
     * 方法签名缓存
     * Key: serviceInterface.methodName
     * Value: MethodSignature对象
     */
    public Map<String, MethodSignature> methodSignatures = new HashMap<>();
    
    /**
     * 方法签名数据结构
     */
    public static class MethodSignature {
        /** 服务接口 */
        public String serviceInterface = "";
        
        /** 方法名 */
        public String methodName = "";
        
        /** 参数类型列表 */
        public List<String> parameterTypes = new ArrayList<>();
        
        /** 参数名称列表 */
        public List<String> parameterNames = new ArrayList<>();
        
        /** 返回类型 */
        public String returnType = "";
        
        /** 描述信息 */
        public String description = "";
        
        /** 创建时间 */
        public long createTime = System.currentTimeMillis();
        
        /** 最后使用时间 */
        public long lastUsedTime = System.currentTimeMillis();
        
        /** 使用次数 */
        public int usageCount = 0;
        
        public MethodSignature() {
            // 默认构造函数，用于XML序列化
        }
        
        public MethodSignature(String serviceInterface, String methodName) {
            this.serviceInterface = serviceInterface;
            this.methodName = methodName;
        }
        
        /**
         * 获取方法的完整签名字符串
         */
        public String getFullSignature() {
            StringBuilder sb = new StringBuilder();
            sb.append(returnType).append(" ");
            sb.append(serviceInterface).append(".").append(methodName);
            sb.append("(");
            for (int i = 0; i < parameterTypes.size(); i++) {
                if (i > 0) sb.append(", ");
                sb.append(parameterTypes.get(i));
                if (i < parameterNames.size() && !parameterNames.get(i).isEmpty()) {
                    sb.append(" ").append(parameterNames.get(i));
                }
            }
            sb.append(")");
            return sb.toString();
        }
        
        /**
         * 获取方法唯一标识
         */
        public String getMethodKey() {
            return serviceInterface + "." + methodName;
        }
    }
    
    public static MethodSignatureConfig getInstance(@NotNull Project project) {
        return project.getService(MethodSignatureConfig.class);
    }
    
    @Override
    public @Nullable MethodSignatureConfig getState() {
        return this;
    }
    
    @Override
    public void loadState(@NotNull MethodSignatureConfig state) {
        XmlSerializerUtil.copyBean(state, this);
    }
    
    /**
     * 保存方法签名
     */
    public void saveMethodSignature(@NotNull MethodSignature signature) {
        String key = signature.getMethodKey();
        signature.lastUsedTime = System.currentTimeMillis();
        if (methodSignatures.containsKey(key)) {
            // 更新现有签名
            MethodSignature existing = methodSignatures.get(key);
            existing.parameterTypes = new ArrayList<>(signature.parameterTypes);
            existing.parameterNames = new ArrayList<>(signature.parameterNames);
            existing.returnType = signature.returnType;
            existing.description = signature.description;
            existing.lastUsedTime = signature.lastUsedTime;
            existing.usageCount++;
        } else {
            // 新增签名
            signature.createTime = System.currentTimeMillis();
            signature.usageCount = 1;
            methodSignatures.put(key, signature);
        }
    }
    
    /**
     * 获取方法签名
     */
    @Nullable
    public MethodSignature getMethodSignature(@NotNull String serviceInterface, @NotNull String methodName) {
        String key = serviceInterface + "." + methodName;
        MethodSignature signature = methodSignatures.get(key);
        if (signature != null) {
            signature.lastUsedTime = System.currentTimeMillis();
            signature.usageCount++;
        }
        return signature;
    }
    
    /**
     * 删除方法签名
     */
    public void removeMethodSignature(@NotNull String serviceInterface, @NotNull String methodName) {
        String key = serviceInterface + "." + methodName;
        methodSignatures.remove(key);
    }
    
    /**
     * 获取所有方法签名列表
     */
    @NotNull
    public List<MethodSignature> getAllMethodSignatures() {
        return new ArrayList<>(methodSignatures.values());
    }
    
    /**
     * 清空所有缓存
     */
    public void clearAll() {
        methodSignatures.clear();
    }
    
    /**
     * 获取缓存统计信息
     */
    public CacheStats getCacheStats() {
        return new CacheStats(
            methodSignatures.size(),
            methodSignatures.values().stream().mapToInt(s -> s.usageCount).sum()
        );
    }
    
    /**
     * 缓存统计信息
     */
    public static class CacheStats {
        public final int totalMethods;
        public final int totalUsages;
        
        public CacheStats(int totalMethods, int totalUsages) {
            this.totalMethods = totalMethods;
            this.totalUsages = totalUsages;
        }
    }
}