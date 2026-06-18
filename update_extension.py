import re

with open("src/main/java/com/temnos/TemnosExtension.java", "r") as f:
    content = f.read()

# Start background thread
content = content.replace("logging.logToOutput(\"Temnos Auth Extension Loaded.\");", """
        SessionKeepAlive keepAlive = new SessionKeepAlive(api, tokenManager, configUI);
        Thread keepAliveThread = new Thread(keepAlive);
        keepAliveThread.setDaemon(true);
        keepAliveThread.start();
        
        logging.logToOutput("Temnos Auth Extension Loaded.");""")

with open("src/main/java/com/temnos/TemnosExtension.java", "w") as f:
    f.write(content)
