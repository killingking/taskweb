package com.sd.task.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sd.task.pojo.dto.AccountCredentials;
import com.sd.task.pojo.dto.JSONResult;
import com.sd.task.utils.TokenAuthenticationService;
import org.json.JSONObject;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class JWTLoginFilter extends AbstractAuthenticationProcessingFilter {
    public JWTLoginFilter(String url, AuthenticationManager authManager) {
        super(new AntPathRequestMatcher(url));
        setAuthenticationManager(authManager);
    }

    //登录时需要验证的操作
    @Override
    public Authentication attemptAuthentication(HttpServletRequest req, HttpServletResponse res) throws AuthenticationException, IOException, ServletException {
        //JSON反序列化成 AccountCredentials
        AccountCredentials creds = new ObjectMapper().readValue(
                req.getInputStream(), AccountCredentials.class);
        //返回一个验证令牌
        return getAuthenticationManager().authenticate(
                new UsernamePasswordAuthenticationToken(
                        creds.getUsername(),
                        creds.getPassword()
                )
        );
    }

    @Override
    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response, FilterChain chain, Authentication authResult) throws IOException, ServletException {
        TokenAuthenticationService.addAuthentication(response, authResult.getName(), authResult);
    }

    @Override
    protected void unsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response, AuthenticationException failed) throws IOException, ServletException {
        response.setContentType("application/json;charset=utf-8");
        response.setStatus(HttpServletResponse.SC_OK);
        response.getOutputStream().write(JSONResult.fillResultString(403, failed.getMessage(), JSONObject.NULL).getBytes(StandardCharsets.UTF_8));
    }
}
