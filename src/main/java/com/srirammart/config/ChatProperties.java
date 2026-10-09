package com.srirammart.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "chatbot")
public class ChatProperties {
    private boolean enabled = true;
    private final Api api = new Api();
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean v) { this.enabled = v; }
    public Api getApi() { return api; }

    public static class Api {
        private String url = "";
        private String model = "openai";
        private String key = "";
        private int timeoutSeconds = 15;
        public String getUrl() { return url; }
        public void setUrl(String v) { this.url = v; }
        public String getModel() { return model; }
        public void setModel(String v) { this.model = v; }
        public String getKey() { return key; }
        public void setKey(String v) { this.key = v; }
        public int getTimeoutSeconds() { return timeoutSeconds; }
        public void setTimeoutSeconds(int v) { this.timeoutSeconds = v; }
    }
}
