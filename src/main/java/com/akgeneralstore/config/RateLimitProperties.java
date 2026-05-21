package com.akgeneralstore.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.rate-limit")
public class RateLimitProperties {

    private int apiRequestsPerMinute = 240;
    private int authRequestsPerMinute = 20;
    private int otpRequestsPerMinute = 5;
    private int uploadRequestsPerMinute = 12;

    public int getApiRequestsPerMinute() {
        return apiRequestsPerMinute;
    }

    public void setApiRequestsPerMinute(int apiRequestsPerMinute) {
        this.apiRequestsPerMinute = apiRequestsPerMinute;
    }

    public int getAuthRequestsPerMinute() {
        return authRequestsPerMinute;
    }

    public void setAuthRequestsPerMinute(int authRequestsPerMinute) {
        this.authRequestsPerMinute = authRequestsPerMinute;
    }

    public int getOtpRequestsPerMinute() {
        return otpRequestsPerMinute;
    }

    public void setOtpRequestsPerMinute(int otpRequestsPerMinute) {
        this.otpRequestsPerMinute = otpRequestsPerMinute;
    }

    public int getUploadRequestsPerMinute() {
        return uploadRequestsPerMinute;
    }

    public void setUploadRequestsPerMinute(int uploadRequestsPerMinute) {
        this.uploadRequestsPerMinute = uploadRequestsPerMinute;
    }
}
