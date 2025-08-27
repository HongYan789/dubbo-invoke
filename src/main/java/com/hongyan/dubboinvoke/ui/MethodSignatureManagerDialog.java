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
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * 方法签名管理对话框
 * 用于查看、编辑和删除缓存的方法签名
 */
public class MethodSignatureManagerDialog extends DialogWrapper {
    
    private final Project project;
    private final MethodSignatureConfig config;
    
    // UI组件
    private JBTable signaturesTable;
    private DefaultTableModel tableModel;
    private JButton editButton;
    private JButton deleteButton;
    private JButton clearAllButton;
    private JLabel statsLabel;
    
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public MethodSignatureManagerDialog(@NotNull Project project) {
        super(project);
        this.project = project;
        this.config = MethodSignatureConfig.getInstance(project);
        
        setTitle("方法签名管理");
        setResizable(true);
        init();
        
        // 加载数据
        refreshData();
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setPreferredSize(new Dimension(1000, 600));
        
        // 创建统计信息面板
        JPanel statsPanel = createStatsPanel();
        mainPanel.add(statsPanel, BorderLayout.NORTH);
        
        // 创建表格面板
        JPanel tablePanel = createTablePanel();
        mainPanel.add(tablePanel, BorderLayout.CENTER);
        
        // 创建操作按钮面板
        JPanel buttonPanel = createButtonPanel();
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);
        
        return mainPanel;
    }
    
    private JPanel createStatsPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.setBorder(BorderFactory.createTitledBorder("统计信息"));
        
        statsLabel = new JLabel();
        panel.add(statsLabel);
        
        return panel;
    }
    
    private JPanel createTablePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("方法签名列表"));
        
        // 创建表格
        String[] columnNames = {
            "服务接口", "方法名", "参数数量", "返回类型", 
            "使用次数", "创建时间", "最后使用时间", "描述"
        };
        
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // 表格不可编辑
            }
            
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                switch (columnIndex) {
                    case 2: // 参数数量
                    case 4: // 使用次数
                        return Integer.class;
                    default:
                        return String.class;
                }
            }
        };
        
        signaturesTable = new JBTable(tableModel);
        signaturesTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        signaturesTable.getTableHeader().setReorderingAllowed(false);
        signaturesTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        
        // 设置列宽
        signaturesTable.getColumnModel().getColumn(0).setPreferredWidth(200); // 服务接口
        signaturesTable.getColumnModel().getColumn(1).setPreferredWidth(150); // 方法名
        signaturesTable.getColumnModel().getColumn(2).setPreferredWidth(80);  // 参数数量
        signaturesTable.getColumnModel().getColumn(3).setPreferredWidth(120); // 返回类型
        signaturesTable.getColumnModel().getColumn(4).setPreferredWidth(80);  // 使用次数
        signaturesTable.getColumnModel().getColumn(5).setPreferredWidth(130); // 创建时间
        signaturesTable.getColumnModel().getColumn(6).setPreferredWidth(130); // 最后使用时间
        signaturesTable.getColumnModel().getColumn(7).setPreferredWidth(200); // 描述
        
        // 添加双击编辑事件
        signaturesTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    editSelectedSignature();
                }
            }
        });
        
        // 添加选择变化监听
        signaturesTable.getSelectionModel().addListSelectionListener(e -> {
            boolean hasSelection = signaturesTable.getSelectedRow() >= 0;
            editButton.setEnabled(hasSelection);
            deleteButton.setEnabled(hasSelection);
        });
        
        JBScrollPane scrollPane = new JBScrollPane(signaturesTable);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        return panel;
    }
    
    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        
        editButton = new JButton("编辑");
        editButton.setEnabled(false);
        editButton.addActionListener(this::editSignature);
        panel.add(editButton);
        
        deleteButton = new JButton("删除");
        deleteButton.setEnabled(false);
        deleteButton.addActionListener(this::deleteSignature);
        panel.add(deleteButton);
        
        panel.add(Box.createHorizontalStrut(20));
        
        JButton refreshButton = new JButton("刷新");
        refreshButton.addActionListener(e -> refreshData());
        panel.add(refreshButton);
        
        clearAllButton = new JButton("清空所有");
        clearAllButton.addActionListener(this::clearAllSignatures);
        panel.add(clearAllButton);
        
        return panel;
    }
    
    private void refreshData() {
        // 清空现有数据
        while (tableModel.getRowCount() > 0) {
            tableModel.removeRow(0);
        }
        
        // 加载所有方法签名
        List<MethodSignatureConfig.MethodSignature> signatures = config.getAllMethodSignatures();
        
        for (MethodSignatureConfig.MethodSignature signature : signatures) {
            Object[] rowData = {
                signature.serviceInterface,
                signature.methodName,
                signature.parameterTypes.size(),
                signature.returnType.isEmpty() ? "void" : getSimpleClassName(signature.returnType),
                signature.usageCount,
                DATE_FORMAT.format(new Date(signature.createTime)),
                DATE_FORMAT.format(new Date(signature.lastUsedTime)),
                signature.description
            };
            tableModel.addRow(rowData);
        }
        
        // 更新统计信息
        MethodSignatureConfig.CacheStats stats = config.getCacheStats();
        statsLabel.setText(String.format(
            "总计: %d 个方法签名，累计使用: %d 次", 
            stats.totalMethods, 
            stats.totalUsages
        ));
        
        // 更新按钮状态
        clearAllButton.setEnabled(stats.totalMethods > 0);
    }
    
    private void editSignature(ActionEvent e) {
        editSelectedSignature();
    }
    
    private void editSelectedSignature() {
        int selectedRow = signaturesTable.getSelectedRow();
        if (selectedRow < 0) {
            return;
        }
        
        String serviceInterface = (String) tableModel.getValueAt(selectedRow, 0);
        String methodName = (String) tableModel.getValueAt(selectedRow, 1);
        
        MethodSignatureConfig.MethodSignature signature = config.getMethodSignature(serviceInterface, methodName);
        if (signature != null) {
            MethodSignatureConfigDialog dialog = new MethodSignatureConfigDialog(project, signature);
            if (dialog.showAndGet()) {
                refreshData(); // 刷新数据
            }
        }
    }
    
    private void deleteSignature(ActionEvent e) {
        int selectedRow = signaturesTable.getSelectedRow();
        if (selectedRow < 0) {
            return;
        }
        
        String serviceInterface = (String) tableModel.getValueAt(selectedRow, 0);
        String methodName = (String) tableModel.getValueAt(selectedRow, 1);
        
        int result = Messages.showYesNoDialog(
            "确定要删除方法签名吗？\n\n" +
            "接口: " + serviceInterface + "\n" +
            "方法: " + methodName,
            "确认删除",
            Messages.getQuestionIcon()
        );
        
        if (result == Messages.YES) {
            config.removeMethodSignature(serviceInterface, methodName);
            refreshData();
            Messages.showInfoMessage("方法签名已删除", "删除成功");
        }
    }
    
    private void clearAllSignatures(ActionEvent e) {
        MethodSignatureConfig.CacheStats stats = config.getCacheStats();
        if (stats.totalMethods == 0) {
            Messages.showInfoMessage("没有需要清空的方法签名", "提示");
            return;
        }
        
        int result = Messages.showYesNoDialog(
            "确定要清空所有方法签名缓存吗？\n\n" +
            "这将删除 " + stats.totalMethods + " 个方法签名配置，\n" +
            "此操作不可撤销！",
            "确认清空",
            Messages.getWarningIcon()
        );
        
        if (result == Messages.YES) {
            config.clearAll();
            refreshData();
            Messages.showInfoMessage("所有方法签名已清空", "清空成功");
        }
    }
    
    /**
     * 获取类名的简化版本（去掉包名）
     */
    private String getSimpleClassName(String fullClassName) {
        if (fullClassName == null || fullClassName.isEmpty()) {
            return "";
        }
        
        int lastDot = fullClassName.lastIndexOf('.');
        if (lastDot >= 0 && lastDot < fullClassName.length() - 1) {
            return fullClassName.substring(lastDot + 1);
        }
        
        return fullClassName;
    }
    
    @Override
    protected Action @NotNull [] createActions() {
        return new Action[]{new DialogWrapperAction("关闭") {
            @Override
            protected void doAction(ActionEvent e) {
                doCancelAction();
            }
        }};
    }
}