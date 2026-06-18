package com.temnos;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.http.HttpService;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.logging.Logging;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SessionKeepAlive implements Runnable {
    private final MontoyaApi api;
    private final TokenManager tokenManager;
    private final ConfigUI config;
    private final Logging logging;
    
    public SessionKeepAlive(MontoyaApi api, TokenManager tokenManager, ConfigUI config) {
        this.api = api;
        this.tokenManager = tokenManager;
        this.config = config;
        this.logging = api.logging();
    }

    @Override
    public void run() {
        while (true) {
            try {
                Thread.sleep(9 * 60 * 1000); // 9 minutes
                
                if (!config.isRunning()) {
                    continue;
                }
                
                logging.logToOutput("SessionKeepAlive: 9 minutes passed, refreshing session and logging out old session...");
                
                String oldJsession = tokenManager.getJsessionId();
                String oldJwt = tokenManager.getJwtToken();
                
                if (oldJsession == null || oldJwt == null) {
                    logging.logToOutput("SessionKeepAlive: No existing session. Refreshing...");
                    // Force a refresh
                    tokenManager.invalidate(oldJwt, oldJsession);
                    tokenManager.getJsessionId();
                    continue;
                }
                
                // Force a refresh to get new tokens
                tokenManager.invalidate(oldJwt, oldJsession);
                String newJsession = tokenManager.getJsessionId(); // This triggers the login flow
                String newJwt = tokenManager.getJwtToken();
                
                if (newJsession != null && newJwt != null) {
                    logging.logToOutput("SessionKeepAlive: Successfully obtained new tokens. Now sending logout for old tokens.");
                    
                    String req1Str = config.getLogoutReq1();
                    String req2Str = config.getLogoutReq2();
                    
                    if (req1Str != null && !req1Str.isEmpty()) {
                        // Replace JWT in req1Str
                        req1Str = req1Str.replaceAll("Bearer\\s+[A-Za-z0-9-_\\.]+", "Bearer " + oldJwt);
                        sendRawRequest(req1Str, config.getApiDomain());
                    }
                    
                    if (req2Str != null && !req2Str.isEmpty()) {
                        // Replace JSESSIONID in req2Str
                        req2Str = req2Str.replaceAll("JSESSIONID=[^;\\r\\n]+", "JSESSIONID=" + oldJsession);
                        sendRawRequest(req2Str, config.getMainDomain());
                    }
                }
                
            } catch (InterruptedException e) {
                logging.logToError("SessionKeepAlive interrupted: " + e.getMessage());
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                logging.logToError("SessionKeepAlive exception: " + e.getMessage());
            }
        }
    }
    
    private void sendRawRequest(String rawRequest, String host) {
        try {
            HttpService service = HttpService.httpService(host, 443, true);
            HttpRequest request = HttpRequest.httpRequest(service, rawRequest);
            api.http().sendRequest(request);
            logging.logToOutput("SessionKeepAlive: Sent logout request to " + host);
        } catch (Exception e) {
            logging.logToError("SessionKeepAlive: Failed to send logout request to " + host + " - " + e.getMessage());
        }
    }
}
