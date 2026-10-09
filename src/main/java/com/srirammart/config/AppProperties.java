package com.srirammart.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public class AppProperties {
    private String uploadsDir = "./uploads";
    private boolean seedDemoData = true;
    private String rememberMeKey = "change-me";
    private final Admin admin = new Admin();

    public String getUploadsDir() { return uploadsDir; }
    public void setUploadsDir(String v) { this.uploadsDir = v; }
    public boolean isSeedDemoData() { return seedDemoData; }
    public void setSeedDemoData(boolean v) { this.seedDemoData = v; }
    public String getRememberMeKey() { return rememberMeKey; }
    public void setRememberMeKey(String v) { this.rememberMeKey = v; }
    public Admin getAdmin() { return admin; }

    public static class Admin {
        private String username = "admin";
        private String email = "admin@srirammart.local";
        private String password = "Admin@123";
        public String getUsername() { return username; }
        public void setUsername(String v) { this.username = v; }
        public String getEmail() { return email; }
        public void setEmail(String v) { this.email = v; }
        public String getPassword() { return password; }
        public void setPassword(String v) { this.password = v; }
    }
}
