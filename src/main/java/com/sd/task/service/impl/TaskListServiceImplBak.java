//package com.sd.task.service.impl;
//
//import com.sd.task.config.LoadDataCommanRunner;
//import com.sd.task.mapper.MarkMapper;
//import com.sd.task.mapper.TaskListMapper;
//import com.sd.task.pojo.Mark;
//import com.sd.task.pojo.TaskList;
//import com.sd.task.service.TaskListService;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.dao.DataAccessException;
//import org.springframework.data.redis.core.RedisOperations;
//import org.springframework.data.redis.core.SessionCallback;
//import org.springframework.data.redis.core.StringRedisTemplate;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//import org.springframework.util.StringUtils;
//
//import javax.servlet.http.HttpServletRequest;
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//import java.util.concurrent.TimeUnit;
//
//@Service
//public class TaskListServiceImplBak implements TaskListService {
//    private static final String TASK_LIST_LOCK = "task_lock";
//
//    @Autowired
//    private HttpServletRequest request;
//
//    @Autowired
//    private StringRedisTemplate stringRedisTemplate;
//
//    @Autowired
//    private TaskListMapper taskListMapper;
//
//    @Autowired
//    private MarkMapper markMapper;
//
//    @Override
//    public TaskList queryTaskListById(Integer id) {
//        return taskListMapper.selTaskListById(id);
//    }
//
//    /***
//     * 添加一个任务清单
//     * @param taskList
//     * @return
//     */
//    @Override
//    @Transactional
//    public int incremTaskList(TaskList taskList) {
//        int res = -1;
//        try {
//            res = taskListMapper.insTaskList(taskList);
//            List<TaskList> tasks = taskListMapper.selTaskListonOpen();
//            stringRedisTemplate.opsForValue().set(taskList.getVideoId() + ":" + LoadDataCommanRunner.TASKCOUNT,
//                    String.valueOf(taskList.getTaskTotal() - taskList.getTaskCount()), 15, TimeUnit.DAYS);
//            request.getServletContext().setAttribute("taskContext", tasks);
//        } catch (Exception e) {
//            res = -2;
//            e.printStackTrace();
//            return res;
//        }
//        return res;
//    }
//
//    @Override
//    public TaskList queryTaskListByVideoId(String videoId) {
//        return taskListMapper.selTaskListByVideoId(videoId);
//    }
//
//
//    /**
//     * 获取任务单
//     *
//     * @param videoId
//     * @param operId
//     * @return
//     */
//    @Override
//    @Transactional
//    public TaskList getTaskListSafe(String videoId, String operId) {
//        if (!StringUtils.hasLength(videoId) || !StringUtils.hasLength(operId)) {
//            return null;
//        }
//        try {
//            boolean locked = stringRedisTemplate.opsForValue().setIfAbsent(TASK_LIST_LOCK + videoId, operId + videoId, 2, TimeUnit.MINUTES);
//            if (!locked) {
//                return null;
//            }
//            int records = markMapper.selMarkByVOId(videoId, operId);
//            if (records > 0) {
//                stringRedisTemplate.delete(TASK_LIST_LOCK + videoId);
//                return null;
//            }
//            TaskList taskList = taskListMapper.selTaskListByVideoId(videoId);
//            if (taskList == null) {
//                stringRedisTemplate.delete(TASK_LIST_LOCK + videoId);
//                return null;
//            }
//            if (taskList.getState() == 2 || taskList.getState() == 0) {
//                if (taskList.getState() == 0) {
//                    taskListMapper.updTaskState(videoId, 2);
//                }
//                return taskList;
//            }
//        } catch (Exception e) {
//            stringRedisTemplate.delete(TASK_LIST_LOCK + videoId);
//            e.printStackTrace();
//            return null;
//        }
//        return null;
//    }
//
//    @Override
//    public List<TaskList> getTaskListIfUseState(Integer state) {
//        return taskListMapper.selTaskListUseState(state);
//    }
//
//    @Override
//    @Transactional
//    public Map<String, Object> getTaskListSafe(String account, String operId, Integer type) {
//        HashMap<String, Object> resMap = new HashMap<>();
//        List<TaskList> taskContext = new ArrayList<>();
//        taskContext = (ArrayList<TaskList>) request.getServletContext().getAttribute("taskContext");
//        List<TaskList> tasks = new ArrayList<>();
//        if (taskContext == null) {
//            resMap.put("task", null);
//            resMap.put("status", 0);//无任务可接
//            resMap.put("msg", "无任务可接");
//            return resMap;
//        }
//        List<Integer> mTaskIds = markMapper.selTaskIdByOperId(operId);
//        Integer tId = null;
//        if (mTaskIds != null) {//过滤掉已经领过的任务
//            for (TaskList task : taskContext) {
//                if ((!mTaskIds.contains(task.getId()) && ((task.getType() == null) || (task.getType() == type)))) {
//                    tasks.add(task);
//                }
//            }
//        }
//        if (tasks.size() == 0) {
//            resMap.put("task", null);
//            resMap.put("status", 3);//无任务可接
//            resMap.put("msg", "已完成所有任务");
//        }
//        boolean locked = false;
//        TaskList resTask = null;
//        //做两次循环取锁
//        String operLock = null;
//        for (TaskList task : tasks) {
//            Integer lockedNum = task.getTaskTotal() - task.getTaskCount();
//            if (lockedNum > 0) {
//                operLock = TASK_LIST_LOCK + ":" + task.getVideoId() + ":" + operId;
//                System.out.println(operLock + ":::" + account + ":" + operId + ":" + task.getVideoId());
//                try {
//                    locked = stringRedisTemplate.opsForValue().setIfAbsent(operLock,
//                            account + ":" + operId + ":" + task.getVideoId(), 1, TimeUnit.MINUTES);
//                    //locked = setLock(operLock, account + ":" + operId + ":" + task.getVideoId(), 1, TimeUnit.MINUTES);
//                } catch (Exception e) {
//                    e.printStackTrace();
//                    resMap.put("task", null);
//                    resMap.put("status", 2);//无空闲任务
//                    resMap.put("msg", "无空闲任务");
//                    if (StringUtils.hasLength(operLock)) {
//                        stringRedisTemplate.delete(operLock);
//                    }
////                    return resMap;
//                }
//            }
//            if (locked) {//获取到锁,存库减一
//                resTask = task;
//                String taskSTK = task.getVideoId() + ":" + LoadDataCommanRunner.TASKCOUNT;
//                String coutTask = stringRedisTemplate.opsForValue().get(taskSTK);
//                //存量大于0时可以取任务
//                if (StringUtils.hasLength(coutTask) && Integer.valueOf(coutTask) > 0) {
////                    stringRedisTemplate.watch(taskSTK);
////                    stringRedisTemplate.setEnableTransactionSupport(true);
////                    stringRedisTemplate.multi();
////                    stringRedisTemplate.opsForValue().decrement(taskSTK);
////                    stringRedisTemplate.exec();
//                    SessionCallback<Long> sessionCallback = new SessionCallback<Long>() {
//                        @Override
//                        @SuppressWarnings("unchecked")
//                        public Long execute(RedisOperations operations) throws DataAccessException {
//                            operations.watch(taskSTK);
//                            operations.multi();
//                            operations.opsForValue().decrement(taskSTK);
//                            List exec = operations.exec();
//                            System.out.println("exec::" + exec);
//                            if (exec != null && exec.size() > 0) {
//                                return (Long) exec.get(0);
//                            }
//                            return null;
//                        }
//                    };
//                    Long execute = stringRedisTemplate.execute(sessionCallback);
//                    System.out.println(execute);
//                    if (execute == null || execute < 0) {
//                        locked = false;
//                    }
//                } else {//任务库存小于<=0的情况下,补充库存
//                    Integer coutMark = markMapper.countMarkByTaskIdAndStatus(resTask.getId(), 0);
//                    TaskList taskList = taskListMapper.selTaskListById(resTask.getId());
//                    int lockSize = stringRedisTemplate.keys(TASK_LIST_LOCK + ":" + task.getVideoId() + ":*").size();
//                    int realSize = taskList.getTaskTotal() - taskList.getTaskCount();
//                    //对比书库 真实存量
//                    if ((coutMark >= realSize/* && lockSize >= 1*/)) {
//                        Long expire = stringRedisTemplate.getExpire(taskSTK);
//                        System.out.println("时间::"+expire);
//                        setCacheList(taskSTK, realSize, expire);//设置存量的原子操作
////                        stringRedisTemplate.watch(taskSTK);
////                        stringRedisTemplate.setEnableTransactionSupport(true);
////                        stringRedisTemplate.multi();
////                        Long expire = stringRedisTemplate.getExpire(taskSTK);
////                        stringRedisTemplate.opsForValue().set(taskSTK, String.valueOf(realSize), expire);
////                        stringRedisTemplate.opsForValue().decrement(taskSTK);
////                        stringRedisTemplate.exec();
//                        //将领取了任务但没有做的记录标记
//                        markMapper.updMarkStatusByTS(resTask.getId(), 0, 3);
//                        break;
//                    }
//                    //另外一种情况是任务有人还再做,不做库存更新
//                    locked = false;
//                }
//                break;
//            }
//        }
//        if (!locked) {
//            resMap.put("task", null);
//            resMap.put("status", 2);//无空闲任务
//            resMap.put("msg", "无空闲任务");
//            if (StringUtils.hasLength(operLock)) {
//                stringRedisTemplate.delete(operLock);
//            }
//        } else {
//            Mark mark = new Mark();
//            mark.setTaskId(resTask.getId());
//            mark.setOperId(operId);
//            mark.setAccount(account);
//            mark.setStatus(0);
//            mark.setLocked(operLock);
//            markMapper.insMark(mark);
//            resMap.put("task", resTask);
//            resMap.put("status", 1);//成功获取任务Id
//            resMap.put("msg", "成功获取任务");
//        }
//        return resMap;
//    }
//
//
//    private Boolean setLock(String key, String value, long timeout, TimeUnit timeUnit) {
//        SessionCallback<Boolean> sessionCallback = new SessionCallback<Boolean>() {
//            List<Object> exec = null;
//
//            @Override
//            @SuppressWarnings("unchecked")
//            public Boolean execute(RedisOperations operations) throws DataAccessException {
//                operations.multi();
//                stringRedisTemplate.opsForValue().setIfAbsent(key, value, timeout, timeUnit);
//                exec = operations.exec();
//                if (exec.size() > 0) {
//                    return (Boolean) exec.get(0);
//                }
//                return false;
//            }
//        };
//        return stringRedisTemplate.execute(sessionCallback);
//    }
//
//    private void setCacheList(String taskSTK, Integer realSize, long expire) {
//        SessionCallback<List> sessionCallback = new SessionCallback<List>() {
//            @Override
//            @SuppressWarnings("unchecked")
//            public List execute(RedisOperations operations) throws DataAccessException {
//                operations.watch(taskSTK);
//                operations.multi();
//                operations.opsForValue().set(taskSTK, String.valueOf(realSize), expire);
//                operations.opsForValue().decrement(taskSTK);
//                return operations.exec();
//            }
//        };
//        stringRedisTemplate.execute(sessionCallback);
//    }
//
//
//    /**
//     * 提交任务
//     *
//     * @param taskId
//     * @param operId
//     * @param status
//     * @param uid
//     * @return
//     */
//    @Transactional
//    @Override
//    public Map<String, Object> commitTaskSafe(Integer taskId, String operId, int status, Integer uid) {
//        TaskList taskList = null;
//        HashMap<String, Object> resMap = new HashMap<>();
//        try {
//            if (status == 0) {
//                resMap.put("status", 0);
//                resMap.put("msg", "任务失败,释放任务");
//                stringRedisTemplate.delete(TASK_LIST_LOCK + taskId);
//                return resMap;
//            }
//            String taskListstr = stringRedisTemplate.opsForValue().get(TASK_LIST_LOCK + taskId);
//            if (!StringUtils.hasLength(taskListstr) || !(uid + ":" + operId + ":" + taskId).equals(taskListstr)) {
//                resMap.put("status", 2);
//                resMap.put("msg", "请求参数有误");
//                stringRedisTemplate.delete(TASK_LIST_LOCK + taskId);
//                return resMap;
//            }
//            taskList = taskListMapper.selTaskListById(taskId);
//            if (taskList == null) {
//                stringRedisTemplate.delete(TASK_LIST_LOCK + taskId);
//                resMap.put("status", 3);
//                resMap.put("msg", "找不到该任务");
//            }
//            if (taskList.getState() == 0 || taskList.getState() == 2) {//未完成和处理中进行操作
//                Mark mark = new Mark();
//                mark.setTaskId(taskId);
//                mark.setOperId(operId);
////                mark.setUid(uid);
//                mark.setStatus(1);
//                markMapper.insMark(mark);
//                taskListMapper.updTaskCount(taskId);
//                if (taskList.getTaskCount() + 1 >= taskList.getTaskTotal()) {
//                    taskListMapper.updTaskStateById(taskId, 1);
//                    List<Integer> taskIds = taskListMapper.selTaskListId();
//                    request.getServletContext().setAttribute("taskIds", taskIds);
//                }
//                if (taskList.getState() == 0) {
//                    taskListMapper.updTaskStateById(taskId, 2);
//                    List<Integer> taskIds = taskListMapper.selTaskListId();
//                    request.getServletContext().setAttribute("taskIds", taskIds);
//                }
//                stringRedisTemplate.delete(TASK_LIST_LOCK + taskId);
//                resMap.put("status", 1);
//                resMap.put("msg", "任务提交成功");
//            } else if (taskList.getState() == 1) {
//                stringRedisTemplate.delete(TASK_LIST_LOCK + taskId);
//                resMap.put("status", 3);
//                resMap.put("msg", "该任务已终止");
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//            stringRedisTemplate.delete(TASK_LIST_LOCK + taskId);
//            resMap.put("status", 4);
//            resMap.put("msg", "任务失败");
//            return resMap;
//        }
//        return resMap;
//    }
//}
