package com.hongyan.dubboinvoke.ui;

import com.hongyan.dubboinvoke.util.JavaMethodParser;
import com.hongyan.dubboinvoke.generator.DubboCommandGenerator;
import com.hongyan.dubboinvoke.service.DubboInvokeService;
import com.hongyan.dubboinvoke.util.OperationLogger;
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
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Dubbo Invoke对话框
 */
public class DubboInvokeDialog extends DialogWrapper {
    private static final OperationLogger logger = OperationLogger.getInstance();
    private final String dubboCommand;
    private JavaMethodParser.MethodInfo methodInfo; // 移除final，允许动态更新
    private final Project project;
    private JBTextArea commandTextArea;
    private JBTextArea methodInfoArea;
    private JBTextArea resultArea;
    private JBTextArea parametersArea;
    private JPanel dynamicParametersPanel; // 动态参数面板
    private java.util.Map<String, JComponent> parameterInputs; // 参数输入组件映射
    private JButton invokeButton;
    private JLabel statusLabel;
    private JProgressBar progressBar;
    private DubboInvokeService dubboInvokeService;
    
    // 服务地址配置相关组件
    private JRadioButton registryRadio;
    private JRadioButton directRadio;
    private JTextField registryAddressField;
    private JTextField directAddressField;
    private ButtonGroup addressTypeGroup;

    public DubboInvokeDialog(@NotNull Project project, 
                           @NotNull String dubboCommand, 
                           @NotNull JavaMethodParser.MethodInfo methodInfo) {
        super(project);
        this.project = project;
        this.dubboCommand = dubboCommand;
        this.methodInfo = methodInfo;
        // 延迟初始化DubboInvokeService，避免静态初始化问题
        this.dubboInvokeService = null;
        // 初始化参数输入组件映射
        this.parameterInputs = new java.util.HashMap<>();
        
        setTitle("Dubbo Invoke Command Generator");
        setResizable(true);
        init();
        
        // 在对话框完全初始化后，执行参数的初始填充
        SwingUtilities.invokeLater(() -> {
            SwingUtilities.invokeLater(() -> {
                initializeParametersFromCommand();
            });
        });
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setPreferredSize(new Dimension(1200, 800));

        // 创建顶部面板，包含方法信息和服务配置
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(createMethodInfoPanel(), BorderLayout.NORTH);
        topPanel.add(createServiceConfigPanel(), BorderLayout.SOUTH);
        mainPanel.add(topPanel, BorderLayout.NORTH);

        // 创建中央三分割面板
        JSplitPane topSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        topSplitPane.setLeftComponent(createCommandPanel());
        topSplitPane.setRightComponent(createParametersPanel());
        topSplitPane.setDividerLocation(500);
        topSplitPane.setResizeWeight(0.5);

        JSplitPane mainSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        mainSplitPane.setTopComponent(topSplitPane);
        mainSplitPane.setBottomComponent(createResultPanel());
        mainSplitPane.setDividerLocation(350);
        mainSplitPane.setResizeWeight(0.65);
        mainPanel.add(mainSplitPane, BorderLayout.CENTER);

        // 创建状态和按钮面板
        JPanel bottomPanel = createStatusAndButtonPanel();
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);

        return mainPanel;
    }

    private JPanel createMethodInfoPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Method Information"));

        methodInfoArea = new JBTextArea();
        methodInfoArea.setEditable(false);
        methodInfoArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        methodInfoArea.setBackground(UIManager.getColor("Panel.background"));
        
        // 生成详细的方法信息
        String methodInfoText = generateDetailedMethodInfo(methodInfo);
        methodInfoArea.setText(methodInfoText);
        methodInfoArea.setRows(6); // 增加行数以显示更多信息

        JBScrollPane scrollPane = new JBScrollPane(methodInfoArea);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }
    
    private JPanel createServiceConfigPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Service Address Configuration"));
        
        // 创建地址类型选择面板
        JPanel typePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        
        registryRadio = new JRadioButton("Registry Center", true);
        directRadio = new JRadioButton("Direct Connection");
        
        addressTypeGroup = new ButtonGroup();
        addressTypeGroup.add(registryRadio);
        addressTypeGroup.add(directRadio);
        
        typePanel.add(registryRadio);
        typePanel.add(directRadio);
        
        // 创建地址输入面板
        JPanel addressPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        
        // 注册中心地址
        gbc.gridx = 0; gbc.gridy = 0; gbc.anchor = GridBagConstraints.WEST;
        addressPanel.add(new JLabel("Registry Address:"), gbc);
        
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        registryAddressField = new JTextField("zookeeper://127.0.0.1:2181", 30);
        addressPanel.add(registryAddressField, gbc);
        
        // 直连地址
        gbc.gridx = 0; gbc.gridy = 1; gbc.anchor = GridBagConstraints.WEST; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        addressPanel.add(new JLabel("Direct Address:"), gbc);
        
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        directAddressField = new JTextField("dubbo://127.0.0.1:20880", 30);
        directAddressField.setEnabled(false); // 默认禁用
        addressPanel.add(directAddressField, gbc);
        
        // 添加单选按钮事件监听
        registryRadio.addActionListener(e -> {
            registryAddressField.setEnabled(true);
            directAddressField.setEnabled(false);
        });
        
        directRadio.addActionListener(e -> {
            registryAddressField.setEnabled(false);
            directAddressField.setEnabled(true);
        });
        
        panel.add(typePanel, BorderLayout.NORTH);
        panel.add(addressPanel, BorderLayout.CENTER);
        
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
        commandTextArea.setEditable(false);
        commandTextArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        commandTextArea.setBackground(UIManager.getColor("Panel.background"));
        commandTextArea.setText(dubboCommand);
        commandTextArea.setRows(4);
        commandTextArea.setLineWrap(true);
        commandTextArea.setWrapStyleWord(true);
        
        // 先设置初始值，避免触发DocumentListener
        isUpdatingFromParameters = true;
        commandTextArea.setText(dubboCommand);
        commandTextArea.selectAll();
        isUpdatingFromParameters = false;
        
        // 添加文档监听器，实现命令文本变化时更新参数面板
        commandTextArea.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                if (!isUpdatingFromParameters && !isUpdatingFromCommand) {
                    SwingUtilities.invokeLater(() -> updateParametersFromCommand());
                }
            }

            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                if (!isUpdatingFromParameters && !isUpdatingFromCommand) {
                    SwingUtilities.invokeLater(() -> updateParametersFromCommand());
                }
            }

            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                if (!isUpdatingFromParameters && !isUpdatingFromCommand) {
                    SwingUtilities.invokeLater(() -> updateParametersFromCommand());
                }
            }
        });

        JBScrollPane scrollPane = new JBScrollPane(commandTextArea);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createResultPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Invoke Result"));

        resultArea = new JBTextArea();
        resultArea.setEditable(false);
        resultArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        resultArea.setBackground(UIManager.getColor("Panel.background"));
        resultArea.setText("Click \"Invoke\" button to execute Dubbo service call...");
        resultArea.setRows(8);
        resultArea.setLineWrap(true);
        resultArea.setWrapStyleWord(true);

        JBScrollPane scrollPane = new JBScrollPane(resultArea);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createParametersPanel() {
        JPanel parametersPanel = new JPanel(new BorderLayout());
        parametersPanel.setBorder(BorderFactory.createTitledBorder("Parameters"));
        
        // 创建动态参数面板
        dynamicParametersPanel = new JPanel();
        dynamicParametersPanel.setLayout(new BoxLayout(dynamicParametersPanel, BoxLayout.Y_AXIS));
        dynamicParametersPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // 根据方法参数生成输入控件
        generateParameterInputs();
        
        JBScrollPane scrollPane = new JBScrollPane(dynamicParametersPanel);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setPreferredSize(new Dimension(400, 300));
        parametersPanel.add(scrollPane, BorderLayout.CENTER);
        
        // 保留原有的文本区域作为备用（隐藏）
        parametersArea = new JBTextArea();
        parametersArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        parametersArea.setBackground(UIManager.getColor("Panel.background"));
        parametersArea.setText("[\"parameter1\", \"parameter2\"]");
        parametersArea.setVisible(false);
        
        return parametersPanel;
    }
    
    /**
     * 根据方法参数生成输入控件
     */
    private void generateParameterInputs() {
        // 清空现有组件
        dynamicParametersPanel.removeAll();
        parameterInputs.clear();
        
        List<JavaMethodParser.ParameterInfo> parameters = methodInfo.getParameters();
        
        if (parameters.isEmpty()) {
            // 无参数方法
            JLabel noParamsLabel = new JLabel("This method has no parameters.");
            noParamsLabel.setHorizontalAlignment(SwingConstants.CENTER);
            dynamicParametersPanel.add(noParamsLabel);
        } else {
            // 为每个参数创建输入控件
            for (int i = 0; i < parameters.size(); i++) {
                JavaMethodParser.ParameterInfo param = parameters.get(i);
                JPanel paramPanel = createParameterInputPanel(param, i);
                dynamicParametersPanel.add(paramPanel);
                
                // 添加间距
                if (i < parameters.size() - 1) {
                    dynamicParametersPanel.add(Box.createVerticalStrut(5));
                }
            }
            
            // 参数初始化将在构造函数完成后执行
        }
        
        // 刷新面板
        dynamicParametersPanel.revalidate();
        dynamicParametersPanel.repaint();
    }
    
    /**
     * 为单个参数创建输入面板
     */
    private JPanel createParameterInputPanel(JavaMethodParser.ParameterInfo param, int index) {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1),
            BorderFactory.createEmptyBorder(8, 8, 8, 8)
        ));
        panel.setBackground(Color.WHITE);
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(2, 5, 2, 5);
        
        // 参数标签
        gbc.gridx = 0; gbc.gridy = 0; gbc.anchor = GridBagConstraints.WEST;
        String labelText = String.format("%d. %s", index + 1, param.getName());
        JLabel nameLabel = new JLabel(labelText);
        nameLabel.setFont(nameLabel.getFont().deriveFont(Font.BOLD));
        panel.add(nameLabel, gbc);
        
        // 参数类型
        gbc.gridx = 1; gbc.anchor = GridBagConstraints.EAST;
        JLabel typeLabel = new JLabel(param.getType());
        typeLabel.setFont(typeLabel.getFont().deriveFont(Font.ITALIC));
        typeLabel.setForeground(Color.GRAY);
        panel.add(typeLabel, gbc);
        
        // 输入组件
        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 2; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        JComponent inputComponent = createInputComponentForType(param.getType());
        parameterInputs.put(param.getName(), inputComponent);
        panel.add(inputComponent, gbc);
        
        return panel;
    }
    
    /**
     * 根据参数类型创建相应的输入控件
     */
    private JComponent createInputComponentForType(String paramType) {
        String normalizedType = paramType.toLowerCase();
        
        if (normalizedType.contains("boolean")) {
            JCheckBox checkBox = new JCheckBox();
            checkBox.setSelected(false);
            // 添加监听器
            checkBox.addActionListener(e -> {
                if (!isUpdatingFromParameters && !isUpdatingFromCommand) {
                    updateCommandFromParameters();
                }
            });
            return checkBox;
        } else {
            // 统一使用可拖动的文本域，支持查看超长参数
            String defaultValue = "";
            int rows = 1;
            
            if (normalizedType.contains("int") || normalizedType.contains("long") || 
                normalizedType.contains("short") || normalizedType.contains("byte")) {
                defaultValue = "0";
            } else if (normalizedType.contains("double") || normalizedType.contains("float")) {
                defaultValue = "0.0";
            } else if (normalizedType.contains("string")) {
                defaultValue = "";
            } else if (normalizedType.contains("list") || normalizedType.contains("[]")
                       || normalizedType.contains("array")) {
                defaultValue = "[]";
                rows = 2;
            } else {
                // 复杂对象类型
                defaultValue = "{}";
                rows = 2;
            }
            
            JBTextArea textArea = new JBTextArea(rows, 20);
            textArea.setText(defaultValue);
            textArea.setLineWrap(true);
            textArea.setWrapStyleWord(true);
            
            // 设置背景色与其他组件一致
            textArea.setBackground(UIManager.getColor("Panel.background"));
            textArea.setBorder(UIManager.getBorder("TextField.border"));
            
            // 添加文档监听器
            textArea.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
                @Override
                public void insertUpdate(javax.swing.event.DocumentEvent e) {
                    if (!isUpdatingFromParameters && !isUpdatingFromCommand) {
                        updateCommandFromParameters();
                    }
                }
                
                @Override
                public void removeUpdate(javax.swing.event.DocumentEvent e) {
                    if (!isUpdatingFromParameters && !isUpdatingFromCommand) {
                        updateCommandFromParameters();
                    }
                }
                
                @Override
                public void changedUpdate(javax.swing.event.DocumentEvent e) {
                    if (!isUpdatingFromParameters && !isUpdatingFromCommand) {
                        updateCommandFromParameters();
                    }
                }
            });
            
            // 创建可拖动的滚动面板
            JBScrollPane scrollPane = new JBScrollPane(textArea);
            scrollPane.setPreferredSize(new Dimension(300, Math.max(25, rows * 20 + 10)));
            scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
            scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
            
            // 简化的拖拽实现 - 使用ResizableComponent
            makeResizable(scrollPane);
            
            return scrollPane;
        }
    }
    
    private void makeResizable(JComponent component) {
        final Point[] startPoint = new Point[1];
        final Dimension[] startSize = new Dimension[1];
        final boolean[] isResizing = new boolean[1];
        
        // 添加拖拽指示器
        component.setBorder(BorderFactory.createCompoundBorder(
            component.getBorder(),
            BorderFactory.createEmptyBorder(0, 0, 2, 2)
        ));
        
        component.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (isInResizeArea(e, component)) {
                    startPoint[0] = e.getPoint();
                    startSize[0] = component.getSize();
                    isResizing[0] = true;
                    component.setCursor(Cursor.getPredefinedCursor(Cursor.SE_RESIZE_CURSOR));
                }
            }
            
            @Override
            public void mouseReleased(MouseEvent e) {
                isResizing[0] = false;
                updateCursor(e, component);
            }
        });
        
        component.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                updateCursor(e, component);
            }
            
            @Override
            public void mouseDragged(MouseEvent e) {
                if (isResizing[0] && startPoint[0] != null && startSize[0] != null) {
                    int deltaX = e.getX() - startPoint[0].x;
                    int deltaY = e.getY() - startPoint[0].y;
                    
                    int newWidth = Math.max(200, startSize[0].width + deltaX);
                    int newHeight = Math.max(50, startSize[0].height + deltaY);
                    
                    Dimension newSize = new Dimension(newWidth, newHeight);
                    component.setPreferredSize(newSize);
                    component.setSize(newSize);
                    
                    // 重新布局父容器
                    Container parent = component.getParent();
                    while (parent != null) {
                        parent.revalidate();
                        parent.repaint();
                        if (parent instanceof JDialog || parent instanceof JFrame) {
                            break;
                        }
                        parent = parent.getParent();
                    }
                }
            }
        });
    }
    
    private boolean isInResizeArea(MouseEvent e, JComponent component) {
        int x = e.getX();
        int y = e.getY();
        int width = component.getWidth();
        int height = component.getHeight();
        
        // 扩大调整大小区域到右下角30x30像素，提高可用性
        return x >= width - 30 && y >= height - 30;
    }
    
    private void updateCursor(MouseEvent e, JComponent component) {
        if (isInResizeArea(e, component)) {
            component.setCursor(Cursor.getPredefinedCursor(Cursor.SE_RESIZE_CURSOR));
        } else {
            component.setCursor(Cursor.getDefaultCursor());
        }
    }
    
    /**
     * 创建状态和按钮面板
     */
    private JPanel createStatusAndButtonPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        
        // 状态面板
        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        statusLabel = new JLabel("Ready");
        progressBar = new JProgressBar();
        progressBar.setVisible(false);
        statusPanel.add(statusLabel);
        statusPanel.add(progressBar);
        
        // 按钮面板
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        
        // 添加查看日志按钮
        JButton viewLogsButton = new JButton("查看日志");
        viewLogsButton.addActionListener(e -> showLogFileLocation());
        
        invokeButton = new JButton("Invoke");
        invokeButton.addActionListener(e -> executeInvoke());
        
        JButton copyButton = new JButton("Copy Command");
        copyButton.addActionListener(e -> copyToClipboard(commandTextArea.getText()));
        
        JButton copyResultButton = new JButton("Copy Result");
        copyResultButton.addActionListener(e -> copyToClipboard(resultArea.getText()));
        
        buttonPanel.add(viewLogsButton);
        buttonPanel.add(invokeButton);
        buttonPanel.add(copyButton);
        buttonPanel.add(copyResultButton);
        
        panel.add(statusPanel, BorderLayout.WEST);
        panel.add(buttonPanel, BorderLayout.EAST);
        
        return panel;
    }
    
    /**
     * 显示日志文件位置
     */
    private void showLogFileLocation() {
        String logFilePath = logger.getLogFilePath();
        String message = "日志文件位置:\n" + logFilePath + "\n\n请复制此路径并在文件管理器中打开。";
        Messages.showInfoMessage(message, "日志文件位置");
    }

    private void copyToClipboard(String text) {
        if (text != null && !text.trim().isEmpty()) {
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            StringSelection selection = new StringSelection(text);
            clipboard.setContents(selection, null);
            
            Messages.showInfoMessage(
                "内容已复制到剪贴板!",
                "Dubbo Invoke Generator"
            );
        }
    }

    private void copyToClipboard(ActionEvent e) {
        String command = commandTextArea.getText();
        copyToClipboard(command);
    }



    private void executeInvoke() {
        logger.log("用户点击了Invoke按钮");
        // 更新状态
        statusLabel.setText("Invoking...");
        progressBar.setVisible(true);
        progressBar.setIndeterminate(true);
        invokeButton.setEnabled(false);
        resultArea.setText("Executing Dubbo service call...");
        
        CompletableFuture.supplyAsync(() -> {
            try {
                logger.log("开始异步执行Dubbo调用");
                
                // 延迟初始化DubboInvokeService，避免静态初始化问题
                if (dubboInvokeService == null) {
                    logger.log("初始化DubboInvokeService");
                    dubboInvokeService = new DubboInvokeService(project);
                }
                
                // 获取服务地址配置
                String serviceAddress = getSelectedServiceAddress();
                logger.log("获取到的服务地址: " + serviceAddress);
                
                if (serviceAddress != null && !serviceAddress.trim().isEmpty()) {
                    // 设置服务地址到DubboInvokeService
                    dubboInvokeService.setServiceAddress(serviceAddress);
                    logger.log("服务地址已设置到DubboInvokeService");
                } else {
                    logger.log("警告: 服务地址为空");
                }
                
                String serviceName = methodInfo.getClassName();
                String methodName = methodInfo.getMethodName();
                logger.log("服务名称: " + serviceName);
                logger.log("方法名称: " + methodName);
                
                // 从动态参数面板收集参数值
                List<Object> parameterValues = collectParameterValues();
                logger.log("收集到的参数数量: " + parameterValues.size());
                for (int i = 0; i < parameterValues.size(); i++) {
                    logger.log("参数" + i + ": " + parameterValues.get(i));
                }
                
                String parametersJson = convertParametersToJson(parameterValues);
                if (parametersJson.isEmpty()) {
                    parametersJson = "[]";
                }
                
                // 注释掉cleanParametersJson调用，避免将[1L]错误转换为[1]
                // parametersJson = cleanParametersJson(parametersJson);
                logger.log("参数JSON: " + parametersJson);
                
                logger.log("开始调用DubboInvokeService.invokeService");
                return dubboInvokeService.invokeService(serviceName, methodName, parametersJson);
            } catch (Exception e) {
                logger.log("Dubbo调用过程中发生异常: " + e.getMessage());
                logger.logException(e);
                return DubboInvokeService.InvokeResult.error("Invoke failed: " + e.getMessage(), e);
            }
        }).whenComplete((result, throwable) -> {
            SwingUtilities.invokeLater(() -> {
                invokeButton.setEnabled(true);
                progressBar.setVisible(false);
                
                if (throwable != null) {
                    logger.log("异步执行完成时发生异常: " + throwable.getMessage());
                    logger.logException(throwable);
                    resultArea.setText("Error: " + throwable.getMessage());
                    statusLabel.setText("Invoke failed: " + throwable.getMessage());
                } else {
                    logger.log("异步执行完成，开始显示结果");
                    displayInvokeResult(result);
                    if (result.isSuccess()) {
                        logger.log("调用成功完成");
                        statusLabel.setText("Invoke completed successfully");
                    } else {
                        logger.log("调用失败: " + result.getErrorMessage());
                        statusLabel.setText("Invoke failed: " + result.getErrorMessage());
                    }
                }
            });
        });
    }
    
    private String extractParametersFromCommand() {
        // 从命令文本中提取参数，使用专门的解析方法
        String commandText = commandTextArea.getText();
        return extractParametersFromCommand(commandText);
    }
    
    private Object[] parseParametersFromText(String parametersText) {
        if (parametersText.isEmpty() || parametersText.equals("[]")) {
            return new Object[0];
        }
        
        try {
            // 简单的JSON数组解析
            parametersText = parametersText.trim();
            if (parametersText.startsWith("[") && parametersText.endsWith("]")) {
                parametersText = parametersText.substring(1, parametersText.length() - 1);
                if (parametersText.trim().isEmpty()) {
                    return new Object[0];
                }
                
                String[] parts = parametersText.split(",");
                Object[] parameters = new Object[parts.length];
                for (int i = 0; i < parts.length; i++) {
                    String part = parts[i].trim();
                    if (part.startsWith("\"") && part.endsWith("\"")) {
                        parameters[i] = part.substring(1, part.length() - 1);
                    } else {
                        parameters[i] = part;
                    }
                }
                return parameters;
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse parameters: " + e.getMessage());
        }
        
        return new Object[]{parametersText};
    }
    
    private void displayInvokeResult(DubboInvokeService.InvokeResult result) {
        if (result.isSuccess()) {
            resultArea.setText("✅ Invoke Success\n\n" + formatJson(result.getResult()));
        } else {
            resultArea.setText("❌ Invoke Failed\n\n" + 
                "Error: " + result.getErrorMessage() + "\n\n" +
                (result.getException() != null ? 
                    "Exception: " + result.getException().getClass().getSimpleName() : ""));
        }
    }
    
    private String formatJson(String json) {
        try {
            // 简单的JSON格式化
            if (json != null && (json.startsWith("{") || json.startsWith("["))) {
                return json.replace(",", ",\n").replace("{", "{\n").replace("}", "\n}");
            }
            return json;
        } catch (Exception e) {
            return json;
        }
    }
    
    /**
     * 清理参数JSON中的Java字面量格式，确保JSON解析器能正确处理
     */
    private String cleanParametersJson(String parametersJson) {
        if (parametersJson == null || parametersJson.trim().isEmpty()) {
            return parametersJson;
        }
        
        try {
            // 清理Long类型字面量：将 1L 转换为 1（不带引号）
            parametersJson = parametersJson.replaceAll("\\b(-?\\d+)[Ll]\\b", "$1");
            
            // 清理Float类型字面量：将 1.0F 转换为 1.0（不带引号）
            parametersJson = parametersJson.replaceAll("\\b(-?\\d*\\.\\d+)[Ff]\\b", "$1");
            
            // 清理Double类型字面量：将 1.0D 转换为 1.0（不带引号）
            parametersJson = parametersJson.replaceAll("\\b(-?\\d*\\.\\d+)[Dd]\\b", "$1");
            
            // 清理带引号的Long类型字面量：将 "1L" 转换为 "1"
            parametersJson = parametersJson.replaceAll("\"(-?\\d+)[Ll]\"", "\"$1\"");
            
            // 清理带引号的Float类型字面量：将 "1.0F" 转换为 "1.0"
            parametersJson = parametersJson.replaceAll("\"(-?\\d*\\.\\d+)[Ff]\"", "\"$1\"");
            
            // 清理带引号的Double类型字面量：将 "1.0D" 转换为 "1.0"
            parametersJson = parametersJson.replaceAll("\"(-?\\d*\\.\\d+)[Dd]\"", "\"$1\"");
            
            return parametersJson;
        } catch (Exception e) {
            logger.log("清理参数JSON时发生异常: " + e.getMessage());
            return parametersJson;
        }
    }

    /**
     * 获取用户选择的服务地址
     */
    private String getSelectedServiceAddress() {
        if (registryRadio.isSelected()) {
            return registryAddressField.getText().trim();
        } else if (directRadio.isSelected()) {
            return directAddressField.getText().trim();
        }
        return null;
    }
    
    /**
     * 收集用户输入的参数值
     */
    private List<Object> collectParameterValues() {
        List<Object> values = new ArrayList<>();
        
        List<JavaMethodParser.ParameterInfo> parameters = methodInfo.getParameters();
        if (parameters.isEmpty()) {
            return values;
        }
        
        for (JavaMethodParser.ParameterInfo param : parameters) {
            JComponent input = parameterInputs.get(param.getName());
            if (input != null) {
                Object value = extractValueFromComponent(input, param.getType());
                values.add(value);
            } else {
                values.add(null);
            }
        }
        
        return values;
    }
    
    /**
     * 从输入组件中提取值
     */
    private Object extractValueFromComponent(JComponent component, String paramType) {
        try {
            if (component instanceof JCheckBox) {
                return ((JCheckBox) component).isSelected();
            } else if (component instanceof JTextField) {
                String text = ((JTextField) component).getText().trim();
                if (text.isEmpty()) {
                    return null;
                }
                
                // 基本类型处理，避免错误的数组转换
                String normalizedType = paramType.toLowerCase();
                if (normalizedType.contains("int")) {
                    return Integer.parseInt(text);
                } else if (normalizedType.contains("long")) {
                    // 处理Long类型，支持1L格式，转换为纯数字
                    if (text.endsWith("L") || text.endsWith("l")) {
                        return Long.parseLong(text.substring(0, text.length() - 1));
                    }
                    return Long.parseLong(text);
                } else if (normalizedType.contains("double")) {
                    return Double.parseDouble(text);
                } else if (normalizedType.contains("float")) {
                    return Float.parseFloat(text);
                } else {
                    return text;
                }
            } else if (component instanceof JBScrollPane) {
                JViewport viewport = ((JBScrollPane) component).getViewport();
                if (viewport.getView() instanceof JBTextArea) {
                    JBTextArea textArea = (JBTextArea) viewport.getView();
                    String text = textArea.getText().trim();
                    if (text.isEmpty()) {
                        return null;
                    }
                    
                    // 对于数组格式的参数，保持原始格式，不要提取内容
                // 这样可以确保List类型参数正确传递给后续处理逻辑
                if (text.startsWith("[") && text.endsWith("]")) {
                    return text; // 保持完整的数组格式
                }
                    return text;
                }
            } else if (component instanceof JBTextArea) {
                String text = ((JBTextArea) component).getText().trim();
                if (text.isEmpty()) {
                    return null;
                }
                
                // 对于数组格式的参数，保持原始格式，不要提取内容
                // 这样可以确保List类型参数正确传递给后续处理逻辑
                if (text.startsWith("[") && text.endsWith("]")) {
                    return text; // 保持完整的数组格式
                }
                return text;
            }
        } catch (Exception e) {
            // 解析失败，返回原始字符串
            if (component instanceof JTextField) {
                return ((JTextField) component).getText();
            } else if (component instanceof JBTextArea) {
                String text = ((JBTextArea) component).getText();
                return text;
            } else if (component instanceof JBScrollPane) {
                JViewport viewport = ((JBScrollPane) component).getViewport();
                if (viewport.getView() instanceof JBTextArea) {
                    String text = ((JBTextArea) viewport.getView()).getText();
                    return text;
                }
            }
        }
        
        return null;
     }
     
     /**
      * 将参数值列表转换为JSON字符串
      * 智能判断是否需要数组格式：单个JSON对象参数直接返回对象，其他情况使用数组格式
      */
     private String convertParametersToJson(List<Object> parameterValues) {
        if (parameterValues == null || parameterValues.isEmpty()) {
            return "";
        }
        
        // 如果只有一个参数且是JSON对象格式，直接返回该对象
        if (parameterValues.size() == 1) {
            Object singleParam = parameterValues.get(0);
            if (singleParam instanceof String) {
                String strParam = (String) singleParam;
                // 检查是否是JSON对象格式
                if (strParam.trim().startsWith("{") && strParam.trim().endsWith("}")) {
                    return strParam;
                }
            }
        }
        
        // 其他情况使用JSON数组格式来包装参数
        return convertToJsonArrayFormat(parameterValues);
     }
    
    private String convertToJsonArrayFormat(List<Object> parameterValues) {
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < parameterValues.size(); i++) {
            if (i > 0) {
                json.append(", ");
            }
            json.append(formatParameterValue(parameterValues.get(i)));
        }
        json.append("]");
        return json.toString();
    }
    
    private String convertToCommaDelimitedFormat(List<Object> parameterValues) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < parameterValues.size(); i++) {
            if (i > 0) {
                result.append(", ");
            }
            result.append(formatParameterValue(parameterValues.get(i)));
        }
        return result.toString();
    }
    
    /**
     * 转换参数为逗号分隔格式，保持List参数的数组格式
     */
    private String convertToCommaDelimitedFormatPreservingArrays(List<Object> parameterValues) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < parameterValues.size(); i++) {
            if (i > 0) {
                result.append(", ");
            }
            Object value = parameterValues.get(i);
            // 对于已经是数组格式的值，直接使用，不进行格式化处理
            if (value instanceof String && ((String) value).startsWith("[") && ((String) value).endsWith("]")) {
                result.append(value);
            } else {
                result.append(formatParameterValue(value));
            }
        }
        return result.toString();
    }
    
    private String formatParameterValue(Object value) {
        if (value == null) {
            return "null";
        } else if (value instanceof String) {
            String strValue = (String) value;
            // 检查是否已经是JSON格式
            if (strValue.startsWith("[") || strValue.startsWith("{")) {
                return strValue;
            } else {
                // 对于字符串值，检查是否是数字字面量（如"1L", "1.0f"等）
                // 注意：保持Long字面量的L后缀，避免Jackson反序列化错误
                if (strValue.matches("\\d+") || strValue.matches("\\d*\\.\\d+") || 
                    strValue.equals("true") || strValue.equals("false") || strValue.equals("null")) {
                    // 纯数字、布尔值、null不需要引号
                    return strValue;
                } else {
                    // 其他字符串（包括带L/F/D后缀的）需要引号
                    return "\"" + strValue.replace("\"", "\\\"") + "\"";
                }
            }
        } else if (value instanceof Boolean || value instanceof Number) {
            return value.toString();
        } else {
            // 其他类型转为字符串
            return "\"" + value.toString().replace("\"", "\\\"") + "\"";
        }
    }
     
     /**
      * 从命令文本更新参数面板（避免循环更新）
      */
     private boolean isUpdatingFromCommand = false;
     private void updateParametersFromCommand() {
         if (isUpdatingFromParameters || isUpdatingFromCommand) {
             logger.log("跳过updateParametersFromCommand，标志位状态: updating=" + isUpdatingFromParameters + ", fromCommand=" + isUpdatingFromCommand);
             return;
         }
         
         SwingUtilities.invokeLater(() -> {
             try {
                 isUpdatingFromCommand = true;
                 logger.log("开始从命令更新参数");
                 String commandText = commandTextArea.getText();
                 if (commandText == null || commandText.trim().isEmpty()) {
                     logger.log("命令为空，跳过参数更新");
                     return;
                 }
                 
                 String parametersJson = extractParametersFromCommand(commandText);
                 logger.log("提取的参数JSON: " + parametersJson);
                 if (parametersJson != null && !parametersJson.trim().isEmpty()) {
                     updateParameterInputsFromJson(parametersJson);
                 } else {
                     logger.log("命令中未找到参数或参数为空");
                 }
             } catch (Exception e) {
                 // 忽略解析错误，避免影响用户输入
                 logger.log("从命令更新参数时发生错误: " + e.getMessage());
                 logger.logException(e);
             } finally {
                 isUpdatingFromCommand = false;
             }
         });
     }
     
     /**
     * 从参数面板更新命令文本（避免循环更新）
     */
    private boolean isUpdatingFromParameters = false;
    private void updateCommandFromParameters() {
        if (isUpdatingFromParameters || isUpdatingFromCommand) {
            logger.log("跳过updateCommandFromParameters，标志位状态: updating=" + isUpdatingFromParameters + ", fromCommand=" + isUpdatingFromCommand);
            return;
        }
        
        SwingUtilities.invokeLater(() -> {
            try {
                isUpdatingFromParameters = true;
                logger.log("开始从参数更新命令");
                List<Object> parameterValues = collectParameterValues();
                
                // 更新命令文本中的参数部分（兼容两种格式）
                String currentCommand = commandTextArea.getText();
                
                // 验证当前命令格式
                if (!isValidCommandFormat(currentCommand)) {
                    logger.log("警告：当前命令格式不正确，跳过更新");
                    return;
                }
                
                String updatedCommand;
                if (currentCommand.contains("$invoke(")) {
                    // 泛化调用使用JSON数组格式
                    String parametersJson = convertParametersToJson(parameterValues);
                    logger.log("收集到的参数JSON: " + parametersJson);
                    updatedCommand = updateCommandParametersGeneric(currentCommand, parametersJson);
                    logger.log("使用泛化调用格式更新命令");
                } else {
                    // 非泛化调用需要保持List参数的数组格式
                    String commaDelimitedParams = convertToCommaDelimitedFormatPreservingArrays(parameterValues);
                    logger.log("收集到的逗号分隔参数: " + commaDelimitedParams);
                    updatedCommand = updateCommandParametersNonGenericDirect(currentCommand, commaDelimitedParams);
                    logger.log("使用非泛化调用格式更新命令");
                }
                
                // 验证更新后的命令格式
                if (isValidCommandFormat(updatedCommand)) {
                    commandTextArea.setText(updatedCommand);
                } else {
                    logger.log("警告：更新后的命令格式不正确，保持原命令");
                }
                logger.log("命令更新完成");
            } catch (Exception e) {
                // 忽略更新错误
                logger.log("从参数更新命令时发生错误: " + e.getMessage());
                logger.logException(e);
            } finally {
                isUpdatingFromParameters = false;
            }
        });
    }
     
     /**
      * 从JSON更新参数输入组件
      */
     private void updateParameterInputsFromJson(String parametersString) {
        // 注意：这个方法是从updateParametersFromCommand调用的，所以isUpdatingFromCommand已经是true
        // 我们只需要检查isUpdatingFromParameters来避免参数面板到命令的反向更新
        
        try {
            // 临时设置标志，避免参数输入组件的DocumentListener触发updateCommandFromParameters
            boolean wasUpdatingFromParameters = isUpdatingFromParameters;
            isUpdatingFromParameters = true;
            
            logger.log("开始从参数字符串更新参数输入组件: " + parametersString);
            
            List<JavaMethodParser.ParameterInfo> parameters = methodInfo.getParameters();
            
            // 如果参数为空或null，清空所有输入组件
            if (parametersString == null || parametersString.trim().isEmpty()) {
                for (JavaMethodParser.ParameterInfo param : parameters) {
                    JComponent input = parameterInputs.get(param.getName());
                    if (input != null) {
                        setComponentValue(input, "", param.getType());
                    }
                }
                isUpdatingFromParameters = wasUpdatingFromParameters;
                return;
            }
            
            // 如果只有一个参数，直接设置参数值
            if (parameters.size() == 1) {
                JavaMethodParser.ParameterInfo param = parameters.get(0);
                JComponent input = parameterInputs.get(param.getName());
                logger.log("设置单个参数 (" + param.getName() + ") 值: " + parametersString);
                if (input != null) {
                    // 直接设置原始值，不进行任何转换
                    setComponentValue(input, parametersString, param.getType());
                } else {
                    logger.log("警告: 未找到参数的输入组件: " + param.getName());
                }
                logger.log("单个参数输入组件更新成功");
            } else {
                // 处理多参数情况
                String[] values;
                
                // 使用splitTopLevel方法智能分割参数
                values = splitTopLevel(parametersString, parameters.size());
                logger.log("处理多参数值: " + parametersString + " -> " + java.util.Arrays.toString(values));
                
                logger.log("解析了 " + values.length + " 个值，有 " + parameters.size() + " 个参数");
                logger.log("参数输入组件映射大小: " + parameterInputs.size());
                
                for (int i = 0; i < Math.min(values.length, parameters.size()); i++) {
                    JavaMethodParser.ParameterInfo param = parameters.get(i);
                    JComponent input = parameterInputs.get(param.getName());
                    logger.log("设置参数 " + i + " (" + param.getName() + ") 值为: " + values[i] + ", 组件: " + (input != null ? input.getClass().getSimpleName() : "null"));
                    if (input != null) {
                        // 直接设置原始值，不进行任何转换
                        setComponentValue(input, values[i], param.getType());
                    } else {
                        logger.log("警告: 未找到参数的输入组件: " + param.getName());
                    }
                }
                logger.log("多参数输入组件更新成功");
            }
            
            // 恢复之前的标志状态
            isUpdatingFromParameters = wasUpdatingFromParameters;
            
        } catch (Exception e) {
            // 忽略解析错误
            logger.log("从参数字符串更新参数输入组件时发生错误: " + e.getMessage());
            logger.logException(e);
            // 确保标志被重置
            isUpdatingFromParameters = false;
        }
    }
     
     /**
      * 简单的JSON数组解析
      */
     private String[] parseJsonArray(String content) {
         List<String> values = new ArrayList<>();
         StringBuilder current = new StringBuilder();
         int depth = 0;
         boolean inString = false;
         boolean escaped = false;
         
         for (char c : content.toCharArray()) {
             if (escaped) {
                 current.append(c);
                 escaped = false;
                 continue;
             }
             
             if (c == '\\') {
                 escaped = true;
                 current.append(c);
                 continue;
             }
             
             if (c == '"' && depth == 0) {
                 inString = !inString;
                 current.append(c);
             } else if (!inString && (c == '{' || c == '[')) {
                 depth++;
                 current.append(c);
             } else if (!inString && (c == '}' || c == ']')) {
                 depth--;
                 current.append(c);
             } else if (!inString && c == ',' && depth == 0) {
                 values.add(current.toString().trim());
                 current = new StringBuilder();
             } else {
                 current.append(c);
             }
         }
         
         if (current.length() > 0) {
             values.add(current.toString().trim());
         }
         
         return values.toArray(new String[0]);
     }
     
     /**
      * 设置组件值
      */
     private void setComponentValue(JComponent component, String value, String paramType) {
         try {
             if (component instanceof JCheckBox) {
                 ((JCheckBox) component).setSelected(Boolean.parseBoolean(value));
             } else if (component instanceof JTextField) {
                 // 移除引号
                 String cleanValue = value;
                 if (cleanValue.startsWith("\"") && cleanValue.endsWith("\"")) {
                     cleanValue = cleanValue.substring(1, cleanValue.length() - 1);
                 }
                 ((JTextField) component).setText(cleanValue);
             } else if (component instanceof JBScrollPane) {
                 JViewport viewport = ((JBScrollPane) component).getViewport();
                 if (viewport.getView() instanceof JBTextArea) {
                     JBTextArea textArea = (JBTextArea) viewport.getView();
                     String cleanValue = value;
                     if (cleanValue.startsWith("\"") && cleanValue.endsWith("\"") && 
                         !cleanValue.startsWith("\"{\"") && !cleanValue.startsWith("\"[")) {
                         cleanValue = cleanValue.substring(1, cleanValue.length() - 1);
                     }
                     textArea.setText(cleanValue);
                 }
             } else if (component instanceof JBTextArea) {
                String cleanValue = value;
                // 直接设置值，不进行额外的包装或处理
                if (cleanValue.startsWith("\"") && cleanValue.endsWith("\"") && 
                    !cleanValue.startsWith("\"{\"") && !cleanValue.startsWith("\"[")) {
                     cleanValue = cleanValue.substring(1, cleanValue.length() - 1);
                 }
                 ((JBTextArea) component).setText(cleanValue);
             }
         } catch (Exception e) {
             // 忽略设置错误
         }
     }
     
     /**
      * 更新命令中的参数部分（泛化调用）
      */
     private String updateCommandParametersGeneric(String command, String parametersJson) {
         int startIndex = command.indexOf("$invoke(");
         if (startIndex == -1) return command;
         
         int openParen = command.indexOf("(", startIndex);
         int closeParen = findMatchingParen(command, openParen);
         if (closeParen == -1) return command;
         
         String beforeParams = command.substring(0, openParen + 1);
         String afterParams = command.substring(closeParen);
         String paramsSection = command.substring(openParen + 1, closeParen);
         String[] parts = paramsSection.split(",", 3);
         
         if (parts.length >= 3) {
             String methodName = parts[0].trim();
             String paramTypes = parts[1].trim();
             return beforeParams + methodName + ", " + paramTypes + ", new Object[]{" + parametersJson + "}" + afterParams;
         }
         
         return command;
     }
     
     /**
      * 更新命令中的参数部分（非泛化调用）
      */
     private String updateCommandParametersNonGeneric(String command, String parametersJson) {
        int lastOpen = command.lastIndexOf('(');
        int lastClose = command.lastIndexOf(')');
        if (lastOpen >= 0 && lastClose > lastOpen) {
            String beforeParams = command.substring(0, lastOpen + 1);
            String afterParams = command.substring(lastClose);
            // 如果参数为空，保持空括号
            if (parametersJson == null || parametersJson.trim().isEmpty()) {
                return beforeParams + afterParams;
            }
            
            // 对于非泛化调用，直接使用JSON数组格式，保持List参数的方括号
            return beforeParams + parametersJson + afterParams;
        }
        return command;
    }
    
    /**
     * 直接使用逗号分隔格式更新非泛化调用命令参数
     */
    private String updateCommandParametersNonGenericDirect(String command, String commaDelimitedParams) {
        int lastOpen = command.lastIndexOf('(');
        int lastClose = command.lastIndexOf(')');
        if (lastOpen >= 0 && lastClose > lastOpen) {
            String beforeParams = command.substring(0, lastOpen + 1);
            String afterParams = command.substring(lastClose);
            // 如果参数为空，保持空括号
            if (commaDelimitedParams == null || commaDelimitedParams.trim().isEmpty()) {
                return beforeParams + afterParams;
            }
            
            return beforeParams + commaDelimitedParams + afterParams;
        }
        return command;
    }
    
    /**
     * 将JSON数组格式转换为逗号分隔格式
     */
    private String convertJsonArrayToCommaDelimited(String jsonArray) {
        if (jsonArray == null || jsonArray.trim().isEmpty()) {
            return "";
        }
        
        // 如果不是JSON数组格式，直接返回
        if (!jsonArray.trim().startsWith("[") || !jsonArray.trim().endsWith("]")) {
            return jsonArray;
        }
        
        try {
            // 解析JSON数组
            String[] tokens = parseJsonArray(jsonArray.trim());
            if (tokens.length == 0) {
                return "";
            }
            
            // 转换为逗号分隔格式
            StringBuilder result = new StringBuilder();
            for (int i = 0; i < tokens.length; i++) {
                if (i > 0) {
                    result.append(", ");
                }
                result.append(tokens[i]);
            }
            return result.toString();
        } catch (Exception e) {
            logger.log("Failed to convert JSON array to comma delimited: " + e.getMessage());
            return jsonArray; // 转换失败时返回原始值
        }
    }
     
     /**
      * 查找匹配的右括号
      */
     private int findMatchingParen(String text, int openIndex) {
         int depth = 1;
         for (int i = openIndex + 1; i < text.length(); i++) {
             char c = text.charAt(i);
             if (c == '(') depth++;
             else if (c == ')') {
                 depth--;
                 if (depth == 0) return i;
             }
         }
         return -1;
     }
     
     /**
      * 验证命令格式是否正确
      */
     private boolean isValidCommandFormat(String command) {
         if (command == null || command.trim().isEmpty()) {
             return false;
         }
         String trimmed = command.trim();
         // 检查是否以invoke开头
         if (!trimmed.startsWith("invoke ")) {
             return false;
         }
         // 检查是否包含方法调用的括号
         int lastOpen = trimmed.lastIndexOf('(');
         int lastClose = trimmed.lastIndexOf(')');
         return lastOpen >= 0 && lastClose > lastOpen;
     }
     
     /**
      * 从命令文本中提取参数JSON（改进版本）
      */
     private String extractParametersFromCommand(String command) {
         try {
             if (command == null) return null;
             String trimmed = command.trim();
             
             // 处理泛化调用: .$invoke("method", new String[]{...}, new Object[]{...})
             int invokeIdx = trimmed.indexOf("$invoke(");
             if (invokeIdx >= 0) {
                 int openParen = trimmed.indexOf('(', invokeIdx);
                 int closeParen = findMatchingParen(trimmed, openParen);
                 if (openParen < 0 || closeParen < 0) return null;
                 String paramsSection = trimmed.substring(openParen + 1, closeParen).trim();
                 String[] threeParts = splitTopLevel(paramsSection, 3);
                 if (threeParts.length >= 3) {
                     String objectArray = threeParts[2].trim();
                     // 期待格式: new Object[]{ ... }
                     int braceOpen = objectArray.indexOf('{');
                     int braceClose = objectArray.lastIndexOf('}');
                     if (objectArray.startsWith("new Object[]") && braceOpen >= 0 && braceClose > braceOpen) {
                         String inside = objectArray.substring(braceOpen + 1, braceClose).trim();
                         // 直接返回参数内容，不进行JSON数组包装
                         return inside;
                     }
                 }
                 return null;
             }
             
             // 处理直接调用: invoke com.Class.method(arg1, arg2, {...})
             int lastOpen = trimmed.lastIndexOf('(');
             int lastClose = trimmed.lastIndexOf(')');
             if (lastOpen >= 0 && lastClose > lastOpen) {
                 String inside = trimmed.substring(lastOpen + 1, lastClose).trim();
                 if (inside.isEmpty()) {
                     return null; // 空参数
                 }
                 
                 // 直接返回参数内容，保持原始格式
                 // [1L] -> [1L], "111" -> "111", {"key":"value"} -> {"key":"value"}
                 return inside;
             }
             
             return null;
         } catch (Exception ex) {
             logger.log("从命令提取参数时发生错误: " + ex.getMessage());
             logger.logException(ex);
             return null;
         }
     }
     
     /**
      * 将字符串拆分为顶层元素
      */
     private String[] splitTopLevel(String text, int max) {
         List<String> tokens = new ArrayList<>();
         StringBuilder current = new StringBuilder();
         int depth = 0;
         boolean inString = false;
         boolean escaped = false;
         
         for (char c : text.toCharArray()) {
             if (escaped) {
                 current.append(c);
                 escaped = false;
                 continue;
             }
             
             if (c == '\\') {
                 escaped = true;
                 current.append(c);
                 continue;
             }
             
             if (c == '"' && depth == 0) {
                 inString = !inString;
                 current.append(c);
             } else if (!inString && (c == '{' || c == '[')) {
                 depth++;
                 current.append(c);
             } else if (!inString && (c == '}' || c == ']')) {
                 depth--;
                 current.append(c);
             } else if (!inString && c == ',' && depth == 0) {
                 tokens.add(current.toString().trim());
                 current = new StringBuilder();
                 if (tokens.size() >= max) break;
             } else {
                 current.append(c);
             }
         }
         
         if (current.length() > 0) {
             tokens.add(current.toString().trim());
         }
         
         return tokens.toArray(new String[0]);
     }
     
     /**
      * 将字符串数组转换为JSON数组
      */
     private String tokensToJsonArray(String[] tokens) {
         StringBuilder json = new StringBuilder("[");
         for (int i = 0; i < tokens.length; i++) {
             if (i > 0) json.append(", ");
             json.append(tokens[i]);
         }
         json.append("]");
         return json.toString();
     }
     
     /**
      * 初始化参数值：从Generated Dubbo Command中解析参数并填充到Parameters面板
      */
     private void initializeParametersFromCommand() {
         if (commandTextArea != null && commandTextArea.getText() != null && !commandTextArea.getText().trim().isEmpty()) {
             logger.log("从命令初始化参数: " + commandTextArea.getText());
             // 延迟执行，确保所有UI组件都已创建完成
             SwingUtilities.invokeLater(() -> {
                 SwingUtilities.invokeLater(() -> {
                     updateParametersFromCommand();
                 });
             });
         }
     }
 
     @Override
    protected @NotNull Action[] createActions() {
        return new Action[]{getOKAction(), getCancelAction()};
    }

    @Override
    protected void doOKAction() {
        copyToClipboard((ActionEvent) null);
        super.doOKAction();
    }
}