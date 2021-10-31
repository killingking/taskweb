package com.sd.task.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sd.task.pojo.TaskList;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Mapper
@Repository
public interface TaskListMapper extends BaseMapper<TaskList> {
    TaskList selTaskListById(Long id);

    TaskList selTaskListByVideoId(String videoId);


    void updTaskStatusByVideoId(String videoId, Integer status);

    void updTaskStatusById(Long taskId, int status);

    int selTaskStatus(String videoId);


    List<TaskList> selTaskListUseStatus(@Param("status") Integer status);

    TaskList sortTaskList(String operId);

    TaskList sortTaskListFirst();

    int countTaskOnOpen();

    List<Long> selTaskListId();

    void updTaskCount(String videoId);

    List<TaskList> selTaskListonOpen();
}
