package com.sd.task.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Mark {
    @TableId(value = "id",type = IdType.AUTO)
    private Long id;
    private Long taskId;
    private String operId;
    private String account;
    private Integer status;
    private String locked;
}
