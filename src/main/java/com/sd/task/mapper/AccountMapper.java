package com.sd.task.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sd.task.pojo.Account;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

@Repository
@Mapper
public interface AccountMapper extends BaseMapper<Account> {
    Integer hasRecord(String account);

    Account selectAccountWithRole(Account account);
}
