package com.hongyan.dubboinvoke.action;

import com.hongyan.dubboinvoke.generator.DubboCommandGenerator;
import com.hongyan.dubboinvoke.ui.DubboInvokeDialog;
import com.hongyan.dubboinvoke.util.JavaMethodParser;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;

/**
 * Dubbo Invoke Action - 生成Dubbo invoke命令的主要Action类
 */
public class DubboInvokeAction extends AnAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        PsiFile psiFile = e.getData(CommonDataKeys.PSI_FILE);

        if (project == null || editor == null || psiFile == null) {
            return;
        }

        // 获取当前光标位置的方法
        PsiMethod method = getCurrentMethod(editor, psiFile);
        if (method == null) {
            Messages.showWarningDialog(project, 
                "请将光标放在Java方法上", 
                "Dubbo Invoke Generator");
            return;
        }

        // 解析方法信息
        JavaMethodParser.MethodInfo methodInfo = JavaMethodParser.parseMethod(method, project);
        if (methodInfo == null) {
            Messages.showErrorDialog(project, "无法解析方法信息", "Dubbo Invoke Generator");
            return;
        }

        // 生成Dubbo命令
        String dubboCommand = DubboCommandGenerator.generateCommand(methodInfo, project);

        // 显示对话框
        DubboInvokeDialog dialog = new DubboInvokeDialog(project, dubboCommand, methodInfo);
        dialog.showAndGet();
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        PsiFile psiFile = e.getData(CommonDataKeys.PSI_FILE);

        // 只在Java文件中启用
        boolean enabled = project != null && 
                         editor != null && 
                         psiFile instanceof PsiJavaFile;

        e.getPresentation().setEnabled(enabled);
        e.getPresentation().setVisible(enabled);
    }

    /**
     * 获取当前光标位置的方法
     */
    private PsiMethod getCurrentMethod(Editor editor, PsiFile psiFile) {
        int offset = editor.getCaretModel().getOffset();
        PsiElement element = psiFile.findElementAt(offset);
        
        if (element == null) {
            return null;
        }

        // 向上查找方法元素
        return PsiTreeUtil.getParentOfType(element, PsiMethod.class);
    }
}