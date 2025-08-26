package com.hongyan.dubboinvoke;

import com.hongyan.dubboinvoke.client.DubboClientManager;

public class DirectInvokeSmokeTest {
    public static void main(String[] args) {
        String serviceInterface = "com.jzt.zhcai.user.companyinfo.CompanyInfoDubboApi";
        String serviceUrl = "dubbo://10.7.8.50:16002"; // 直连地址
        String methodName = "queryCompanyInfoByCompanyId";

        // 优先按原始需求使用 Long 入参
        String[] paramTypes = new String[]{"java.lang.Long"};
        Object[] parameters = new Object[]{1L};

        DubboClientManager clientManager = DubboClientManager.getInstance();
        try {
            String result = clientManager.invokeServiceAsJson(
                serviceInterface,
                serviceUrl,
                methodName,
                paramTypes,
                parameters
            );
            System.out.println("Invoke result (java.lang.Long):\n" + result);
        } catch (Throwable first) {
            System.err.println("First attempt failed with java.lang.Long, trying primitive long. Error: " + first);
            try {
                String[] altTypes = new String[]{"long"};
                Object[] altParams = new Object[]{1L};
                String result = clientManager.invokeServiceAsJson(
                    serviceInterface,
                    serviceUrl,
                    methodName,
                    altTypes,
                    altParams
                );
                System.out.println("Invoke result (long):\n" + result);
            } catch (Throwable t) {
                t.printStackTrace();
                System.err.println("Invoke failed: " + t);
            }
        }
    }
} 