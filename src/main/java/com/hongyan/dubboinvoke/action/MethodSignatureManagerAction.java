package com.hongyan.dubboinvoke.action;

import com.hongyan.dubboinvoke.ui.MethodSignatureManagerDialog;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

/**
 * 方法签名管理Action - 打开方法签名管理对话框
 */
public class MethodSignatureManagerAction extends AnAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) {
            return;
        }

        MethodSignatureManagerDialog dialog = new MethodSignatureManagerDialog(project);
        dialog.showAndGet();
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        e.getPresentation().setEnabled(project != null);
        e.getPresentation().setText("方法签名管理");
        e.getPresentation().setDescription("管理缓存的Dubbo方法签名配置");
    }
}