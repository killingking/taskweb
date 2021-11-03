package com.sd.task.service;

import com.sd.task.pojo.Account;
import org.springframework.security.core.Authentication;

public interface AccountService {
    Account createAccount(Account account) throws Exception;

    Authentication authAccount(Account account);

    Account selAccountByToken(String token);
}
