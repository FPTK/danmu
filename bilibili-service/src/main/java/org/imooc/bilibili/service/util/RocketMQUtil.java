package org.imooc.bilibili.service.util;


import org.apache.commons.validator.Msg;
import org.apache.rocketmq.client.exception.MQBrokerException;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.CountDownLatch2;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.remoting.exception.RemotingException;

import java.util.concurrent.TimeUnit;

public class RocketMQUtil {

    // 发送信息方法: 1.同步发送 2.异步发送
    public static void syncSengMsg(DefaultMQProducer producer, Message msg) throws Exception {
        // 消息来了, 调用producer中的方法
        SendResult result = producer.send(msg);
        System.out.println(result);
    }

    // 异步发送, 但业务场景对发送消息是否成功不大关心时使用
    public static void asyncSendMsg(DefaultMQProducer producer, Message msg) throws Exception{
        // 对消息发送两次
        // 首先引入一个倒计时类
        int messageCount = 1;
        CountDownLatch2 countDownLatch2 = new CountDownLatch2(messageCount);
        for (int i = 0; i < messageCount; i++) {
            producer.send(msg, new SendCallback(){
                // 发送成功或者发送失败的回调函数
                @Override
                public void onSuccess(SendResult sendResult) {
                    // 成功则计数器减一
                    countDownLatch2.countDown();
                    System.out.println(sendResult.getMsgId());

                }

                @Override
                public void onException(Throwable e) {
                    // 失败也减一, 打印提示语
                    countDownLatch2.countDown();
                    System.out.println("发送消息的时候发生了一场! " + e);
                    e.printStackTrace();
                }
            });
        }
        // 所有信息发送完后, 让计数器等待一段时间
        countDownLatch2.await(5, TimeUnit.SECONDS);
    }
}
