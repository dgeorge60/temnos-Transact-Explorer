import re

with open("src/main/java/com/temnos/ConfigUI.java", "r") as f:
    content = f.read()

# Add JTextArea variables
content = content.replace("private JTextField txtSsoCookie;", """private JTextField txtSsoCookie;
    private JTextArea txtLogoutReq1;
    private JTextArea txtLogoutReq2;""")

# Add UI elements
ui_elements = """
        domainsPanel.add(new JLabel("SSO Cookie (Updates daily):"));
        txtSsoCookie = new JTextField("");
        domainsPanel.add(txtSsoCookie);

        JPanel logoutPanel = new JPanel(new GridLayout(2, 1));
        logoutPanel.setBorder(BorderFactory.createTitledBorder("Logout Requests (Auto-updated with old tokens)"));
        
        JPanel req1Panel = new JPanel(new BorderLayout());
        req1Panel.add(new JLabel("Logout Req 1 (API - JWT):"), BorderLayout.NORTH);
        txtLogoutReq1 = new JTextArea(5, 50);
        req1Panel.add(new JScrollPane(txtLogoutReq1), BorderLayout.CENTER);
        
        JPanel req2Panel = new JPanel(new BorderLayout());
        req2Panel.add(new JLabel("Logout Req 2 (Main - JSESSIONID):"), BorderLayout.NORTH);
        txtLogoutReq2 = new JTextArea(5, 50);
        req2Panel.add(new JScrollPane(txtLogoutReq2), BorderLayout.CENTER);
        
        logoutPanel.add(req1Panel);
        logoutPanel.add(req2Panel);
"""
content = re.sub(r'\s*domainsPanel\.add\(new JLabel\("SSO Cookie \(Updates daily\):"\)\);\s*txtSsoCookie = new JTextField\(""\);\s*domainsPanel\.add\(txtSsoCookie\);', ui_elements, content)

# Add to panel
content = content.replace("panel.add(domainsPanel);", """panel.add(domainsPanel);
        panel.add(logoutPanel);""")

# Add getters
getters = """
    public String getSsoCookie() {
        return txtSsoCookie.getText().trim();
    }

    public String getLogoutReq1() {
        return txtLogoutReq1.getText();
    }

    public String getLogoutReq2() {
        return txtLogoutReq2.getText();
    }
"""
content = re.sub(r'\s*public String getSsoCookie\(\) \{\s*return txtSsoCookie\.getText\(\)\.trim\(\);\s*\}', getters, content)

with open("src/main/java/com/temnos/ConfigUI.java", "w") as f:
    f.write(content)
