package com.sd.task.service;

import com.sd.task.pojo.TaskList;

import java.util.List;
import java.util.Map;

public interface TaskListService {
    TaskList queryTaskListById(Long id);

    void incremTaskList(TaskList taskList) throws Exception;

    TaskList queryTaskListByVideoId(String videoId);

//    boolean commitTaskList(String videoId, String operId, int status);

    List<TaskList> getTaskListIfUseStatus(Integer status);


    TaskList getTaskListSafe(String account, String operId, Integer type) throws Exception;

    TaskList commitTaskSafe(String videoId, String operId, String account, Integer status) throws Exception;
}
