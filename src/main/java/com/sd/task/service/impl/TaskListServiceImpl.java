package com.sd.task.service.impl;

import com.sd.task.config.LoadDataCommanRunner;
import com.sd.task.mapper.MarkMapper;
import com.sd.task.mapper.TaskListMapper;
import com.sd.task.pojo.Mark;
import com.sd.task.pojo.TaskList;
import com.sd.task.service.TaskListService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.sd.task.config.LoadDataCommanRunner.TASKCOUNT;

@Service
public class TaskListServiceImpl implements TaskListService {
    public static final String TASK_LIST_LOCK = "task_lock";

    @Autowired
    private HttpServletRequest request;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private TaskListMapper taskListMapper;

    @Autowired
    private MarkMapper markMapper;

    @Override
    public TaskList queryTaskListById(Long id) {
        return taskListMapper.selTaskListById(id);
    }

    /***
     * 添加一个任务清单
     * @param taskList
     * @return
     */
    @Override
    @Transactional
    public int incremTaskList(TaskList taskList) {
        int res = -1;
        LocalDateTime localDateTime = LocalDateTime.now();
        LocalDateTime expireTime = taskList.getExpireTime();
        taskList.setStatus(0);
        taskList.setCreateTime(localDateTime);
        if (taskList.getType() == null) {
            taskList.setType(0);
        }
        long interval = ChronoUnit.SECONDS.between(localDateTime, expireTime);
        if (interval < 0) {
            res = -2;
            return res;
        }
        res = taskListMapper.insert(taskList);
        List<TaskList> tasks = taskListMapper.selTaskListonOpen();
        stringRedisTemplate.opsForValue().set(taskList.getVideoId() + ":" + TASKCOUNT,
                String.valueOf(taskList.getTaskTotal()), interval, TimeUnit.SECONDS);
        request.getServletContext().setAttribute("taskContext", tasks);
        return res;
    }

    @Override
    public TaskList queryTaskListByVideoId(String videoId) {
        return taskListMapper.selTaskListByVideoId(videoId);
    }



    @Override
    public List<TaskList> getTaskListIfUseStatus(Integer status) {
        return taskListMapper.selTaskListUseStatus(status);
    }

    /**
     * 申请任务
     *
     * @param account
     * @param operId
     * @param type
     * @return
     */
    @Override
    @Transactional
    public Map<String, Object> getTaskListSafe(String account, String operId, Integer type) {
        HashMap<String, Object> resMap = new HashMap<>();
        List<TaskList> taskContext = new ArrayList<>();
        //从context里取的任务都是在处理中的
        taskContext = (ArrayList<TaskList>) request.getServletContext().getAttribute("taskContext");
        List<TaskList> tasks = new ArrayList<>();
        if (taskContext == null) {
            resMap.put("task", null);
            resMap.put("status", 0);//无任务可接
            resMap.put("msg", "无任务可接");
            return resMap;
        }
        List<Long> mTaskIds = markMapper.selTaskIdByOperId(operId);
        if (mTaskIds != null) {//过滤掉已经领过的任务和类型不匹配的任务
            for (TaskList task : taskContext) {
                if ((!mTaskIds.contains(task.getId()) && ((task.getType() == null) || (task.getType() == type)))) {
                    tasks.add(task);
                }
            }
        }
        //如果该operid都匹配不到,那么它就没有可以领的任务
        if (tasks.size() == 0) {
            resMap.put("task", null);
            resMap.put("status", 3);//无任务可接
            resMap.put("msg", "无任务可领");
            return resMap;
        }
        boolean locked = false;
        boolean block = false;
        TaskList resTask = null;
        String operLock = null;
        String taskSTK = null;
        for (TaskList task : tasks) {//从根据operid筛选出来的任务中遍历去取
            LocalDateTime now = LocalDateTime.now();
            if (task.getExpireTime() != null && now.isAfter(task.getExpireTime())) {//如果任务到期了,更新数据库任务状态,从context删除,redis中删除
                taskListMapper.updTaskStatusByVideoId(task.getVideoId(), 3);
                List<TaskList> taskLists = taskListMapper.selTaskListonOpen();
                request.getServletContext().setAttribute("taskContext", taskLists);
                //
                stringRedisTemplate.delete(task.getVideoId() + ":" + TASKCOUNT);
                continue;
            }
            taskSTK = task.getVideoId() + ":" + LoadDataCommanRunner.TASKCOUNT;
            String coutTask = null;
            try {
                if (decrTaskSTK(taskSTK)) {
                    resTask = task;
                    block = true;
                    break;
                } else {
                    resTask = task;
                }
            } catch (Exception e) {
                resMap.put("task", null);
                resMap.put("status", 2);//无空闲任务
                resMap.put("msg", "无空闲任务");
                e.printStackTrace();
                return resMap;
            }
        }
        if (block) {
            try {
                operLock = TASK_LIST_LOCK + ":" + resTask.getVideoId() + ":" + operId;
                locked = stringRedisTemplate.opsForValue().setIfAbsent(operLock,
                        account + ":" + operId + ":" + resTask.getVideoId(), 1, TimeUnit.MINUTES);
            } catch (Exception e) {
                e.printStackTrace();
                resMap.put("task", null);
                resMap.put("status", 2);//无空闲任务
                resMap.put("msg", "无空闲任务");
                if (StringUtils.hasLength(operLock)) {
                    stringRedisTemplate.delete(operLock);
                    stringRedisTemplate.opsForValue().increment(taskSTK);
                }
            }
        } else {
            if (resTask == null) {
                resMap.put("task", null);
                resMap.put("status", 2);//无空闲任务
                resMap.put("msg", "无空闲任务");
                return resMap;
            }
            locked = false;
        }
        if (!locked) {
            resMap.put("task", null);
            resMap.put("status", 2);//无空闲任务
            resMap.put("msg", "无空闲任务");
            if (StringUtils.hasLength(operLock)) {
                stringRedisTemplate.delete(operLock);
            }
        } else {
            Mark mark = new Mark();
            mark.setTaskId(resTask.getId());
            mark.setOperId(operId);
            mark.setAccount(account);
            mark.setStatus(0);
            mark.setLocked(operLock);
            markMapper.insMark(mark);
            resMap.put("task", resTask);
            resMap.put("status", 1);//成功获取任务Id
            resMap.put("msg", "成功获取任务");
        }
        return resMap;
    }

    private boolean decrTaskSTK(String taskSTK) {
        SessionCallback sessionCallback = new SessionCallback() {
            @Override
            public Object execute(RedisOperations operations) throws DataAccessException {
//                operations.setEnableTransactionSupport(true);//开启事务支出
                operations.watch(taskSTK);
                String coutTask = (String) operations.opsForValue().get(taskSTK);
                if (!StringUtils.hasLength(coutTask) || Integer.valueOf(coutTask) <= 0) {
                    return null;
                }
                //存库数量大于0
                operations.multi();
                operations.opsForValue().decrement(taskSTK);
                List exec = operations.exec();
                return exec;
            }
        };
        List execute = (List) stringRedisTemplate.execute(sessionCallback);
        if (execute != null && execute.size() > 0) {
            return true;
        }
        return false;
    }

    /**
     * 提交任务
     *
     * @param videoId
     * @param operId
     * @param account
     * @param status
     * @return
     */
    @Transactional
    @Override
    public Map<String, Object> commitTaskSafe(String videoId, String operId, String account, Integer status) {
        String lockKey = TASK_LIST_LOCK + ":" + videoId + ":" + operId;
        String lockValue = stringRedisTemplate.opsForValue().get(lockKey);
        HashMap<String, Object> resMap = new HashMap<>();
        try {
            if (status == 0) {
                resMap.put("status", 0);
                resMap.put("msg", "任务失败,释放任务");
                markMapper.updMarkStatusByLocked(lockKey, 2);
                stringRedisTemplate.opsForValue().increment(videoId + ":" + LoadDataCommanRunner.TASKCOUNT);
                stringRedisTemplate.delete(lockKey);
                return resMap;
            } else if (status == 1) {
                if (!StringUtils.hasLength(lockValue) || !(account + ":" + operId + ":" + videoId).equals(lockValue)) {
                    resMap.put("status", 3);
                    resMap.put("msg", "任务已提交或参数有误");
                    stringRedisTemplate.delete(lockKey);
                    return resMap;
                }
                TaskList task = taskListMapper.selTaskListByVideoId(videoId);
                if (task == null) {
                    stringRedisTemplate.delete(lockKey);
                    resMap.put("status", 3);
                    resMap.put("msg", "找不到该任务");
                    return resMap;
                }
                if (task.getStatus() == 0 || task.getStatus() == 2) {//未完成和处理中进行操作
                    markMapper.updMarkStatusByLocked(lockKey, 1);
                    taskListMapper.updTaskCount(videoId);
                    if (task.getTaskCount() + 1 >= task.getTaskTotal()) {
                        task.setFinishTime(LocalDateTime.now());
                        task.setStatus(1);
                        task.setTaskCount(task.getTaskCount() + 1);
                        taskListMapper.updateById(task);
//                        taskListMapper.updTaskStatusByVideoId(videoId, 1);
                        stringRedisTemplate.delete(videoId + ":" + LoadDataCommanRunner.TASKCOUNT);
                        List<TaskList> taskLists = taskListMapper.selTaskListonOpen();
                        request.getServletContext().setAttribute("taskContext", taskLists);
                    }
                    if (task.getStatus() == 0) {
                        taskListMapper.updTaskStatusByVideoId(videoId, 2);
                    }
                    stringRedisTemplate.delete(lockKey);
                    resMap.put("status", 1);
                    resMap.put("msg", "任务提交成功");
                    task.setTaskCount(task.getTaskCount() + 1);
                    task.setStatus(1);
                    resMap.put("task", task);
                } else if (task.getStatus() == 1) {
                    stringRedisTemplate.delete(lockKey);
                    resMap.put("status", 3);
                    resMap.put("msg", "该任务额已满");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            stringRedisTemplate.delete(lockKey);
            resMap.put("status", 4);
            resMap.put("msg", "任务失败");
            return resMap;
        }
        return resMap;
    }
}
