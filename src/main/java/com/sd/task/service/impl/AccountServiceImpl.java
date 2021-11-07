package com.sd.task.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.sd.task.mapper.AccountMapper;
import com.sd.task.mapper.AccountRoleMapper;
import com.sd.task.pojo.Account;
import com.sd.task.pojo.AccountRole;
import com.sd.task.pojo.Role;
import com.sd.task.pojo.dto.GranteAuthorityImpl;
import com.sd.task.service.AccountService;
import com.sd.task.utils.TokenAuthenticationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class AccountServiceImpl implements AccountService {
    @Autowired
    private AccountMapper accountMapper;
    @Autowired
    private AccountRoleMapper accountRoleMapper;

    @Override
    @Transactional
    public Account createAccount(Account account) throws Exception {
        Integer hasRecord = accountMapper.hasRecord(account.getAccount());
        if (hasRecord != null) {
            throw new Exception("该用户已存在");
        }
        account.setStatus(1);
        account.setCreateTime(LocalDateTime.now());
        account.setUpdTime(LocalDateTime.now());
        account.setMoney(0);
        accountMapper.insert(account);
        //新增用户默认权限为3-普通成员
        accountRoleMapper.insert(new AccountRole(account.getId(), 3L));
        return account;
    }

    @Override
    public Authentication authAccount(Account account) {
        account = accountMapper.selectAccountWithRole(account);
        //这里设置权限和角色
        if (account == null) {
            return null;
        }
        ArrayList<GrantedAuthority> authorities = new ArrayList<>();
        List<Role> roles = account.getRoles();
        for (Role role : roles) {
            authorities.add(new GranteAuthorityImpl(role.getRoleName()));
        }
        //生成令牌
        Authentication auth = new UsernamePasswordAuthenticationToken(
                account.getAccount(), account.getPassword(), authorities);
        return auth;
    }

    @Override
    public Account selAccountByToken(String token) {
        String username = TokenAuthenticationService.getAuthentication(token);
        QueryWrapper<Account> wrapper = new QueryWrapper<>();
        wrapper.eq("account", username);
        Account account = accountMapper.selectOne(wrapper);
        return account;
    }
}
