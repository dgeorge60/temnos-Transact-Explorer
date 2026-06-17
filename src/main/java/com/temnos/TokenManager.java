package com.temnos;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.http.HttpService;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.http.message.responses.HttpResponse;
import burp.api.montoya.logging.Logging;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TokenManager {
    private final MontoyaApi api;
    private final ConfigUI config;
    private final Logging logging;

    private String jsessionId = null;
    private String jwtToken = null;

    private final Object lock = new Object();

    public TokenManager(MontoyaApi api, ConfigUI config) {
        this.api = api;
        this.config = config;
        this.logging = api.logging();
    }

    public String getJsessionId() {
        if (jsessionId == null) {
            refreshToken();
        }
        return jsessionId;
    }

    public String getJwtToken() {
        if (jwtToken == null) {
            refreshToken();
        }
        return jwtToken;
    }

    public void invalidate() {
        synchronized (lock) {
            jsessionId = null;
            jwtToken = null;
            logging.logToOutput("Tokens invalidated. Will refresh on next request.");
        }
    }

    private void refreshToken() {
        synchronized (lock) {
            if (jsessionId != null && jwtToken != null) {
                return; // Already refreshed by another thread
            }

            logging.logToOutput("Starting OAuth flow to get new JSESSIONID and JWT...");
            
            try {
                // Request 1: Authorize
                String ssoDomain = config.getSsoDomain();
                String authUrl = config.getAuthUrl();
                String ssoCookie = config.getSsoCookie();

                burp.api.montoya.http.message.requests.HttpRequest req1 = burp.api.montoya.http.message.requests.HttpRequest.httpRequestFromUrl("https://" + ssoDomain + authUrl)
                        .withHeader("Host", ssoDomain)
                        .withHeader("Cookie", ssoCookie);

                HttpResponse res1 = api.http().sendRequest(req1).response();
                
                String location = null;
                for (var header : res1.headers()) {
                    if (header.name().equalsIgnoreCase("Location")) {
                        location = header.value();
                        break;
                    }
                }

                if (location == null) {
                    logging.logToError("Failed to find Location header in SSO response.");
                    return;
                }

                // Extract path and query from location
                String redirectPathAndQuery;
                if (location.startsWith("http")) {
                    java.net.URL url = new java.net.URL(location);
                    redirectPathAndQuery = url.getPath() + (url.getQuery() != null ? "?" + url.getQuery() : "");
                } else {
                    redirectPathAndQuery = location;
                }

                // Request 2: Follow Redirect
                String mainDomain = config.getMainDomain();
                burp.api.montoya.http.message.requests.HttpRequest req2 = burp.api.montoya.http.message.requests.HttpRequest.httpRequestFromUrl("https://" + mainDomain + redirectPathAndQuery)
                        .withHeader("Host", mainDomain);

                HttpResponse res2 = api.http().sendRequest(req2).response();
                
                String newJsessionId = null;
                for (var header : res2.headers()) {
                    if (header.name().equalsIgnoreCase("Set-Cookie")) {
                        String val = header.value();
                        if (val.contains("JSESSIONID=")) {
                            Matcher m = Pattern.compile("JSESSIONID=([^;]+)").matcher(val);
                            if (m.find()) {
                                newJsessionId = m.group(1);
                            }
                        }
                    }
                }

                if (newJsessionId == null) {
                    logging.logToError("Failed to extract JSESSIONID from redirect response.");
                    return;
                }

                // Request 3: Get Token
                burp.api.montoya.http.message.requests.HttpRequest req3 = burp.api.montoya.http.message.requests.HttpRequest.httpRequestFromUrl("https://" + mainDomain + "/transact-explorer-wa/token")
                        .withHeader("Host", mainDomain)
                        .withHeader("Cookie", "JSESSIONID=" + newJsessionId)
                        .withHeader("Accept", "application/json, text/plain, */*");

                HttpResponse res3 = api.http().sendRequest(req3).response();
                
                String body = res3.bodyToString();
                Matcher m = Pattern.compile("\"token\"\\s*:\\s*\"([^\"]+)\"").matcher(body);
                String newJwt = null;
                if (m.find()) {
                    newJwt = m.group(1);
                }

                if (newJwt == null) {
                    logging.logToError("Failed to extract JWT from token response.");
                    return;
                }

                this.jsessionId = newJsessionId;
                this.jwtToken = newJwt;
                logging.logToOutput("Successfully refreshed tokens.");

            } catch (Exception e) {
                logging.logToError("Exception during token refresh: " + e.getMessage());
            }
        }
    }
}
