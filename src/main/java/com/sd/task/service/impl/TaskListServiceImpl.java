package com.sd.task.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.sd.task.config.LoadDataCommanRunner;
import com.sd.task.mapper.AccountMapper;
import com.sd.task.mapper.MarkMapper;
import com.sd.task.mapper.TaskListMapper;
import com.sd.task.pojo.Account;
import com.sd.task.pojo.Mark;
import com.sd.task.pojo.TaskList;
import com.sd.task.service.AccountService;
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

    @Autowired
    private AccountMapper accountMapper;

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
    public void incremTaskList(TaskList taskList) throws Exception {
        LocalDateTime localDateTime = LocalDateTime.now();
        LocalDateTime expireTime = taskList.getExpireTime();
        taskList.setStatus(0);
        taskList.setCreateTime(localDateTime);
        if (taskList.getType() == null) {
            taskList.setType(0);
        }
        long interval = ChronoUnit.SECONDS.between(localDateTime, expireTime);
        if (interval < 0) {
            throw new Exception("任务期限有误");
        }
        try {
            taskListMapper.insert(taskList);
            List<TaskList> tasks = taskListMapper.selTaskListonOpen();
            stringRedisTemplate.opsForValue().set(taskList.getVideoId() + ":" + TASKCOUNT,
                    String.valueOf(taskList.getTaskTotal()), interval, TimeUnit.SECONDS);
            request.getServletContext().setAttribute("taskContext", tasks);
        } catch (Exception e) {
            throw new Exception("任务已存在,无法重复添加");
        }
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
    public TaskList getTaskListSafe(String account, String operId, Integer type) throws Exception {
        Integer accExists = accountMapper.selectCount(
                new QueryWrapper<Account>()
                        .eq("account", account)
                        .eq("status", 1));
        if (accExists == null || accExists <= 0) {
            throw new Exception("非法账户");
        }
        //从context里取的任务都是在处理中的
        List<TaskList> taskContext = (ArrayList<TaskList>) request.getServletContext().getAttribute("taskContext");
        List<TaskList> tasks = new ArrayList<>();
        if (taskContext == null) {
            throw new Exception("无任务可接");
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
            throw new Exception("无任务可领");
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
            try {
                if (decrTaskSTK(taskSTK)) {
                    resTask = task;
                    block = true;
                    break;
                } else {
                    resTask = task;
                }
            } catch (Exception e) {
                e.printStackTrace();
                throw new Exception("无空闲任务");
            }
        }
        if (block) {
            try {
                operLock = TASK_LIST_LOCK + ":" + resTask.getVideoId() + ":" + operId;
                locked = stringRedisTemplate.opsForValue().setIfAbsent(operLock,
                        account + ":" + operId + ":" + resTask.getVideoId(), 1, TimeUnit.MINUTES);
            } catch (Exception e) {
                e.printStackTrace();
                if (StringUtils.hasLength(operLock)) {
                    stringRedisTemplate.delete(operLock);
                    stringRedisTemplate.opsForValue().increment(taskSTK);
                }
                throw new Exception("无空闲任务");
            }
        } else {
            if (resTask == null) {
                throw new Exception("无空闲任务");
            }
            locked = false;
        }
        if (!locked) {
            if (StringUtils.hasLength(operLock)) {
                stringRedisTemplate.delete(operLock);
            }
            throw new Exception("无空闲任务");
        } else {
            Mark mark = new Mark();
            mark.setTaskId(resTask.getId());
            mark.setOperId(operId);
            mark.setAccount(account);
            mark.setStatus(0);
            mark.setLocked(operLock);
            markMapper.insMark(mark);
            return resTask;
        }
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
    public TaskList commitTaskSafe(String videoId, String operId, String account, Integer status) throws Exception {
        String lockKey = TASK_LIST_LOCK + ":" + videoId + ":" + operId;
        String lockValue = stringRedisTemplate.opsForValue().get(lockKey);
        if (status == 0) {
            markMapper.updMarkStatusByLocked(lockKey, 2);
            stringRedisTemplate.opsForValue().increment(videoId + ":" + LoadDataCommanRunner.TASKCOUNT);
            stringRedisTemplate.delete(lockKey);
            throw new Exception("任务失败,释放任务");
        } else if (status == 1) {
            if (!StringUtils.hasLength(lockValue)) {
                QueryWrapper<Mark> wrapper = new QueryWrapper();
                wrapper.eq("locked", TASK_LIST_LOCK + ":" + videoId + ":" + operId);
                Mark mark = markMapper.selectOne(wrapper);
                if (mark == null) {
                    throw new Exception("参数有误");
                }
                if (mark.getStatus() == 1) {
                    throw new Exception("任务已提交");
                }
                if (mark.getStatus() == 3) {
                    throw new Exception("任务已超时");
                }
                throw new Exception("任务已提交或任务已超时或参数有误");
            }
            if (!(account + ":" + operId + ":" + videoId).equals(lockValue)) {
                stringRedisTemplate.delete(lockKey);
                throw new Exception("提交参数参数有误");
            }
            TaskList task = taskListMapper.selTaskListByVideoId(videoId);
            if (task == null) {
                stringRedisTemplate.delete(lockKey);
                throw new Exception("找不到该任务");
            }
            if (task.getStatus() == 0 || task.getStatus() == 2) {//未完成和处理中进行操作
                markMapper.updMarkStatusByLocked(lockKey, 1);
                taskListMapper.updTaskCount(videoId);
                if (task.getTaskCount() + 1 >= task.getTaskTotal()) {
                    task.setFinishTime(LocalDateTime.now());
                    task.setStatus(1);
                    task.setTaskCount(task.getTaskCount() + 1);
                    taskListMapper.updateById(task);
                    stringRedisTemplate.delete(videoId + ":" + LoadDataCommanRunner.TASKCOUNT);
                    List<TaskList> taskLists = taskListMapper.selTaskListonOpen();
                    request.getServletContext().setAttribute("taskContext", taskLists);
                }
                if (task.getStatus() == 0) {
                    taskListMapper.updTaskStatusByVideoId(videoId, 2);
                }
                stringRedisTemplate.delete(lockKey);
                return task;
            } else if (task.getStatus() == 1) {
                stringRedisTemplate.delete(lockKey);
                throw new Exception("该任务配额已满");
            }
        } else {
            stringRedisTemplate.delete(lockKey);
            throw new Exception("任务失败");
        }
        return null;
    }

    @Override
    @Transactional
    public void deleteTaskById(Long id) throws Exception {
        TaskList taskList = taskListMapper.selTaskListById(id);
        if (taskList == null) {
            throw new Exception("任务不存在");
        }
        taskListMapper.deleteById(id);
        List<TaskList> taskContext = (ArrayList<TaskList>) request.getServletContext().getAttribute("taskContext");
        if (taskContext != null) {
            for (TaskList task : taskContext) {
                if ((task.getId() == id)) {
                    taskContext.remove(task);
                    stringRedisTemplate.delete(taskList.getVideoId() + ":" + LoadDataCommanRunner.TASKCOUNT);
                    break;
                }
            }
        }
    }

    @Override
    public Map<String, Integer> countTask() {
        HashMap<String, Integer> resMap = new HashMap<>();
        QueryWrapper<TaskList> wrapper1 = new QueryWrapper<>();
        QueryWrapper<TaskList> wrapper2 = new QueryWrapper<>();
        QueryWrapper<TaskList> wrapper3 = new QueryWrapper<>();
        wrapper1.eq("status", 0).or().eq("status", 2);
        wrapper2.eq("status", 1);
        wrapper3.eq("status", 3);
        Integer produces = taskListMapper.selectCount(wrapper1);
        Integer success = taskListMapper.selectCount(wrapper2);
        Integer fail = taskListMapper.selectCount(wrapper3);
        resMap.put("success", success);
        resMap.put("produces", produces);
        resMap.put("fail", fail);
        return resMap;
    }
}
