package com.temnos;

import burp.api.montoya.core.ToolType;

import javax.swing.*;
import java.awt.*;
import java.util.EnumSet;

public class ConfigUI {
    private JPanel panel;
    private JCheckBox chkTarget;
    private JCheckBox chkIntruder;
    private JCheckBox chkScanner;
    private JCheckBox chkSequencer;
    private JCheckBox chkProxy;
    private JCheckBox chkRepeater;
    private JCheckBox chkExtensions;

    private JToggleButton btnStartStop;
    
    private JTextField txtSsoDomain;
    private JTextField txtMainDomain;
    private JTextField txtApiDomain;
    private JTextField txtAuthUrl;
    private JTextField txtSsoCookie;

    public ConfigUI() {
        panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        JPanel toolsPanel = new JPanel();
        toolsPanel.setBorder(BorderFactory.createTitledBorder("Active Tools"));
        chkTarget = new JCheckBox("Target");
        chkIntruder = new JCheckBox("Intruder");
        chkScanner = new JCheckBox("Scanner");
        chkSequencer = new JCheckBox("Sequencer");
        chkProxy = new JCheckBox("Proxy (use with caution)");
        chkRepeater = new JCheckBox("Repeater");
        chkExtensions = new JCheckBox("Extensions");

        toolsPanel.add(chkTarget);
        toolsPanel.add(chkIntruder);
        toolsPanel.add(chkScanner);
        toolsPanel.add(chkSequencer);
        toolsPanel.add(chkProxy);
        toolsPanel.add(chkRepeater);
        toolsPanel.add(chkExtensions);

        JPanel domainsPanel = new JPanel(new GridLayout(5, 2));
        domainsPanel.setBorder(BorderFactory.createTitledBorder("Domains & Auth Configuration"));
        
        domainsPanel.add(new JLabel("SSO Domain (e.g., sso.com):"));
        txtSsoDomain = new JTextField("sso.com");
        domainsPanel.add(txtSsoDomain);
        
        domainsPanel.add(new JLabel("Main Domain (e.g., main.com):"));
        txtMainDomain = new JTextField("main.com");
        domainsPanel.add(txtMainDomain);

        domainsPanel.add(new JLabel("API Domain (e.g., api.main.com):"));
        txtApiDomain = new JTextField("api.main.com");
        domainsPanel.add(txtApiDomain);

        domainsPanel.add(new JLabel("Authorize URL path+query:"));
        txtAuthUrl = new JTextField("/sgconnect/oauth2/authorize?scope=openid+profile&response_type=code&redirect_uri=https%3A%2F%2Fmain.com%2Ftransact-explorer-wa%2F&nonce=MTc4MTAwNzA4MTEyNA%3D%3D&client_id=4f08fd1b-65b9-4a17-a700-ab249c060a05");
        domainsPanel.add(txtAuthUrl);

        domainsPanel.add(new JLabel("SSO Cookie (Updates daily):"));
        txtSsoCookie = new JTextField("");
        domainsPanel.add(txtSsoCookie);

        JPanel controlPanel = new JPanel();
        btnStartStop = new JToggleButton("Start Extension");
        btnStartStop.addActionListener(e -> {
            if (btnStartStop.isSelected()) {
                btnStartStop.setText("Stop Extension");
            } else {
                btnStartStop.setText("Start Extension");
            }
        });
        controlPanel.add(btnStartStop);

        panel.add(controlPanel);
        panel.add(domainsPanel);
        panel.add(toolsPanel);
    }

    public JPanel getPanel() {
        return panel;
    }

    public boolean isRunning() {
        return btnStartStop.isSelected();
    }

    public String getSsoDomain() {
        return txtSsoDomain.getText().trim();
    }

    public String getMainDomain() {
        return txtMainDomain.getText().trim();
    }

    public String getApiDomain() {
        return txtApiDomain.getText().trim();
    }

    public String getAuthUrl() {
        return txtAuthUrl.getText().trim();
    }

    public String getSsoCookie() {
        return txtSsoCookie.getText().trim();
    }

    public EnumSet<ToolType> getActiveTools() {
        EnumSet<ToolType> tools = EnumSet.noneOf(ToolType.class);
        if (chkTarget.isSelected()) tools.add(ToolType.TARGET);
        if (chkIntruder.isSelected()) tools.add(ToolType.INTRUDER);
        if (chkScanner.isSelected()) tools.add(ToolType.SCANNER);
        if (chkSequencer.isSelected()) tools.add(ToolType.SEQUENCER);
        if (chkProxy.isSelected()) tools.add(ToolType.PROXY);
        if (chkRepeater.isSelected()) tools.add(ToolType.REPEATER);
        if (chkExtensions.isSelected()) tools.add(ToolType.EXTENSIONS);
        return tools;
    }
}
