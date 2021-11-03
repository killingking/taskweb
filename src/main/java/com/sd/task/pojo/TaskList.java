package com.sd.task.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskList {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    @NotNull(message = "不能为空")
    private String videoId;
    private String secuId;
    private String account;
    @Min(value = 0,message = "数值必须大于0")
    private Integer taskTotal;
    private Integer taskCount;
    private Integer status;
    @Min(value = 0,message = "数值必须大于0")
    private Integer price;
    @NotNull
    private Integer type;
    private LocalDateTime createTime;
    private LocalDateTime finishTime;
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime expireTime;
    @TableField("`desc`")
    private String desc;
    private Integer taskLevel;
    private String chanel;
}
