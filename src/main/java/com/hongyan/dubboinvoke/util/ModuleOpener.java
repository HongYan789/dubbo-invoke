package com.hongyan.dubboinvoke.util;

import java.lang.reflect.Method;

/**
 * JDK模块访问工具类
 * 用于在IntelliJ IDEA插件环境中打开必要的JDK模块以支持反射访问
 * 解决Java 9+模块系统对反射访问的限制
 * 
 * @author hongyan
 * @since 1.0
 */
public final class ModuleOpener {
	private static volatile boolean attempted = false;
	private static final OperationLogger logger = 
		OperationLogger.getInstance();

	private ModuleOpener() {}

	/**
	 * 尝试打开JDK模块以支持反射访问
	 * 避免触发任何Dubbo类的初始化
	 */
	public static void openIfNeeded() {
		if (attempted) {
			return;
		}
		attempted = true;
		
		try {
			logger.log("开始尝试打开JDK模块以支持反射访问");
			
			// 只尝试反射修改模块，避免使用ByteBuddy Agent
			if (tryReflectionModuleModificationSafe()) {
				logger.log("反射修改模块成功");
			} else {
				logger.log("反射修改模块失败，但继续执行");
			}
			
			logger.log("JDK模块打开尝试完成");
			
		} catch (Exception e) {
			logger.log("JDK模块打开过程中发生异常: " + e.getMessage());
			// 不抛出异常，让程序继续运行
		}
	}
	

	
	/**
	 * 尝试使用反射修改模块访问权限
	 */
	private static boolean tryReflectionModuleModificationSafe() {
		try {
			logger.log("尝试使用反射修改模块...");
			
			// 获取当前模块
			Module currentModule = ModuleOpener.class.getModule();
			Module javaBaseModule = String.class.getModule();
			
			// 尝试打开java.base模块的关键包
			openModulePackage(currentModule, javaBaseModule, "java.lang");
			openModulePackage(currentModule, javaBaseModule, "java.lang.reflect");
			openModulePackage(currentModule, javaBaseModule, "java.util");
			openModulePackage(currentModule, javaBaseModule, "java.io");
			openModulePackage(currentModule, javaBaseModule, "java.net");
			openModulePackage(currentModule, javaBaseModule, "java.security");
			openModulePackage(currentModule, javaBaseModule, "java.math");
			openModulePackage(currentModule, javaBaseModule, "java.time");
			openModulePackage(currentModule, javaBaseModule, "java.nio");
			openModulePackage(currentModule, javaBaseModule, "java.lang.invoke");
			openModulePackage(currentModule, javaBaseModule, "java.text");
			
			logger.log("反射修改模块成功");
			return true;
			
		} catch (Exception e) {
			logger.log("反射修改模块失败: " + e.getMessage());
		}
		return false;
	}
	
	/**
	 * 打开指定模块的包访问权限
	 */
	private static void openModulePackage(Module fromModule, Module toModule, String packageName) {
		try {
			// 使用反射调用Module.implAddOpens方法
			Method implAddOpensMethod = Module.class.getDeclaredMethod("implAddOpens", String.class, Module.class);
			implAddOpensMethod.setAccessible(true);
			implAddOpensMethod.invoke(toModule, packageName, fromModule);
			
		} catch (Exception e) {
			// 忽略单个包打开失败
		}
	}
}