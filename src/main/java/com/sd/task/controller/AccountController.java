package com.sd.task.controller;

import com.sd.task.pojo.Account;
import com.sd.task.pojo.dto.JSONResult;
import com.sd.task.service.AccountService;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.spring.web.json.Json;

@RestController
@RequestMapping("/account")
public class AccountController {
    @Autowired
    private AccountService accountService;

    @PostMapping("/register")
    public String registryAccount(@RequestBody @Validated Account account) {
        try {
            accountService.createAccount(account);
            return JSONResult.fillResultString(1, "注册成功", account);
        } catch (Exception e) {
            return JSONResult.fillResultString(0, e.getMessage(), JSONObject.NULL);
        }
    }

    @PostMapping("/test")
    public String loginAccount(@RequestBody @Validated Account account) {
        try {
            return JSONResult.fillResultString(1, "注册成功", account);
        } catch (Exception e) {
            return JSONResult.fillResultString(0, e.getMessage(), JSONObject.NULL);
        }
    }

    @GetMapping("/info")
    public String getAccountInfo(@RequestParam String token) {
        try {
            Account account = accountService.selAccountByToken(token);
            JSONObject accountJson = new JSONObject(account);
            return JSONResult.fillResultString(1, "", accountJson);
        } catch (Exception e) {
            return JSONResult.fillResultString(0, e.getMessage(), JSONObject.NULL);
        }
    }
}
