package com.sd.task.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sd.task.pojo.Mark;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Mapper
@Repository
public interface MarkMapper extends BaseMapper<Mark> {

    void insMark(Mark mark);

    int selMarkByVOId(String videoId, String operId);

    Long countMarkByOperId(String operId);

    List<Mark> selTaskByOperId(String operId);

    Long ckeckMarkByTOId(Long taskId, String operId);

    List<Long> selTaskIdByOperId(String operId);

    Long countMarkByTaskIdAndStatus(Long taskId, Integer status);

    void updMarkStatusByTS(Long taskId, Integer srcStatus, Integer descStatus);

    void updMarkStatusByLocked(String locked, Integer status);

    void updMarkStatusByVOS(String videoId,String operId,Integer srcStatus,Integer descStatus);
}
