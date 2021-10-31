package com.sd.task.config;

import com.sd.task.mapper.TaskListMapper;
import com.sd.task.pojo.TaskList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.context.WebApplicationContext;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
public class LoadDataCommanRunner implements CommandLineRunner {
    @Autowired
    private TaskListMapper taskListMapper;
    @Autowired
    private WebApplicationContext context;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    public static final String TASKCOUNT = "tasknum";

    @Override
    public void run(String... args) throws Exception {
        List<TaskList> tasks = taskListMapper.selTaskListonOpen();
        context.getServletContext().setAttribute("taskContext", tasks);
        for (TaskList task : tasks) {
            stringRedisTemplate.opsForValue().set(task.getVideoId() + ":" + TASKCOUNT, String.valueOf(task.getTaskTotal() - task.getTaskCount()),15, TimeUnit.DAYS);
        }
    }
}
