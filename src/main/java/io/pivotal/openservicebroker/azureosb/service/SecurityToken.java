package io.pivotal.openservicebroker.azureosb.service;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SecurityToken {

    @JsonProperty("access_token")
    private String accessToken;

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }
}
