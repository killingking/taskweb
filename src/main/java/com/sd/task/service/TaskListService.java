package com.sd.task.service;

import com.sd.task.pojo.TaskList;

import java.util.List;
import java.util.Map;

public interface TaskListService {
    TaskList queryTaskListById(Long id);

    int incremTaskList(TaskList taskList);

    TaskList queryTaskListByVideoId(String videoId);

//    boolean commitTaskList(String videoId, String operId, int status);

    List<TaskList> getTaskListIfUseStatus(Integer status);


    Map<String, Object> getTaskListSafe(String account, String operId, Integer type);

    Map<String, Object> commitTaskSafe(String videoId, String operId, String account, Integer status);
}
