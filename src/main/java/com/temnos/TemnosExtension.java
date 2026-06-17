package com.temnos;

import burp.api.montoya.BurpExtension;
import burp.api.montoya.MontoyaApi;
import burp.api.montoya.logging.Logging;

public class TemnosExtension implements BurpExtension {
    private MontoyaApi api;
    private Logging logging;
    private TokenManager tokenManager;
    private ConfigUI configUI;
    private AuthHttpHandler httpHandler;

    @Override
    public void initialize(MontoyaApi api) {
        this.api = api;
        this.logging = api.logging();

        api.extension().setName("Temnos Transact Explorer Auth");

        configUI = new ConfigUI();
        api.userInterface().registerSuiteTab("Temnos Auth", configUI.getPanel());

        tokenManager = new TokenManager(api, configUI);
        httpHandler = new AuthHttpHandler(api, tokenManager, configUI);

        api.http().registerHttpHandler(httpHandler);

        logging.logToOutput("Temnos Auth Extension Loaded.");
    }
}
