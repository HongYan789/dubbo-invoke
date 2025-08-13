package com.hongyan.dubboinvoke.util;

import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.PsiShortNamesCache;
import com.intellij.psi.util.PsiUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Java类字段解析器
 * 用于解析Java类的字段信息，生成动态的JSON参数
 */
public class JavaClassFieldParser {

    /**
     * 字段信息类
     */
    public static class FieldInfo {
        private final String name;
        private final String type;
        private final String exampleValue;

        public FieldInfo(String name, String type, String exampleValue) {
            this.name = name;
            this.type = type;
            this.exampleValue = exampleValue;
        }

        public String getName() { return name; }
        public String getType() { return type; }
        public String getExampleValue() { return exampleValue; }
    }

    /**
     * 解析类的字段信息
     * @param className 完整的类名
     * @param project IntelliJ项目实例
     * @return 字段信息列表
     */
    @NotNull
    public static List<FieldInfo> parseClassFields(@NotNull String className, @Nullable Project project) {
        List<FieldInfo> fields = new ArrayList<>();
        
        if (project == null) {
            System.out.println("[DEBUG] Project is null for class: " + className);
            return getDefaultFields(className);
        }
        
        try {
            System.out.println("[DEBUG] Trying to find class: " + className);
            // 查找类
            PsiClass psiClass = findClass(className, project);
            if (psiClass != null) {
                System.out.println("[DEBUG] Found class: " + psiClass.getQualifiedName());
                // 解析字段
                PsiField[] psiFields = psiClass.getAllFields();
                System.out.println("[DEBUG] Found " + psiFields.length + " fields in class");
                for (PsiField field : psiFields) {
                    // 跳过静态字段和常量
                    if (field.hasModifierProperty(PsiModifier.STATIC) || 
                        field.hasModifierProperty(PsiModifier.FINAL)) {
                        continue;
                    }
                    
                    String fieldName = field.getName();
                    String fieldType = getFieldType(field);
                    String exampleValue = generateExampleValueWithProject(fieldType, project, psiClass);
                    
                    System.out.println("[DEBUG] Adding field: " + fieldName + " (" + fieldType + ") = " + exampleValue);
                    fields.add(new FieldInfo(fieldName, fieldType, exampleValue));
                }
            } else {
                System.out.println("[DEBUG] Class not found: " + className);
            }
        } catch (Exception e) {
            System.out.println("[DEBUG] Exception while parsing class " + className + ": " + e.getMessage());
            // 如果解析失败，返回默认字段
            return getDefaultFields(className);
        }
        
        // 如果没有找到字段，返回默认字段
        if (fields.isEmpty()) {
            System.out.println("[DEBUG] No fields found, using default fields for: " + className);
            return getDefaultFields(className);
        }
        
        System.out.println("[DEBUG] Successfully parsed " + fields.size() + " fields for: " + className);
        return fields;
    }

    /**
     * 查找类
     */
    @Nullable
    private static PsiClass findClass(@NotNull String className, @NotNull Project project) {
        // 首先尝试通过JavaPsiFacade查找
        JavaPsiFacade facade = JavaPsiFacade.getInstance(project);
        PsiClass psiClass = facade.findClass(className, GlobalSearchScope.allScope(project));
        
        if (psiClass != null) {
            return psiClass;
        }
        
        // 如果找不到，尝试通过短名称查找
        String shortName = getShortClassName(className);
        PsiShortNamesCache cache = PsiShortNamesCache.getInstance(project);
        PsiClass[] classes = cache.getClassesByName(shortName, GlobalSearchScope.allScope(project));
        
        for (PsiClass clazz : classes) {
            if (className.equals(clazz.getQualifiedName())) {
                return clazz;
            }
        }
        
        return null;
    }

    /**
     * 获取字段类型
     */
    @NotNull
    private static String getFieldType(@NotNull PsiField field) {
        PsiType type = field.getType();
        return type.getCanonicalText();
    }

    /**
     * 生成示例值
     */
    @NotNull
    private static String generateExampleValue(@NotNull String fieldType, @Nullable PsiField field) {
        return generateExampleValueWithProject(fieldType, field != null ? field.getProject() : null);
    }

    /**
     * 生成示例值（兼容旧版本）
     */
    @NotNull
    private static String generateExampleValue(@NotNull String fieldType) {
        return generateExampleValueWithProject(fieldType, null);
    }
    
    /**
     * 根据字段类型生成示例值（带项目上下文）
     */
    @NotNull
    private static String generateExampleValueWithProject(@NotNull String fieldType, @Nullable Project project) {
        return generateExampleValueWithProject(fieldType, project, null);
    }
    
    /**
     * 根据字段类型生成示例值（带项目上下文和上下文类）
     */
    @NotNull
    private static String generateExampleValueWithProject(@NotNull String fieldType, @Nullable Project project, @Nullable PsiClass contextClass) {
        switch (fieldType) {
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
                return "1L";
            case "double":
            case "java.lang.Double":
            case "Double":
                return "1.0";
            case "float":
            case "java.lang.Float":
            case "Float":
                return "1.0f";
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
                if (fieldType.startsWith("java.util.List") || fieldType.contains("List")) {
                    return generateListValue(fieldType, project, contextClass);
                } else if (fieldType.startsWith("java.util.Map") || fieldType.contains("Map")) {
                    return "{}";
                } else if (fieldType.endsWith("[]")) {
                    return "[]";
                } else {
                    // 对于复杂对象类型，尝试递归解析其字段
                    return generateComplexObjectValue(fieldType, project, contextClass);
                }
        }
    }
    
    // 用于防止无限递归的类型集合
    private static final ThreadLocal<Set<String>> PARSING_TYPES = ThreadLocal.withInitial(HashSet::new);
    
    /**
     * 生成List类型的示例值
     */
    @NotNull
    private static String generateListValue(@NotNull String listType, @Nullable Project project, @Nullable PsiClass contextClass) {
        if (project == null) {
            return "[]";
        }
        
        // 提取泛型类型
        String genericType = extractGenericType(listType);
        if (genericType == null || genericType.isEmpty()) {
            return "[]";
        }
        
        // 处理基本类型
        if (isPrimitiveType(genericType)) {
            return generatePrimitiveListValue(genericType);
        }
        
        // 处理复杂对象类型
        String elementValue = generateComplexObjectValue(genericType, project, contextClass);
        if (elementValue.equals("\"\"")) {
            return "[]";
        }
        
        return "[" + elementValue + "]";
    }
    
    /**
     * 提取List泛型类型
     */
    @Nullable
    private static String extractGenericType(@NotNull String listType) {
        int startIndex = listType.indexOf('<');
        int endIndex = listType.lastIndexOf('>');
        
        if (startIndex != -1 && endIndex != -1 && startIndex < endIndex) {
            return listType.substring(startIndex + 1, endIndex).trim();
        }
        
        return null;
    }
    
    /**
     * 判断是否为基本类型
     */
    private static boolean isPrimitiveType(@NotNull String type) {
        return type.equals("String") || type.equals("java.lang.String") ||
               type.equals("Integer") || type.equals("java.lang.Integer") ||
               type.equals("Long") || type.equals("java.lang.Long") ||
               type.equals("Double") || type.equals("java.lang.Double") ||
               type.equals("Float") || type.equals("java.lang.Float") ||
               type.equals("Boolean") || type.equals("java.lang.Boolean") ||
               type.equals("int") || type.equals("long") || type.equals("double") ||
               type.equals("float") || type.equals("boolean");
    }
    
    /**
     * 生成基本类型List的示例值
     */
    @NotNull
    private static String generatePrimitiveListValue(@NotNull String primitiveType) {
        switch (primitiveType) {
            case "String":
            case "java.lang.String":
                return "[\"example\"]";
            case "Integer":
            case "java.lang.Integer":
            case "int":
                return "[1]";
            case "Long":
            case "java.lang.Long":
            case "long":
                return "[1L]";
            case "Double":
            case "java.lang.Double":
            case "double":
                return "[1.0]";
            case "Float":
            case "java.lang.Float":
            case "float":
                return "[1.0f]";
            case "Boolean":
            case "java.lang.Boolean":
            case "boolean":
                return "[true]";
            default:
                return "[]";
        }
    }
    
    /**
     * 生成复杂对象的示例值
     */
    @NotNull
    private static String generateComplexObjectValue(@NotNull String fieldType, @Nullable Project project) {
        return generateComplexObjectValue(fieldType, project, null);
    }
    
    /**
     * 生成复杂对象的示例值（带上下文类）
     */
    @NotNull
    private static String generateComplexObjectValue(@NotNull String fieldType, @Nullable Project project, @Nullable PsiClass contextClass) {
        if (project == null) {
            return "\"\"";
        }
        
        // 防止无限递归
        Set<String> parsingTypes = PARSING_TYPES.get();
        if (parsingTypes.contains(fieldType)) {
            System.out.println("[DEBUG] Circular reference detected for type: " + fieldType);
            return "\"\"";
        }
        
        try {
            parsingTypes.add(fieldType);
            
            // 使用TypeResolver解析类型全路径
            String resolvedType = TypeResolver.resolveFullTypeName(fieldType, contextClass, project);
            
            // 递归解析复杂对象的字段
            List<FieldInfo> nestedFields = parseClassFields(resolvedType, project);
            if (!nestedFields.isEmpty()) {
                StringBuilder json = new StringBuilder();
                json.append("{");
                json.append(fieldsToJsonString(nestedFields));
                json.append("}");
                return json.toString();
            }
        } catch (Exception e) {
            System.out.println("[DEBUG] Failed to parse nested object: " + fieldType + ", error: " + e.getMessage());
        } finally {
            parsingTypes.remove(fieldType);
            if (parsingTypes.isEmpty()) {
                PARSING_TYPES.remove();
            }
        }
        
        // 如果无法解析，返回空字符串
        return "\"\"";
    }

    /**
     * 获取短类名
     */
    @NotNull
    private static String getShortClassName(@NotNull String fullClassName) {
        int lastDotIndex = fullClassName.lastIndexOf('.');
        return lastDotIndex >= 0 ? fullClassName.substring(lastDotIndex + 1) : fullClassName;
    }

    /**
     * 获取默认字段（当无法解析类时使用）
     */
    @NotNull
    private static List<FieldInfo> getDefaultFields(@NotNull String className) {
        List<FieldInfo> defaultFields = new ArrayList<>();
        
        // 根据类名推测可能的字段
        if (className.contains("StoreCompanyBlackImport4ZyDTO")) {
            if (className.endsWith(".Row")) {
                // 内部类Row的字段
                defaultFields.add(new FieldInfo("companyId", "java.lang.String", "\"example\""));
                defaultFields.add(new FieldInfo("danwBh", "java.lang.String", "\"example\""));
                defaultFields.add(new FieldInfo("freezeCause", "java.lang.String", "\"example\""));
                defaultFields.add(new FieldInfo("errorMessage", "java.lang.String", "\"example\""));
                defaultFields.add(new FieldInfo("checkPass", "java.lang.Boolean", "true"));
            } else {
                // 主类的字段
                defaultFields.add(new FieldInfo("storeId", "java.lang.Long", "1L"));
                defaultFields.add(new FieldInfo("createUserName", "java.lang.String", "\"example\""));
                defaultFields.add(new FieldInfo("createUser", "java.lang.Long", "1L"));
                // rows字段使用List<Row>类型
                String rowJson = "{\"class\":\"com.jzt.zhcai.user.storecompanyblack.dto.StoreCompanyBlackImport4ZyDTO.Row\", \"companyId\":\"example\", \"danwBh\":\"example\", \"freezeCause\":\"example\",\"errorMessage\":\"example\", \"checkPass\": true}";
                defaultFields.add(new FieldInfo("rows", "java.util.List<com.jzt.zhcai.user.storecompanyblack.dto.StoreCompanyBlackImport4ZyDTO.Row>", "[" + rowJson + "]"));
            }
        } else if (className.contains("CompanyInfoDetailQry") || className.contains("CompanyInfo")) {
            defaultFields.add(new FieldInfo("id", "java.lang.Long", "123L"));
            defaultFields.add(new FieldInfo("name", "java.lang.String", "\"测试公司\""));
            defaultFields.add(new FieldInfo("code", "java.lang.String", "\"TEST001\""));
        } else if (className.contains("UserProfileRequest")) {
            defaultFields.add(new FieldInfo("id", "java.lang.Long", "1L"));
            defaultFields.add(new FieldInfo("name", "java.lang.String", "\"example\""));
            defaultFields.add(new FieldInfo("email", "java.lang.String", "\"user@example.com\""));
            defaultFields.add(new FieldInfo("address", "com.hongyan.dubboinvoke.dto.AddressInfo", "{\"province\":\"example\",\"city\":\"example\",\"district\":\"example\",\"street\":\"example\",\"zipCode\":\"example\"}"));
            defaultFields.add(new FieldInfo("contact", "com.hongyan.dubboinvoke.dto.ContactInfo", "{\"phone\":\"example\",\"mobile\":\"example\",\"qq\":\"example\",\"wechat\":\"example\"}"));
        } else if (className.contains("AddressInfo")) {
            defaultFields.add(new FieldInfo("province", "java.lang.String", "\"example\""));
            defaultFields.add(new FieldInfo("city", "java.lang.String", "\"example\""));
            defaultFields.add(new FieldInfo("district", "java.lang.String", "\"example\""));
            defaultFields.add(new FieldInfo("street", "java.lang.String", "\"example\""));
            defaultFields.add(new FieldInfo("zipCode", "java.lang.String", "\"example\""));
        } else if (className.contains("ContactInfo")) {
            defaultFields.add(new FieldInfo("phone", "java.lang.String", "\"example\""));
            defaultFields.add(new FieldInfo("mobile", "java.lang.String", "\"example\""));
            defaultFields.add(new FieldInfo("qq", "java.lang.String", "\"example\""));
            defaultFields.add(new FieldInfo("wechat", "java.lang.String", "\"example\""));
        } else if (className.contains("Request") || className.contains("Qry")) {
            // 通用请求对象的默认字段
            defaultFields.add(new FieldInfo("id", "java.lang.Long", "1L"));
            defaultFields.add(new FieldInfo("name", "java.lang.String", "\"example\""));
        } else {
            // 对于其他类型，返回一个通用的默认字段
            defaultFields.add(new FieldInfo("value", "java.lang.String", "\"\""));
        }
        
        return defaultFields;
    }

    /**
     * 将字段信息转换为JSON字符串
     */
    @NotNull
    public static String fieldsToJsonString(@NotNull List<FieldInfo> fields) {
        if (fields.isEmpty()) {
            return "";
        }
        
        StringBuilder json = new StringBuilder();
        for (int i = 0; i < fields.size(); i++) {
            FieldInfo field = fields.get(i);
            json.append("\"").append(field.getName()).append("\":").append(field.getExampleValue());
            if (i < fields.size() - 1) {
                json.append(",");
            }
        }
        
        return json.toString();
    }
}