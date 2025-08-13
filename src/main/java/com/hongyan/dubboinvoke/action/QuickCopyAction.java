package com.hongyan.dubboinvoke.action;

import com.hongyan.dubboinvoke.generator.DubboCommandGenerator;
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

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;

/**
 * 快速复制Action - 直接生成命令并复制到剪切板
 */
public class QuickCopyAction extends AnAction {

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
        JavaMethodParser.MethodInfo methodInfo = JavaMethodParser.parseMethod(method);
        if (methodInfo == null) {
            Messages.showErrorDialog(project, 
                "无法解析方法信息", 
                "Dubbo Invoke Generator");
            return;
        }

        // 生成Dubbo命令
        String dubboCommand = DubboCommandGenerator.generateCommand(methodInfo, project);

        // 直接复制到剪切板
        copyToClipboard(dubboCommand);

        // 显示成功消息
        String methodName = methodInfo.getMethodName();
        Messages.showInfoMessage(project,
            String.format("Dubbo command for method '%s' copied to clipboard!", methodName),
            "Dubbo Invoke Generator");
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

    /**
     * 复制文本到剪切板
     */
    private void copyToClipboard(String text) {
        if (text != null && !text.trim().isEmpty()) {
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            StringSelection selection = new StringSelection(text);
            clipboard.setContents(selection, null);
        }
    }
}