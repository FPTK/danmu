package org.imooc.bilibili.service.websocket;

import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.databind.deser.std.MapEntryDeserializer;
import com.mysql.cj.util.StringUtils;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.common.message.Message;
import org.imooc.bilibili.domain.Danmu;
import org.imooc.bilibili.domain.constant.UserMomentsConstant;
import org.imooc.bilibili.service.DanmuService;
import org.imooc.bilibili.service.util.RocketMQUtil;
import org.imooc.bilibili.service.util.TokenUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@ServerEndpoint("/imserver/{token}")
public class WebSocketService {
    // onmessage: 前端有消息发送给服务端
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private static final AtomicInteger ONLINE_COUNT = new AtomicInteger(0);

    public static final ConcurrentHashMap<String, WebSocketService> WEBSOCKET_MAP = new ConcurrentHashMap<>();

    private Session session;

    private String sessionId;

    private Long userId;

    // 建立全局变量, 一定要用static, 解决多例模式下依赖的注入问题
    private static ApplicationContext APPLICATION_CONTEXT;

    public static void setApplicationContext(ApplicationContext applicationContext) {
        WebSocketService.APPLICATION_CONTEXT = applicationContext;
    }

    // onclose: 服务端连接中断, 客户端进行页面刷新, 或者关闭当前页面
    @OnOpen
    public void openConnection(Session session, @PathParam("token") String token) {
        // 获取userid
        try {
            this.userId = TokenUtil.verifyToken(token);
        } catch (Exception e) {}
        this.sessionId = session.getId();
        this.session = session;
        if (WEBSOCKET_MAP.containsKey(sessionId)) {
            WEBSOCKET_MAP.remove(sessionId);
            WEBSOCKET_MAP.put(sessionId, this);
        } else {
            WEBSOCKET_MAP.put(sessionId, this);
            ONLINE_COUNT.getAndIncrement();
        }
        logger.info("用户连接成功, sessionId: " + sessionId + ", 当前在线人数为: " + ONLINE_COUNT);
        try {
            this.sendMessage("0");
        } catch (Exception e) {
            logger.error("连接异常");

        }
    }

    @OnClose
    public void closeConnection() {
        if (WEBSOCKET_MAP.containsKey(sessionId)) {
            WEBSOCKET_MAP.remove(sessionId);
            ONLINE_COUNT.getAndDecrement();
        }
        //  ONLINE_COUNT.get() 将数据转为int
        logger.info("用户退出: " + sessionId + ", 当前在线人数为: " + ONLINE_COUNT.get());
    }

    // message是由前端发送到后端的, 保存的形式是danmu这个类的json形式
    @OnMessage
    public void onMessage(String message) {
        logger.info("用户信息: " + sessionId + ", 报文: " + message);
        if (!StringUtils.isNullOrEmpty(message)) {
            try {
                // 群发消息, 前端传过来一个用户新建的弹幕, 后端将这条弹幕群发给所有的的用户, 群发给所有跟服务器进行长连接的客户端
                for (Map.Entry<String, WebSocketService> entry : WEBSOCKET_MAP.entrySet()) {
                    // 首先获取所有客户端的websocket
                    WebSocketService webSocketService = entry.getValue();
                    // 引入rocketmq来缓解服务器压力
                    DefaultMQProducer danmusProducer = (DefaultMQProducer)APPLICATION_CONTEXT.getBean("danmusProducer");
                    // jsonobject其实就是一个map
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("message", message);
                    jsonObject.put("sessonId", webSocketService.getSessionId());
                    Message msg = new Message(UserMomentsConstant.TOPIC_DANMUS, jsonObject.toJSONString().getBytes(StandardCharsets.UTF_8));
                    RocketMQUtil.asyncSendMsg(danmusProducer, msg);
                }
                // 将弹幕保存到redis和数据库中
                if (this.userId != null) {
                    // 保存弹幕到数据库
                    Danmu danmu = JSONObject.parseObject(message, Danmu.class);
                    danmu.setUserId(userId);
                    danmu.setCreateTime(new Date());
                    DanmuService danmuService = (DanmuService)APPLICATION_CONTEXT.getBean("danmuService");
                    // TODO: 使用rocketmq保存弹幕
                    danmuService.asyncAddDanmu(danmu);
                    // 保存弹幕到redis， redis并发量是数据库的10+倍，而且是串行处理模式，所以不需要异步，如果是分布式的话，需要有分布式锁操作
                    danmuService.addDanmusToRedis(danmu);

                }
            } catch (Exception e) {
                logger.error("弹幕接收出现问题");
                e.printStackTrace();
            }
        }
    }

    @OnError
    public void onError(Throwable error) {

    }

    public void sendMessage(String message) throws IOException {
        this.session.getBasicRemote().sendText(message);
    }

    public Session getSession() {
        return session;
    }


    public String getSessionId() {
        return sessionId;
    }


    //实现定时任务， 参数为指定的时间间隔
    @Scheduled(fixedRate=5000)
    private void noticeOnlineCount() throws IOException {
        for(Map.Entry<String, WebSocketService> entry : WebSocketService.WEBSOCKET_MAP.entrySet()){
            WebSocketService webSocketService = entry.getValue();
            if(webSocketService.session.isOpen()){
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("onlineCount", ONLINE_COUNT.get());
                jsonObject.put("msg", "当前在线人数为" + ONLINE_COUNT.get());
                webSocketService.sendMessage(jsonObject.toJSONString());
            }
        }
    }
}
