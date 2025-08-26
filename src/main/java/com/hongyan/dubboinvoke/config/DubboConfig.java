package com.hongyan.dubboinvoke.config;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Dubbo配置管理
 */
@Service(Service.Level.PROJECT)
@State(
    name = "DubboInvokeConfig",
    storages = @Storage("dubbo-invoke.xml")
)
public final class DubboConfig implements PersistentStateComponent<DubboConfig> {
    
    // 注册中心地址
    public String registryAddress = "zookeeper://127.0.0.1:2181";
    
    // 应用名称
    public String applicationName = "dubbo-invoke-client";
    
    // 超时时间（毫秒）
    public int timeout = 3000;
    
    // 重试次数
    public int retries = 0;
    
    // 协议
    public String protocol = "dubbo";
    
    // 版本
    public String version = "";
    
    // 分组
    public String group = "";
    
    // 是否使用泛化调用
    public boolean useGeneric = false;
    
    // 是否显示详细命令
    public boolean showDetailedCommand = true;
    
    // 默认参数值模板
    public boolean useExampleValues = true;
    
    // 服务端口
    public String servicePort = "20880";
    
    // 服务地址
    public String serviceAddress = "";

    public static DubboConfig getInstance(@NotNull Project project) {
        return project.getService(DubboConfig.class);
    }

    @Override
    public @Nullable DubboConfig getState() {
        return this;
    }

    @Override
    public void loadState(@NotNull DubboConfig state) {
        XmlSerializerUtil.copyBean(state, this);
    }

    // Getters and Setters
    public String getRegistryAddress() {
        return registryAddress;
    }

    public void setRegistryAddress(String registryAddress) {
        this.registryAddress = registryAddress;
    }

    public String getApplicationName() {
        return applicationName;
    }

    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public int getRetries() {
        return retries;
    }

    public void setRetries(int retries) {
        this.retries = retries;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public boolean isUseGeneric() {
        return useGeneric;
    }

    public void setUseGeneric(boolean useGeneric) {
        this.useGeneric = useGeneric;
    }

    public boolean isShowDetailedCommand() {
        return showDetailedCommand;
    }

    public void setShowDetailedCommand(boolean showDetailedCommand) {
        this.showDetailedCommand = showDetailedCommand;
    }

    public boolean isUseExampleValues() {
        return useExampleValues;
    }

    public void setUseExampleValues(boolean useExampleValues) {
        this.useExampleValues = useExampleValues;
    }
    
    public String getServicePort() {
        return servicePort;
    }
    
    public void setServicePort(String servicePort) {
        this.servicePort = servicePort;
    }
    
    public String getServiceAddress() {
        return serviceAddress;
    }
    
    public void setServiceAddress(String serviceAddress) {
        this.serviceAddress = serviceAddress;
    }
}