package com.sd.task.component;

import com.sd.task.pojo.Account;
import com.sd.task.service.AccountService;
import lombok.SneakyThrows;
import org.springframework.context.ApplicationContext;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

//自定义身份认证验证组件
public class CustomAuthenticationProvider implements AuthenticationProvider {

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        //获取认证的用户名&密码
        String name = authentication.getName();
        String password = authentication.getCredentials().toString();
        //认证逻辑
        Account account = new Account();
        account.setAccount(name);
        account.setPassword(password);
        ApplicationContext applicationContext = BeanUtils.getApplicationContext();
        AccountService accountService = applicationContext.getBean("accountServiceImpl", AccountService.class);
        Authentication auth = accountService.authAccount(account);
        if (auth != null) {
            return auth;
        } else {
            throw new BadCredentialsException("用户名或密码错误");
        }
    }

    //是否可以提供输入类型的认证服务
    @Override
    public boolean supports(Class<?> authentication) {
        return authentication.equals(UsernamePasswordAuthenticationToken.class);
    }


}
