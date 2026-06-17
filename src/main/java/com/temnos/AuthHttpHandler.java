package com.temnos;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.core.ToolType;
import burp.api.montoya.http.handler.*;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.logging.Logging;

import java.util.EnumSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AuthHttpHandler implements HttpHandler {
    private final MontoyaApi api;
    private final TokenManager tokenManager;
    private final ConfigUI config;
    private final Logging logging;

    public AuthHttpHandler(MontoyaApi api, TokenManager tokenManager, ConfigUI config) {
        this.api = api;
        this.tokenManager = tokenManager;
        this.config = config;
        this.logging = api.logging();
    }

    @Override
    public RequestToBeSentAction handleHttpRequestToBeSent(HttpRequestToBeSent requestToBeSent) {
        if (!config.isRunning()) {
            return RequestToBeSentAction.continueWith(requestToBeSent);
        }

        EnumSet<ToolType> activeTools = config.getActiveTools();
        if (!activeTools.contains(requestToBeSent.toolSource().toolType())) {
            return RequestToBeSentAction.continueWith(requestToBeSent);
        }

        HttpRequest modifiedRequest = requestToBeSent;
        String host = requestToBeSent.httpService().host();
        
        // Don't intercept our own auth requests
        if (requestToBeSent.path().contains("/transact-explorer-wa/token") || 
            requestToBeSent.path().contains("/sgconnect/oauth2/authorize")) {
            return RequestToBeSentAction.continueWith(requestToBeSent);
        }

        boolean modified = false;

        if (host.equalsIgnoreCase(config.getMainDomain())) {
            // Replace JSESSIONID if present
            if (requestToBeSent.hasHeader("Cookie")) {
                String cookie = requestToBeSent.headerValue("Cookie");
                if (cookie != null && cookie.contains("JSESSIONID=")) {
                    String jsessionId = tokenManager.getJsessionId();
                    if (jsessionId != null) {
                        String newCookie = cookie.replaceAll("JSESSIONID=[^;]+", "JSESSIONID=" + jsessionId);
                        modifiedRequest = modifiedRequest.withUpdatedHeader("Cookie", newCookie);
                        modified = true;
                    }
                }
            }
        } else if (host.equalsIgnoreCase(config.getApiDomain())) {
            // Replace Authorization if present
            if (requestToBeSent.hasHeader("Authorization")) {
                String auth = requestToBeSent.headerValue("Authorization");
                if (auth != null && auth.startsWith("Bearer ")) {
                    String jwt = tokenManager.getJwtToken();
                    if (jwt != null) {
                        modifiedRequest = modifiedRequest.withUpdatedHeader("Authorization", "Bearer " + jwt);
                        modified = true;
                    }
                }
            }
        }

        if (modified) {
            return RequestToBeSentAction.continueWith(modifiedRequest);
        }

        return RequestToBeSentAction.continueWith(requestToBeSent);
    }

    @Override
    public ResponseReceivedAction handleHttpResponseReceived(HttpResponseReceived responseReceived) {
        if (!config.isRunning()) {
            return ResponseReceivedAction.continueWith(responseReceived);
        }

        EnumSet<ToolType> activeTools = config.getActiveTools();
        if (!activeTools.contains(responseReceived.toolSource().toolType())) {
            return ResponseReceivedAction.continueWith(responseReceived);
        }

        String host = responseReceived.initiatingRequest().httpService().host();
        
        // Handle 401 or 302 related to expired token/session
        if (host.equalsIgnoreCase(config.getApiDomain()) && responseReceived.statusCode() == 401) {
            logging.logToOutput("Received 401 on API domain. Invalidating tokens.");
            tokenManager.invalidate();
        } else if (host.equalsIgnoreCase(config.getMainDomain()) && responseReceived.statusCode() == 302) {
            String location = responseReceived.headerValue("Location");
            if (location != null && location.contains("/sgconnect/oauth2/authorize")) {
                logging.logToOutput("Received 302 redirect to SSO on Main domain. Invalidating tokens.");
                tokenManager.invalidate();
            }
        }

        return ResponseReceivedAction.continueWith(responseReceived);
    }
}
