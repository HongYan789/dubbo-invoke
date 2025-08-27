package com.hongyan.dubboinvoke.generator;

import com.hongyan.dubboinvoke.config.DubboConfig;
import com.hongyan.dubboinvoke.util.JavaMethodParser;
import com.hongyan.dubboinvoke.util.JavaClassFieldParser;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Dubbo命令生成器
 */
public class DubboCommandGenerator {

    /**
     * 生成基本的Dubbo invoke命令
     */
    @NotNull
    public static String generateCommand(@NotNull JavaMethodParser.MethodInfo methodInfo) {
        return generateCommand(methodInfo, null);
    }

    /**
     * 生成带配置的Dubbo invoke命令
     */
    @NotNull
    public static String generateCommand(@NotNull JavaMethodParser.MethodInfo methodInfo, Project project) {
        DubboConfig config = project != null ? DubboConfig.getInstance(project) : null;
        
        // 始终生成基本命令，不再生成带注释的命令
        return generateBasicCommand(methodInfo, config, project);
    }

    /**
     * 生成基本命令
     */
    private static String generateBasicCommand(@NotNull JavaMethodParser.MethodInfo methodInfo, DubboConfig config) {
        return generateBasicCommand(methodInfo, config, null);
    }
    
    private static String generateBasicCommand(@NotNull JavaMethodParser.MethodInfo methodInfo, DubboConfig config, @Nullable Project project) {
        StringBuilder command = new StringBuilder();
        
        if (config != null && config.isUseGeneric()) {
            // 泛化调用命令
            command.append("invoke ");
            command.append(methodInfo.getClassName());
            command.append(".$invoke(\"");
            command.append(methodInfo.getMethodName());
            command.append("\", new String[]{");
            command.append(generateParameterTypes(methodInfo));
            command.append("}, new Object[]{");
            command.append(generateParameterValues(methodInfo, config));
            command.append("})");
        } else {
            // 直接调用命令 - 生成JSON格式参数
            command.append("invoke ");
            command.append(methodInfo.getClassName());
            command.append(".");
            command.append(methodInfo.getMethodName());
            command.append("(");
            command.append(generateJsonParameterValues(methodInfo, config, project));
            command.append(")");
        }
        
        return command.toString();
    }

    /**
     * 生成带注释的Dubbo命令
     */
    @NotNull
    public static String generateCommandWithComments(@NotNull JavaMethodParser.MethodInfo methodInfo) {
        return generateCommandWithComments(methodInfo, null);
    }

    /**
     * 生成带注释和配置的Dubbo命令（已废弃，保留兼容性）
     */
    @NotNull
    @Deprecated
    public static String generateCommandWithComments(@NotNull JavaMethodParser.MethodInfo methodInfo, DubboConfig config) {
        // 不再生成注释，直接返回基本命令
        return generateBasicCommand(methodInfo, config);
    }

    /**
     * 生成参数类型字符串
     */
    private static String generateParameterTypes(@NotNull JavaMethodParser.MethodInfo methodInfo) {
        List<JavaMethodParser.ParameterInfo> parameters = methodInfo.getParameters();
        return parameters.stream()
                .map(param -> "\"" + param.getType() + "\"")
                .collect(Collectors.joining(", "));
    }

    /**
     * 生成参数值字符串
     */
    private static String generateParameterValues(@NotNull JavaMethodParser.MethodInfo methodInfo, DubboConfig config) {
        List<JavaMethodParser.ParameterInfo> parameters = methodInfo.getParameters();
        
        return parameters.stream()
                .map(param -> {
                    if (config != null && config.isUseExampleValues()) {
                        return param.getExampleValue();
                    } else {
                        return "null";
                    }
                })
                .collect(Collectors.joining(", "));
    }

    /**
     * 生成JSON格式的参数值字符串
     */
    private static String generateJsonParameterValues(@NotNull JavaMethodParser.MethodInfo methodInfo, DubboConfig config) {
        return generateJsonParameterValues(methodInfo, config, null);
    }
    
    /**
     * 生成JSON格式的参数值字符串（带Project参数）
     */
    private static String generateJsonParameterValues(@NotNull JavaMethodParser.MethodInfo methodInfo, DubboConfig config, @Nullable Project project) {
        List<JavaMethodParser.ParameterInfo> parameters = methodInfo.getParameters();
        
        if (parameters.isEmpty()) {
            return "";
        }
        
        return parameters.stream()
                .map(param -> {
                    String type = param.getType();
                    String paramName = param.getName();
                    
                    // 处理List<T>类型
                    if (type.startsWith("java.util.List<") || type.startsWith("List<")) {
                        return generateListParameter(type, paramName, methodInfo, project);
                    }
                    
                    // 处理基本类型
                    if (isPrimitiveOrWrapper(type)) {
                        return param.getExampleValue();
                    }
                    
                    // 处理复杂对象类型
                    StringBuilder json = new StringBuilder();
                    json.append("{");
                    json.append("\"class\":\"").append(param.getType()).append("\"");
                    
                    if (config != null && config.isUseExampleValues()) {
                        // 添加示例字段
                        String exampleFields = generateExampleFields(param.getType(), project);
                        if (!exampleFields.isEmpty()) {
                            json.append(",").append(exampleFields);
                        }
                    } else {
                        // 即使不使用示例值配置，也要尝试解析实际字段
                        String exampleFields = generateExampleFields(param.getType(), project);
                        if (!exampleFields.isEmpty() && !exampleFields.equals("\"\": \"\"")) {
                            json.append(",").append(exampleFields);
                        }
                    }
                    
                    json.append("}");
                    return json.toString();
                })
                .collect(Collectors.joining(","));
    }

    /**
     * 处理List<T>类型参数
     */
    private static String generateListParameter(String type, String paramName, @NotNull JavaMethodParser.MethodInfo methodInfo, @Nullable Project project) {
        // 提取泛型类型
        String genericType = extractGenericType(type);
        
        if (genericType == null || genericType.isEmpty()) {
            return "[]"; // 空数组
        }
        
        // 处理基本类型和包装类型
        if (isPrimitiveOrWrapper(genericType)) {
            return generatePrimitiveListValue(genericType);
        }
        
        // 处理String类型
        if ("String".equals(genericType) || "java.lang.String".equals(genericType)) {
            return "[\"example\"]"; // String类型使用示例值
        }
        
        // 处理复杂对象类型
        StringBuilder json = new StringBuilder();
        json.append("[{");
        
        // 尝试解析泛型类型的全路径
        String resolvedType = resolveFullClassName(genericType, methodInfo.getContainingClass(), project);
        json.append("\"class\":\"").append(resolvedType.isEmpty() ? "" : resolvedType).append("\"");
        
        // 尝试解析字段
        String exampleFields = generateExampleFields(resolvedType, project);
        if (!exampleFields.isEmpty() && !exampleFields.equals("\"\": \"\"")) {
            json.append(", ").append(exampleFields);
        }
        
        json.append("}]");
        return json.toString();
    }
    
    /**
     * 生成基本类型List的示例值
     */
    private static String generatePrimitiveListValue(String primitiveType) {
        switch (primitiveType) {
            case "Long":
            case "java.lang.Long":
            case "long":
                return "[1]";
            case "Integer":
            case "java.lang.Integer":
            case "int":
                return "[1]";
            case "Double":
            case "java.lang.Double":
            case "double":
                return "[1.0]";
            case "Float":
            case "java.lang.Float":
            case "float":
                return "[1.0]";
            case "Boolean":
            case "java.lang.Boolean":
            case "boolean":
                return "[true]";
            case "Short":
            case "java.lang.Short":
            case "short":
                return "[1]";
            case "Byte":
            case "java.lang.Byte":
            case "byte":
                return "[1]";
            case "Character":
            case "java.lang.Character":
            case "char":
                return "['a']";
            default:
                return "[]";
        }
    }
    
    /**
     * 提取List<T>中的泛型类型T
     */
    private static String extractGenericType(String listType) {
        int start = listType.indexOf('<');
        int end = listType.lastIndexOf('>');
        
        if (start != -1 && end != -1 && start < end) {
            return listType.substring(start + 1, end).trim();
        }
        
        return null;
    }
    
    /**
     * 解析类名的全路径
     */
    private static String resolveFullClassName(String className, @Nullable com.intellij.psi.PsiClass contextClass, @Nullable Project project) {
        if (project == null) {
            return className;
        }
        
        // 使用TypeResolver解析全路径
        try {
            String fullPath = com.hongyan.dubboinvoke.util.TypeResolver.resolveFullTypeName(className, contextClass, project);
            return fullPath != null ? fullPath : className;
        } catch (Exception e) {
            return className;
        }
    }
    
    /**
     * 生成示例字段（兼容方法）
     */
    private static String generateExampleFields(String type) {
        return generateExampleFields(type, null);
    }
    
    /**
     * 生成示例字段（使用动态字段解析）
     */
    private static String generateExampleFields(String type, @Nullable Project project) {
        // 对于基本类型，不生成额外字段
        if (isPrimitiveOrWrapper(type)) {
            return "";
        }
        
        try {
            // 对于复杂类型，使用JavaClassFieldParser解析
            if (project != null) {
                List<JavaClassFieldParser.FieldInfo> fields = JavaClassFieldParser.parseClassFields(type, project);
                if (!fields.isEmpty()) {
                    return JavaClassFieldParser.fieldsToJsonString(fields);
                }
            }
        } catch (Exception e) {
            // 忽略异常，使用默认值
        }
        
        // 如果无法解析字段，返回空字符串
        return "\"\":\"\"";
    }

    /**
     * 判断是否为基本类型或包装类型
     */
    private static boolean isPrimitiveOrWrapper(String type) {
        return type.equals("int") || type.equals("java.lang.Integer") || type.equals("Integer") ||
               type.equals("long") || type.equals("java.lang.Long") || type.equals("Long") ||
               type.equals("double") || type.equals("java.lang.Double") || type.equals("Double") ||
               type.equals("float") || type.equals("java.lang.Float") || type.equals("Float") ||
               type.equals("boolean") || type.equals("java.lang.Boolean") || type.equals("Boolean") ||
               type.equals("byte") || type.equals("java.lang.Byte") || type.equals("Byte") ||
               type.equals("short") || type.equals("java.lang.Short") || type.equals("Short") ||
               type.equals("char") || type.equals("java.lang.Character") || type.equals("Character") ||
               type.equals("java.lang.String") || type.equals("String");
    }

    /**
     * 生成简化的方法签名
     */
    @NotNull
    public static String generateMethodSignature(@NotNull JavaMethodParser.MethodInfo methodInfo) {
        StringBuilder signature = new StringBuilder();
        
        signature.append(methodInfo.getReturnType()).append(" ");
        signature.append(methodInfo.getMethodName()).append("(");
        
        List<JavaMethodParser.ParameterInfo> parameters = methodInfo.getParameters();
        if (!parameters.isEmpty()) {
            String paramString = parameters.stream()
                    .map(param -> param.getType() + " " + param.getName())
                    .collect(Collectors.joining(", "));
            signature.append(paramString);
        }
        
        signature.append(")");
        
        return signature.toString();
    }
}