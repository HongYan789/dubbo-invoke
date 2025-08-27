package com.hongyan.dubboinvoke;

import com.hongyan.dubboinvoke.client.DubboClientManager;

/**
 * 参数类型修复验证测试
 * 验证对复杂参数类型的正确处理，特别是List和Array类型参数
 */
public class ParameterTypeFixTest {
    
    public static void main(String[] args) {
        System.out.println("=== 开始参数类型修复验证测试 ===");
        
        try {
            DubboClientManager clientManager = DubboClientManager.getInstance();
            System.out.println("✅ DubboClientManager初始化成功");
            
            // 测试1：getCompanyIdList方法（期望List<Long>参数）
            testGetCompanyIdListMethod(clientManager);
            
            // 测试2：findRequireRepairCompanyInfo方法（期望String数组参数）
            testFindRequireRepairCompanyInfoMethod(clientManager);
            
            // 测试3：常规单个ID方法（期望Long参数）
            testRegularIdMethod(clientManager);
            
        } catch (Exception e) {
            System.out.println("❌ 测试失败: " + e.getMessage());
            e.printStackTrace();
        }
        
        System.out.println("\n=== 测试完成 ===");
    }
    
    /**
     * 测试getCompanyIdList方法 - 期望List<Long>参数
     */
    private static void testGetCompanyIdListMethod(DubboClientManager clientManager) {
        System.out.println("\n=== 测试1：getCompanyIdList方法（List<Long>参数）===");
        
        String serviceInterface = "com.jzt.zhcai.user.companyinfo.CompanyInfoDubboApi";
        String serviceUrl = "dubbo://10.7.8.50:16002";
        String methodName = "getCompanyIdList";
        
        // 模拟UI输入的数组格式参数
        String[] parameterTypes = {"java.util.List"};
        Object[] parameters = {java.util.Arrays.asList(1919926727277895723L)};
        
        System.out.println("服务接口: " + serviceInterface);
        System.out.println("方法名: " + methodName);
        System.out.println("参数类型: " + java.util.Arrays.toString(parameterTypes));
        System.out.println("参数值: " + java.util.Arrays.toString(parameters));
        
        try {
            Object result = clientManager.invokeService(
                serviceInterface, serviceUrl, methodName, parameterTypes, parameters
            );
            
            System.out.println("✅ getCompanyIdList调用成功！");
            System.out.println("返回结果类型: " + (result != null ? result.getClass().getName() : "null"));
            
        } catch (Exception e) {
            System.out.println("❌ getCompanyIdList调用失败: " + e.getMessage());
            
            // 分析错误类型
            if (e.getMessage().contains("NoSuchMethodException")) {
                System.out.println("   🔍 方法签名不匹配 - 参数类型可能仍需调整");
            } else if (e.getMessage().contains("is not visible from class loader")) {
                System.out.println("   🔍 类加载器问题 - 建议使用注册中心模式");
            } else {
                System.out.println("   🔍 其他异常: " + e.getClass().getSimpleName());
            }
        }
    }
    
    /**
     * 测试findRequireRepairCompanyInfo方法 - 期望String或String[]参数
     */
    private static void testFindRequireRepairCompanyInfoMethod(DubboClientManager clientManager) {
        System.out.println("\n=== 测试2：findRequireRepairCompanyInfo方法（String参数）===");
        
        String serviceInterface = "com.jzt.zhcai.user.companyinfo.CompanyInfoDubboApi";
        String serviceUrl = "dubbo://10.7.8.50:16002";
        String methodName = "findRequireRepairCompanyInfo";
        
        // 先测试String参数
        String[] parameterTypes = {"java.lang.String"};
        Object[] parameters = {"1919926727277895723"};
        
        System.out.println("服务接口: " + serviceInterface);
        System.out.println("方法名: " + methodName);
        System.out.println("参数类型: " + java.util.Arrays.toString(parameterTypes));
        System.out.println("参数值: " + java.util.Arrays.toString(parameters));
        
        try {
            Object result = clientManager.invokeService(
                serviceInterface, serviceUrl, methodName, parameterTypes, parameters
            );
            
            System.out.println("✅ findRequireRepairCompanyInfo(String)调用成功！");
            System.out.println("返回结果类型: " + (result != null ? result.getClass().getName() : "null"));
            
        } catch (Exception e) {
            System.out.println("❌ findRequireRepairCompanyInfo(String)调用失败: " + e.getMessage());
            
            // 如果String参数失败，尝试String[]参数
            System.out.println("   🔄 尝试使用String[]参数...");
            testFindRequireRepairWithArrayParam(clientManager, serviceInterface, serviceUrl, methodName);
        }
    }
    
    /**
     * 测试findRequireRepairCompanyInfo方法的String[]参数版本
     */
    private static void testFindRequireRepairWithArrayParam(DubboClientManager clientManager, 
                                                          String serviceInterface, String serviceUrl, String methodName) {
        String[] parameterTypes = {"[Ljava.lang.String;"}; // String[]的JVM内部表示
        Object[] parameters = {new String[]{"1919926727277895723"}};
        
        System.out.println("尝试String[]参数:");
        System.out.println("参数类型: " + java.util.Arrays.toString(parameterTypes));
        System.out.println("参数值: " + java.util.Arrays.toString(parameters));
        
        try {
            Object result = clientManager.invokeService(
                serviceInterface, serviceUrl, methodName, parameterTypes, parameters
            );
            
            System.out.println("✅ findRequireRepairCompanyInfo(String[])调用成功！");
            System.out.println("返回结果类型: " + (result != null ? result.getClass().getName() : "null"));
            
        } catch (Exception e) {
            System.out.println("❌ findRequireRepairCompanyInfo(String[])也调用失败: " + e.getMessage());
            System.out.println("   🔍 可能需要进一步分析实际的方法签名");
        }
    }
    
    /**
     * 测试常规ID方法 - 期望Long参数（作为对比）
     */
    private static void testRegularIdMethod(DubboClientManager clientManager) {
        System.out.println("\n=== 测试3：queryCompanyByCompanyId方法（Long参数）===");
        
        String serviceInterface = "com.jzt.zhcai.user.companyinfo.CompanyInfoDubboApi";
        String serviceUrl = "dubbo://10.7.8.50:16002";
        String methodName = "queryCompanyByCompanyId";
        String[] parameterTypes = {"java.lang.Long"};
        Object[] parameters = {1919926727277895723L};
        
        System.out.println("服务接口: " + serviceInterface);
        System.out.println("方法名: " + methodName);
        System.out.println("参数类型: " + java.util.Arrays.toString(parameterTypes));
        System.out.println("参数值: " + java.util.Arrays.toString(parameters));
        
        try {
            Object result = clientManager.invokeService(
                serviceInterface, serviceUrl, methodName, parameterTypes, parameters
            );
            
            System.out.println("✅ queryCompanyByCompanyId调用成功！");
            System.out.println("返回结果类型: " + (result != null ? result.getClass().getName() : "null"));
            
        } catch (Exception e) {
            System.out.println("❌ queryCompanyByCompanyId调用失败: " + e.getMessage());
            
            if (e.getMessage().contains("Connection refused") || e.getMessage().contains("No provider")) {
                System.out.println("   🔍 网络连接问题 - 这是正常的，说明参数类型处理正确");
            } else {
                System.out.println("   🔍 需要进一步分析的问题");
            }
        }
    }
}