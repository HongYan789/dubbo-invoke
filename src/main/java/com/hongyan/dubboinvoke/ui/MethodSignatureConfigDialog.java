package com.hongyan.dubboinvoke.ui;

import com.hongyan.dubboinvoke.config.MethodSignatureConfig;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.table.JBTable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 方法签名配置对话框
 * 用于配置或编辑Dubbo方法的参数类型签名
 */
public class MethodSignatureConfigDialog extends DialogWrapper {
    
    private final Project project;
    private final MethodSignatureConfig.MethodSignature methodSignature;
    private final boolean isEditing;
    
    // UI组件
    private JTextField serviceInterfaceField;
    private JTextField methodNameField;
    private JTextField returnTypeField;
    private JTextArea descriptionArea;
    private JBTable parametersTable;
    private DefaultTableModel tableModel;
    private JButton addParameterButton;
    private JButton removeParameterButton;
    private JButton quickAddButton;
    
    // 常用类型
    private static final String[] COMMON_TYPES = {
        "java.lang.String",
        "java.lang.Long", 
        "java.lang.Integer",
        "java.lang.Boolean",
        "java.lang.Double",
        "java.lang.Float",
        "java.util.List",
        "java.util.Map",
        "java.util.Date",
        "java.math.BigDecimal"
    };

    /**
     * 新建方法签名
     */
    public MethodSignatureConfigDialog(@NotNull Project project, @NotNull String serviceInterface, @NotNull String methodName) {
        super(project);
        this.project = project;
        this.methodSignature = new MethodSignatureConfig.MethodSignature(serviceInterface, methodName);
        this.isEditing = false;
        
        setTitle("配置方法签名 - " + methodName);
        setResizable(true);
        init();
    }
    
    /**
     * 编辑已有方法签名
     */
    public MethodSignatureConfigDialog(@NotNull Project project, @NotNull MethodSignatureConfig.MethodSignature signature) {
        super(project);
        this.project = project;
        this.methodSignature = signature;
        this.isEditing = true;
        
        setTitle("编辑方法签名 - " + signature.methodName);
        setResizable(true);
        init();
        
        // 填充现有数据
        fillExistingData();
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setPreferredSize(new Dimension(800, 600));
        
        // 创建基本信息面板
        JPanel basicInfoPanel = createBasicInfoPanel();
        mainPanel.add(basicInfoPanel, BorderLayout.NORTH);
        
        // 创建参数配置面板
        JPanel parametersPanel = createParametersPanel();
        mainPanel.add(parametersPanel, BorderLayout.CENTER);
        
        // 创建描述面板
        JPanel descriptionPanel = createDescriptionPanel();
        mainPanel.add(descriptionPanel, BorderLayout.SOUTH);
        
        return mainPanel;
    }
    
    private JPanel createBasicInfoPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("基本信息"));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        
        // 服务接口
        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("服务接口:"), gbc);
        
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        serviceInterfaceField = new JTextField(methodSignature.serviceInterface, 40);
        serviceInterfaceField.setEditable(!isEditing); // 编辑时不允许修改接口名
        panel.add(serviceInterfaceField, gbc);
        
        // 方法名
        gbc.gridx = 0; gbc.gridy = 1; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        panel.add(new JLabel("方法名:"), gbc);
        
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        methodNameField = new JTextField(methodSignature.methodName, 40);
        methodNameField.setEditable(!isEditing); // 编辑时不允许修改方法名
        panel.add(methodNameField, gbc);
        
        // 返回类型
        gbc.gridx = 0; gbc.gridy = 2; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        panel.add(new JLabel("返回类型:"), gbc);
        
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        returnTypeField = new JTextField(methodSignature.returnType, 40);
        panel.add(returnTypeField, gbc);
        
        return panel;
    }
    
    private JPanel createParametersPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("参数配置"));
        
        // 创建参数表格
        String[] columnNames = {"参数名", "参数类型"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return true;
            }
        };
        
        parametersTable = new JBTable(tableModel);
        parametersTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        parametersTable.getTableHeader().setReorderingAllowed(false);
        
        // 设置参数类型列的下拉编辑器
        JComboBox<String> typeComboBox = new JComboBox<>(COMMON_TYPES);
        typeComboBox.setEditable(true);
        parametersTable.getColumnModel().getColumn(1).setCellEditor(new DefaultCellEditor(typeComboBox));
        
        JBScrollPane tableScrollPane = new JBScrollPane(parametersTable);
        tableScrollPane.setPreferredSize(new Dimension(700, 200));
        panel.add(tableScrollPane, BorderLayout.CENTER);
        
        // 创建按钮面板
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        
        addParameterButton = new JButton("添加参数");
        addParameterButton.addActionListener(this::addParameter);
        buttonPanel.add(addParameterButton);
        
        removeParameterButton = new JButton("删除参数");
        removeParameterButton.addActionListener(this::removeParameter);
        buttonPanel.add(removeParameterButton);
        
        quickAddButton = new JButton("快速添加常见参数");
        quickAddButton.addActionListener(this::quickAddParameters);
        buttonPanel.add(quickAddButton);
        
        panel.add(buttonPanel, BorderLayout.SOUTH);
        
        return panel;
    }
    
    private JPanel createDescriptionPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("描述信息"));
        
        descriptionArea = new JTextArea(methodSignature.description, 3, 70);
        descriptionArea.setLineWrap(true);
        descriptionArea.setWrapStyleWord(true);
        
        JBScrollPane scrollPane = new JBScrollPane(descriptionArea);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        return panel;
    }
    
    private void fillExistingData() {
        // 填充现有的参数数据
        for (int i = 0; i < methodSignature.parameterTypes.size(); i++) {
            String paramType = methodSignature.parameterTypes.get(i);
            String paramName = i < methodSignature.parameterNames.size() ? methodSignature.parameterNames.get(i) : "param" + (i + 1);
            tableModel.addRow(new Object[]{paramName, paramType});
        }
    }
    
    private void addParameter(ActionEvent e) {
        int paramIndex = tableModel.getRowCount() + 1;
        tableModel.addRow(new Object[]{"param" + paramIndex, "java.lang.String"});
    }
    
    private void removeParameter(ActionEvent e) {
        int selectedRow = parametersTable.getSelectedRow();
        if (selectedRow >= 0) {
            tableModel.removeRow(selectedRow);
        } else {
            Messages.showWarningDialog("请先选择要删除的参数", "删除参数");
        }
    }
    
    private void quickAddParameters(ActionEvent e) {
        String[] options = {
            "List + Long (列表查询)",
            "List + List + Long (双列表查询)", 
            "String + Integer (名称+页码)",
            "Long (ID查询)",
            "String (名称查询)"
        };
        
        String choice = (String) JOptionPane.showInputDialog(
            this.getContentPanel(),
            "选择要添加的参数组合:",
            "快速添加参数",
            JOptionPane.QUESTION_MESSAGE,
            null,
            options,
            options[0]
        );
        
        if (choice != null) {
            // 清空现有参数
            while (tableModel.getRowCount() > 0) {
                tableModel.removeRow(0);
            }
            
            // 添加对应的参数组合
            switch (choice) {
                case "List + Long (列表查询)":
                    tableModel.addRow(new Object[]{"idList", "java.util.List"});
                    tableModel.addRow(new Object[]{"storeId", "java.lang.Long"});
                    break;
                case "List + List + Long (双列表查询)":
                    tableModel.addRow(new Object[]{"companyIdList", "java.util.List"});
                    tableModel.addRow(new Object[]{"danwBhList", "java.util.List"});
                    tableModel.addRow(new Object[]{"storeId", "java.lang.Long"});
                    break;
                case "String + Integer (名称+页码)":
                    tableModel.addRow(new Object[]{"name", "java.lang.String"});
                    tableModel.addRow(new Object[]{"pageIndex", "java.lang.Integer"});
                    break;
                case "Long (ID查询)":
                    tableModel.addRow(new Object[]{"id", "java.lang.Long"});
                    break;
                case "String (名称查询)":
                    tableModel.addRow(new Object[]{"name", "java.lang.String"});
                    break;
            }
        }
    }
    
    @Override
    protected void doOKAction() {
        if (validateAndSave()) {
            super.doOKAction();
        }
    }
    
    private boolean validateAndSave() {
        // 验证基本信息
        String serviceInterface = serviceInterfaceField.getText().trim();
        String methodName = methodNameField.getText().trim();
        String returnType = returnTypeField.getText().trim();
        
        if (serviceInterface.isEmpty()) {
            Messages.showErrorDialog("服务接口不能为空", "验证错误");
            return false;
        }
        
        if (methodName.isEmpty()) {
            Messages.showErrorDialog("方法名不能为空", "验证错误");
            return false;
        }
        
        // 收集参数信息
        List<String> parameterTypes = new ArrayList<>();
        List<String> parameterNames = new ArrayList<>();
        
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            String paramName = (String) tableModel.getValueAt(i, 0);
            String paramType = (String) tableModel.getValueAt(i, 1);
            
            if (paramName == null || paramName.trim().isEmpty()) {
                Messages.showErrorDialog("第" + (i + 1) + "个参数名不能为空", "验证错误");
                return false;
            }
            
            if (paramType == null || paramType.trim().isEmpty()) {
                Messages.showErrorDialog("第" + (i + 1) + "个参数类型不能为空", "验证错误");
                return false;
            }
            
            parameterNames.add(paramName.trim());
            parameterTypes.add(paramType.trim());
        }
        
        // 更新方法签名数据
        methodSignature.serviceInterface = serviceInterface;
        methodSignature.methodName = methodName;
        methodSignature.returnType = returnType.isEmpty() ? "java.lang.Object" : returnType;
        methodSignature.parameterTypes = parameterTypes;
        methodSignature.parameterNames = parameterNames;
        methodSignature.description = descriptionArea.getText().trim();
        
        // 保存到配置
        MethodSignatureConfig config = MethodSignatureConfig.getInstance(project);
        config.saveMethodSignature(methodSignature);
        
        Messages.showInfoMessage(
            "方法签名已成功保存！\n" +
            "接口: " + serviceInterface + "\n" +
            "方法: " + methodName + "\n" +
            "参数数量: " + parameterTypes.size(),
            "保存成功"
        );
        
        return true;
    }
    
    public MethodSignatureConfig.MethodSignature getMethodSignature() {
        return methodSignature;
    }
}