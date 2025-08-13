package com.hongyan.dubboinvoke.action;

import com.hongyan.dubboinvoke.ui.DubboConfigDialog;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

/**
 * 配置Action - 打开Dubbo配置对话框
 */
public class ConfigAction extends AnAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) {
            return;
        }

        DubboConfigDialog dialog = new DubboConfigDialog(project);
        dialog.showAndGet();
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        e.getPresentation().setEnabled(project != null);
    }
}