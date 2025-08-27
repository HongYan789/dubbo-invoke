package com.hongyan.dubboinvoke.util;

import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Java方法解析器
 */
public class JavaMethodParser {

    /**
     * 方法信息类
     */
    public static class MethodInfo {
        private final String className;
        private final String methodName;
        private final List<ParameterInfo> parameters;
        private final String returnType;
        private final PsiClass containingClass;

        public MethodInfo(String className, String methodName, List<ParameterInfo> parameters, String returnType, PsiClass containingClass) {
            this.className = className;
            this.methodName = methodName;
            this.parameters = parameters;
            this.returnType = returnType;
            this.containingClass = containingClass;
        }

        public String getClassName() { return className; }
        public String getMethodName() { return methodName; }
        public List<ParameterInfo> getParameters() { return parameters; }
        public String getReturnType() { return returnType; }
        public PsiClass getContainingClass() { return containingClass; }
    }

    /**
     * 参数信息类
     */
    public static class ParameterInfo {
        private final String name;
        private final String type;
        private final String exampleValue;

        public ParameterInfo(String name, String type, String exampleValue) {
            this.name = name;
            this.type = type;
            this.exampleValue = exampleValue;
        }

        public String getName() { return name; }
        public String getType() { return type; }
        public String getExampleValue() { return exampleValue; }
    }

    /**
     * 解析方法信息
     */
    @Nullable
    public static MethodInfo parseMethod(@NotNull PsiMethod method) {
        try {
            // 获取类名
            PsiClass containingClass = method.getContainingClass();
            if (containingClass == null) {
                return null;
            }
            String className = containingClass.getQualifiedName();
            if (className == null) {
                return null;
            }

            // 获取方法名
            String methodName = method.getName();

            // 解析参数
            List<ParameterInfo> parameters = new ArrayList<>();
            PsiParameterList parameterList = method.getParameterList();
            for (PsiParameter parameter : parameterList.getParameters()) {
                String paramName = parameter.getName();
                String paramType = parameter.getType().getCanonicalText();
                
                // 解析参数类型的全路径
                String fullParamType = TypeResolver.resolveFullTypeName(paramType, containingClass, null);
                
                String exampleValue = generateExampleValue(fullParamType);
                parameters.add(new ParameterInfo(paramName, fullParamType, exampleValue));
            }

            // 获取返回类型
            PsiType returnType = method.getReturnType();
            String returnTypeStr = returnType != null ? returnType.getCanonicalText() : "void";

            return new MethodInfo(className, methodName, parameters, returnTypeStr, containingClass);
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * 解析方法信息（带Project参数）
     */
    @Nullable
    public static MethodInfo parseMethod(@NotNull PsiMethod method, @Nullable Project project) {
        try {
            // 获取类名
            PsiClass containingClass = method.getContainingClass();
            if (containingClass == null) {
                return null;
            }
            String className = containingClass.getQualifiedName();
            if (className == null) {
                return null;
            }

            // 获取方法名
            String methodName = method.getName();

            // 解析参数
            List<ParameterInfo> parameters = new ArrayList<>();
            PsiParameterList parameterList = method.getParameterList();
            for (PsiParameter parameter : parameterList.getParameters()) {
                String paramName = parameter.getName();
                String paramType = parameter.getType().getCanonicalText();
                
                // 解析参数类型的全路径
                String fullParamType = TypeResolver.resolveFullTypeName(paramType, containingClass, project);
                
                String exampleValue = generateExampleValue(fullParamType);
                parameters.add(new ParameterInfo(paramName, fullParamType, exampleValue));
            }

            // 获取返回类型
            PsiType returnType = method.getReturnType();
            String returnTypeStr = returnType != null ? returnType.getCanonicalText() : "void";

            return new MethodInfo(className, methodName, parameters, returnTypeStr, containingClass);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 根据类型生成示例值
     */
    private static String generateExampleValue(String type) {
        switch (type) {
            case "java.lang.String":
            case "String":
                return "\"example\"";
            case "int":
            case "java.lang.Integer":
            case "Integer":
                return "1";
            case "long":
            case "java.lang.Long":
            case "Long":
                return "1";
            case "double":
            case "java.lang.Double":
            case "Double":
                return "1.0";
            case "float":
            case "java.lang.Float":
            case "Float":
                return "1.0";
            case "boolean":
            case "java.lang.Boolean":
            case "Boolean":
                return "true";
            case "byte":
            case "java.lang.Byte":
            case "Byte":
                return "1";
            case "short":
            case "java.lang.Short":
            case "Short":
                return "1";
            case "char":
            case "java.lang.Character":
            case "Character":
                return "'a'";
            default:
                if (type.startsWith("java.util.List") || type.contains("List")) {
                    return "[]";
                } else if (type.startsWith("java.util.Map") || type.contains("Map")) {
                    return "{}";
                } else if (type.endsWith("[]")) {
                    return "[]";
                } else {
                    return "{}";
                }
        }
    }
}