package org.imooc.bilibili.service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.common.message.Message;
import org.imooc.bilibili.dao.UserMomentsDao;
import org.imooc.bilibili.domain.UserMoment;
import org.imooc.bilibili.service.util.RocketMQUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

import static org.imooc.bilibili.domain.constant.UserMomentsConstant.TOPIC_MOMENTS;

@Service
public class UserMomentsService {

    @Autowired
    private UserMomentsDao userMomentsDao;

    // applicationContext可以获取到springboot的配置和bean, 在这里引入是为了获取在rocketmq配置类中定义的生产者和消费者bean
    @Autowired
    private ApplicationContext applicationContext;


    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    // 新建用户动态
    public void addUserMoments(UserMoment userMoment) throws Exception {
        userMoment.setCreateTime(new Date());
        userMomentsDao.addUserMoments(userMoment);
        // 新建完动态后向mq发送消息, 告知新建了一条动态
        DefaultMQProducer producer = (DefaultMQProducer) applicationContext.getBean("momentsProducer");
        Message msg = new Message(TOPIC_MOMENTS, JSONObject.toJSONString(userMoment).getBytes(StandardCharsets.UTF_8));
        RocketMQUtil.syncSengMsg(producer, msg);
    }

    public List<UserMoment> getUserSubscribedMoments(Long userId) {
        // 消息要从redis中取出
        String key = "subscribed-" + userId;
        // 主要处理逻辑卸载了RocketMQConfig.consumer中
        String listStr = redisTemplate.opsForValue().get(key);
        return JSONArray.parseArray(listStr, UserMoment.class);
    }
}
