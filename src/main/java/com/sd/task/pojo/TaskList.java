package com.sd.task.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskList {
    @TableId(value = "id",type = IdType.AUTO)
    private Long id;
    private String videoId;
    private String secuId;
    private String account;
    private Integer taskTotal;
    private Integer taskCount;
    private Integer status;
    private Integer price;
    private Integer type;
    private LocalDateTime createTime;
    private LocalDateTime finishTime;
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime expireTime;
    private String description;
    private Integer taskLevel;
    private String chanel;
}
