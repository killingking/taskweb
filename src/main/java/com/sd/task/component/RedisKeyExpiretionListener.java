package com.sd.task.component;

import com.sd.task.config.LoadDataCommanRunner;
import com.sd.task.mapper.MarkMapper;
import com.sd.task.pojo.Mark;
import com.sd.task.service.impl.TaskListServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.KeyExpirationEventMessageListener;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class RedisKeyExpiretionListener extends KeyExpirationEventMessageListener {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private MarkMapper markMapper;

    public RedisKeyExpiretionListener(RedisMessageListenerContainer listenerContainer) {
        super(listenerContainer);
    }

    @Override
    @Transactional
    public void onMessage(Message message, byte[] pattern) {
        String expireKey = message.toString();
        String operLockKey = TaskListServiceImpl.TASK_LIST_LOCK + ":";
        if (expireKey.startsWith(operLockKey)) {
            String[] splitExpireKey = expireKey.split(":");
            String taskSTKkey = splitExpireKey[1] + ":" + LoadDataCommanRunner.TASKCOUNT;
            stringRedisTemplate.opsForValue().increment(taskSTKkey);
            markMapper.updMarkStatusByVOS(splitExpireKey[1], splitExpireKey[2], 0, 3);
        }
    }
}
