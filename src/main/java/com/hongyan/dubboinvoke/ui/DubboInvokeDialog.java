package com.hongyan.dubboinvoke.ui;

import com.hongyan.dubboinvoke.generator.DubboCommandGenerator;
import com.hongyan.dubboinvoke.util.JavaMethodParser;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextArea;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.util.List;

/**
 * Dubbo Invoke对话框
 */
public class DubboInvokeDialog extends DialogWrapper {
    private final String dubboCommand;
    private final JavaMethodParser.MethodInfo methodInfo;
    private final Project project;
    private JBTextArea commandTextArea;
    private JBTextArea methodInfoArea;

    public DubboInvokeDialog(@NotNull Project project, 
                           @NotNull String dubboCommand, 
                           @NotNull JavaMethodParser.MethodInfo methodInfo) {
        super(project);
        this.project = project;
        this.dubboCommand = dubboCommand;
        this.methodInfo = methodInfo;
        
        setTitle("Dubbo Invoke Command Generator");
        setResizable(true);
        init();
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setPreferredSize(new Dimension(600, 400));

        // 创建方法信息面板
        JPanel methodPanel = createMethodInfoPanel();
        mainPanel.add(methodPanel, BorderLayout.NORTH);

        // 创建命令显示面板
        JPanel commandPanel = createCommandPanel();
        mainPanel.add(commandPanel, BorderLayout.CENTER);

        // 创建按钮面板
        JPanel buttonPanel = createButtonPanel();
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        return mainPanel;
    }

    private JPanel createMethodInfoPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Method Information"));

        methodInfoArea = new JBTextArea();
        methodInfoArea.setEditable(false);
        methodInfoArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        methodInfoArea.setBackground(getContentPane().getBackground());
        
        // 生成详细的方法信息
        String methodInfoText = generateDetailedMethodInfo(methodInfo);
        methodInfoArea.setText(methodInfoText);
        methodInfoArea.setRows(6); // 增加行数以显示更多信息

        JBScrollPane scrollPane = new JBScrollPane(methodInfoArea);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }
    
    /**
     * 生成详细的方法信息
     */
    private String generateDetailedMethodInfo(JavaMethodParser.MethodInfo methodInfo) {
        StringBuilder info = new StringBuilder();
        
        // 类名
        info.append("类名 (Class): ").append(methodInfo.getClassName()).append("\n");
        
        // 方法名
        info.append("方法名 (Method): ").append(methodInfo.getMethodName()).append("\n");
        
        // 返回类型
        info.append("返回类型 (Return Type): ").append(methodInfo.getReturnType()).append("\n");
        
        // 方法全路径 (使用完整类型名称的Signature)
        String signature = generateFullMethodSignature(methodInfo);
        info.append("方法全路径 (Full Path): ").append(signature).append("\n");
        
        // 参数信息
        info.append("参数 (Parameters): ");
        if (methodInfo.getParameters().isEmpty()) {
            info.append("无参数");
        } else {
            info.append("\n");
            for (int i = 0; i < methodInfo.getParameters().size(); i++) {
                JavaMethodParser.ParameterInfo param = methodInfo.getParameters().get(i);
                info.append("  ").append(i + 1).append(". ")
                    .append(param.getType()).append(" ").append(param.getName()).append("\n");
            }
            // 移除最后一个换行符
            info.setLength(info.length() - 1);
        }
        
        return info.toString();
    }

    /**
     * 生成包含完整类型名称的方法签名
     */
    private String generateFullMethodSignature(JavaMethodParser.MethodInfo methodInfo) {
        StringBuilder signature = new StringBuilder();
        
        // 返回类型（完整类型名称）
        signature.append(methodInfo.getReturnType()).append(" ");
        
        // 方法名
        signature.append(methodInfo.getMethodName()).append("(");
        
        // 参数（完整类型名称）
        List<JavaMethodParser.ParameterInfo> parameters = methodInfo.getParameters();
        if (!parameters.isEmpty()) {
            String paramString = parameters.stream()
                    .map(param -> param.getType() + " " + param.getName())
                    .collect(java.util.stream.Collectors.joining(", "));
            signature.append(paramString);
        }
        
        signature.append(")");
        
        return signature.toString();
    }

    private JPanel createCommandPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Generated Dubbo Command"));

        commandTextArea = new JBTextArea();
        commandTextArea.setEditable(true);
        commandTextArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        commandTextArea.setText(dubboCommand);
        commandTextArea.setRows(8);
        commandTextArea.setLineWrap(true);
        commandTextArea.setWrapStyleWord(true);
        commandTextArea.selectAll();

        JBScrollPane scrollPane = new JBScrollPane(commandTextArea);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        JButton copyButton = new JButton("Copy Command");
        copyButton.addActionListener(this::copyToClipboard);

        JButton refreshButton = new JButton("Refresh");
        refreshButton.addActionListener(this::refreshCommand);

        panel.add(refreshButton);
        panel.add(copyButton);

        return panel;
    }

    private void copyToClipboard(ActionEvent e) {
        String command = commandTextArea.getText();
        if (command != null && !command.trim().isEmpty()) {
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            StringSelection selection = new StringSelection(command);
            clipboard.setContents(selection, null);
            
            Messages.showInfoMessage(
                "Command copied to clipboard!",
                "Dubbo Invoke Generator"
            );
        }
    }

    private void refreshCommand(ActionEvent e) {
        String newCommand = DubboCommandGenerator.generateCommand(methodInfo, project);
        commandTextArea.setText(newCommand);
        commandTextArea.selectAll();
    }

    @Override
    protected @NotNull Action[] createActions() {
        return new Action[]{getOKAction(), getCancelAction()};
    }

    @Override
    protected void doOKAction() {
        copyToClipboard(null);
        super.doOKAction();
    }
}