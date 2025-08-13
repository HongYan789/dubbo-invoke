package com.hongyan.dubboinvoke.ui;

import com.hongyan.dubboinvoke.config.DubboConfig;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBTextField;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

/**
 * Dubbo配置对话框
 */
public class DubboConfigDialog extends DialogWrapper {
    private final DubboConfig config;
    
    private JBTextField registryAddressField;
    private JBTextField applicationNameField;
    private JBTextField timeoutField;
    private JBTextField retriesField;
    private JBTextField protocolField;
    private JBTextField versionField;
    private JBTextField groupField;
    private JBCheckBox useGenericCheckBox;
    private JBCheckBox showDetailedCommandCheckBox;
    private JBCheckBox useExampleValuesCheckBox;

    public DubboConfigDialog(@NotNull Project project) {
        super(project);
        this.config = DubboConfig.getInstance(project);
        
        setTitle("Dubbo Invoke Configuration");
        setResizable(true);
        init();
        
        loadConfigValues();
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setPreferredSize(new Dimension(500, 400));

        // 创建表单面板
        JPanel formPanel = createFormPanel();
        mainPanel.add(formPanel, BorderLayout.CENTER);

        return mainPanel;
    }

    private JPanel createFormPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        int row = 0;

        // Registry Address
        gbc.gridx = 0; gbc.gridy = row;
        panel.add(new JBLabel("Registry Address:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        registryAddressField = new JBTextField();
        panel.add(registryAddressField, gbc);
        row++;

        // Application Name
        gbc.gridx = 0; gbc.gridy = row; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        panel.add(new JBLabel("Application Name:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        applicationNameField = new JBTextField();
        panel.add(applicationNameField, gbc);
        row++;

        // Timeout
        gbc.gridx = 0; gbc.gridy = row; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        panel.add(new JBLabel("Timeout (ms):"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        timeoutField = new JBTextField();
        panel.add(timeoutField, gbc);
        row++;

        // Retries
        gbc.gridx = 0; gbc.gridy = row; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        panel.add(new JBLabel("Retries:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        retriesField = new JBTextField();
        panel.add(retriesField, gbc);
        row++;

        // Protocol
        gbc.gridx = 0; gbc.gridy = row; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        panel.add(new JBLabel("Protocol:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        protocolField = new JBTextField();
        panel.add(protocolField, gbc);
        row++;

        // Version
        gbc.gridx = 0; gbc.gridy = row; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        panel.add(new JBLabel("Version:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        versionField = new JBTextField();
        panel.add(versionField, gbc);
        row++;

        // Group
        gbc.gridx = 0; gbc.gridy = row; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        panel.add(new JBLabel("Group:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        groupField = new JBTextField();
        panel.add(groupField, gbc);
        row++;

        // Separator
        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 2; gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(new JSeparator(), gbc);
        gbc.gridwidth = 1;
        row++;

        // Use Generic
        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 2; gbc.fill = GridBagConstraints.NONE;
        useGenericCheckBox = new JBCheckBox("Use Generic Invocation");
        panel.add(useGenericCheckBox, gbc);
        gbc.gridwidth = 1;
        row++;

        // Show Detailed Command
        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 2;
        showDetailedCommandCheckBox = new JBCheckBox("Show Detailed Command with Comments");
        panel.add(showDetailedCommandCheckBox, gbc);
        gbc.gridwidth = 1;
        row++;

        // Use Example Values
        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 2;
        useExampleValuesCheckBox = new JBCheckBox("Generate Example Parameter Values");
        panel.add(useExampleValuesCheckBox, gbc);

        return panel;
    }

    private void loadConfigValues() {
        registryAddressField.setText(config.getRegistryAddress());
        applicationNameField.setText(config.getApplicationName());
        timeoutField.setText(String.valueOf(config.getTimeout()));
        retriesField.setText(String.valueOf(config.getRetries()));
        protocolField.setText(config.getProtocol());
        versionField.setText(config.getVersion());
        groupField.setText(config.getGroup());
        useGenericCheckBox.setSelected(config.isUseGeneric());
        showDetailedCommandCheckBox.setSelected(config.isShowDetailedCommand());
        useExampleValuesCheckBox.setSelected(config.isUseExampleValues());
    }

    @Override
    protected void doOKAction() {
        // 保存配置
        config.setRegistryAddress(registryAddressField.getText().trim());
        config.setApplicationName(applicationNameField.getText().trim());
        
        try {
            config.setTimeout(Integer.parseInt(timeoutField.getText().trim()));
        } catch (NumberFormatException e) {
            config.setTimeout(3000);
        }
        
        try {
            config.setRetries(Integer.parseInt(retriesField.getText().trim()));
        } catch (NumberFormatException e) {
            config.setRetries(0);
        }
        
        config.setProtocol(protocolField.getText().trim());
        config.setVersion(versionField.getText().trim());
        config.setGroup(groupField.getText().trim());
        config.setUseGeneric(useGenericCheckBox.isSelected());
        config.setShowDetailedCommand(showDetailedCommandCheckBox.isSelected());
        config.setUseExampleValues(useExampleValuesCheckBox.isSelected());
        
        super.doOKAction();
    }
}