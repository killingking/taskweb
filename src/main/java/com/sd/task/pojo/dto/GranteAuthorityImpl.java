package com.sd.task.pojo.dto;

import org.springframework.security.core.GrantedAuthority;

public class GranteAuthorityImpl implements GrantedAuthority {
    private String authority;

    public GranteAuthorityImpl(String authority) {
        this.authority = authority;
    }

    public void setAuthority(String authority) {
        this.authority = authority;
    }

    @Override
    public String getAuthority() {
        return this.authority;
    }
}
