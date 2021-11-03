package com.sd.task.config;

import com.sd.task.component.CustomAuthenticationProvider;
import com.sd.task.filter.JWTAuthenticationFilter;
import com.sd.task.filter.JWTLoginFilter;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {
    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.csrf().disable()
                //对所有请求认证
                .authorizeRequests()
                // 放行静态资源
                .antMatchers(
                        HttpMethod.GET,
                        "/*.html",
                        "/**/*.html",
                        "/**/*.css",
                        "/**/*.js",
                        "/webSocket/**",
                        "/swagger-resources/**",
                        "/**.ico",
                        "/v2/**",
                        "/profile/**"
                ).permitAll()
                //对所有/account/register的请求都放行
                .antMatchers(HttpMethod.POST, "/account/register").permitAll()
                //所有/login的post请求都放行
                .antMatchers(HttpMethod.POST, "/account/login").permitAll()
                .antMatchers(HttpMethod.GET, "/account/info").permitAll()
                //方便测试开放
                .antMatchers("/api/task/**").permitAll()
                //权限检查
//                .antMatchers("/hello").hasAuthority("AUTH_WRITE")
                //角色检查
                .antMatchers("/api").hasAnyRole("ADMIN", "MENBER")
                .anyRequest().authenticated()
                .and()
                //添加一个过滤器 所有访问/login的请求交给JWTLoginFilter来处理
                .addFilterBefore(new JWTLoginFilter("/account/login", authenticationManager()),
                        UsernamePasswordAuthenticationFilter.class)
                //添加一个过滤器验证其他请求的Token是否合法
                .addFilterBefore(new JWTAuthenticationFilter(),
                        UsernamePasswordAuthenticationFilter.class)
                //解决跨域问题
                .cors().and().csrf().disable();

    }

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        //自定义身份验证组件
        auth.authenticationProvider(new CustomAuthenticationProvider());
    }
}
