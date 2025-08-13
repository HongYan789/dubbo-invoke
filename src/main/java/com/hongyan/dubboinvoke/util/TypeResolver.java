package com.hongyan.dubboinvoke.util;

import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.PsiShortNamesCache;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;

/**
 * 类型解析器
 * 用于解析参数类型的全路径
 */
public class TypeResolver {

    /**
     * 解析参数类型的全路径
     * @param shortTypeName 短类型名称（如：CompanyLngAndLatRequest）
     * @param contextClass 当前类的PsiClass，用于查找import语句
     * @param project IntelliJ项目实例
     * @return 完整的类型路径，如果找不到则返回原始类型名
     */
    @NotNull
    public static String resolveFullTypeName(@NotNull String shortTypeName, 
                                            @Nullable PsiClass contextClass, 
                                            @Nullable Project project) {
        // 如果已经是全路径，直接返回
        if (shortTypeName.contains(".")) {
            return shortTypeName;
        }
        
        // 基本类型和包装类型直接返回
        if (isPrimitiveOrWrapper(shortTypeName)) {
            return shortTypeName;
        }
        
        // 1. 优先查找当前类的内部类
        if (contextClass != null) {
            String innerClassType = findInnerClass(shortTypeName, contextClass);
            if (innerClassType != null) {
                return innerClassType;
            }
        }
        
        // 2. 尝试从当前类的import语句中查找
        if (contextClass != null) {
            String fullTypeFromImports = findTypeInImports(shortTypeName, contextClass);
            if (fullTypeFromImports != null) {
                return fullTypeFromImports;
            }
        }
        
        // 3. 尝试在项目中搜索
        if (project != null) {
            String fullTypeFromProject = findTypeInProject(shortTypeName, project);
            if (fullTypeFromProject != null) {
                return fullTypeFromProject;
            }
        }
        
        // 4. 如果都找不到，返回原始类型名
        return shortTypeName;
    }
    
    /**
     * 查找内部类
     */
    @Nullable
    private static String findInnerClass(@NotNull String shortTypeName, @NotNull PsiClass contextClass) {
        // 查找当前类的内部类
        PsiClass[] innerClasses = contextClass.getInnerClasses();
        for (PsiClass innerClass : innerClasses) {
            if (shortTypeName.equals(innerClass.getName())) {
                String qualifiedName = innerClass.getQualifiedName();
                if (qualifiedName != null) {
                    return qualifiedName;
                }
            }
        }
        
        // 递归查找外部类的内部类
        PsiClass containingClass = contextClass.getContainingClass();
        if (containingClass != null) {
            return findInnerClass(shortTypeName, containingClass);
        }
        
        return null;
    }
    
    /**
     * 从当前类的import语句中查找类型
     */
    @Nullable
    private static String findTypeInImports(@NotNull String shortTypeName, @NotNull PsiClass contextClass) {
        PsiFile containingFile = contextClass.getContainingFile();
        if (!(containingFile instanceof PsiJavaFile)) {
            return null;
        }
        
        PsiJavaFile javaFile = (PsiJavaFile) containingFile;
        PsiImportList importList = javaFile.getImportList();
        if (importList == null) {
            return null;
        }
        
        // 检查所有import语句
        for (PsiImportStatement importStatement : importList.getImportStatements()) {
            if (importStatement.isOnDemand()) {
                continue; // 跳过通配符import
            }
            
            String importedClassName = importStatement.getQualifiedName();
            if (importedClassName != null && importedClassName.endsWith("." + shortTypeName)) {
                return importedClassName;
            }
        }
        
        return null;
    }
    
    /**
     * 在项目中搜索类型
     */
    @Nullable
    private static String findTypeInProject(@NotNull String shortTypeName, @NotNull Project project) {
        try {
            PsiShortNamesCache cache = PsiShortNamesCache.getInstance(project);
            PsiClass[] classes = cache.getClassesByName(shortTypeName, GlobalSearchScope.allScope(project));
            
            if (classes.length > 0) {
                // 优先选择第一个匹配的类
                // 可以根据需要添加更智能的选择逻辑
                for (PsiClass clazz : classes) {
                    String qualifiedName = clazz.getQualifiedName();
                    if (qualifiedName != null) {
                        return qualifiedName;
                    }
                }
            }
        } catch (Exception e) {
            // 忽略异常，返回null
        }
        
        return null;
    }
    
    /**
     * 判断是否为基本类型或包装类型
     */
    private static boolean isPrimitiveOrWrapper(@NotNull String type) {
        List<String> primitiveTypes = Arrays.asList(
            "int", "long", "double", "float", "boolean", "byte", "short", "char",
            "Integer", "Long", "Double", "Float", "Boolean", "Byte", "Short", "Character",
            "java.lang.Integer", "java.lang.Long", "java.lang.Double", "java.lang.Float",
            "java.lang.Boolean", "java.lang.Byte", "java.lang.Short", "java.lang.Character",
            "String", "java.lang.String"
        );
        return primitiveTypes.contains(type);
    }
}