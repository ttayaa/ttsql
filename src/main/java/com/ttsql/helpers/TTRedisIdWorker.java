package com.ttsql.helpers;

import org.springframework.data.redis.core.StringRedisTemplate;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

public class TTRedisIdWorker {

    public static StringRedisTemplate stringRedisTemplate;

    /**
     * 时间戳开始时间，从2022年1月1号0点0时0分开始
     */
    private static final Long START_TIME = 1640995200L;

    /**
     * id 生成数量  每天最多2的31次方个订单数量
     */
    private static final int COUNT_BITS = 32;

    private static final String REDIS_COUNT_KEY = "TTRedisPrimaryKey:";


    /**
     * 根据redis生成唯一订单号
     *
     * @return
     */
    public Long generateNextId() {
        // 获取当前时间
        LocalDateTime now = LocalDateTime.now();
        long currentStamp = now.toEpochSecond(ZoneOffset.UTC);
        // 获取当前时间戳（秒）
        long timeStamp = currentStamp - START_TIME;
        // 组装成key=TTRedisPrimaryKey:2022:01:01(组装成这种形式方便日后根据日期统计当天的订单数量)
        String date = now.format(DateTimeFormatter.ofPattern("yyyy:MM:dd:HH:mm"));
        String redisKey = REDIS_COUNT_KEY + date;
        // ID 自增长
        long idCount = stringRedisTemplate.opsForValue().increment(redisKey);
        // 返回唯一ID号（拼接而来的）
        return timeStamp << COUNT_BITS | idCount;
    }

    /**
     * 获取2022年1月1号0点0时0分的时间戳
     * @param args
     */
    public static void main(String[] args) {
        LocalDateTime startLocalTime = LocalDateTime.of(2022, 1, 1, 0, 0, 0);
        long startTime = startLocalTime.toEpochSecond(ZoneOffset.UTC);
        System.out.println(startTime);
        LocalDateTime now = LocalDateTime.now();

        String date = now.format(DateTimeFormatter.ofPattern("yyyy:MM:dd:HH:mm"));
        System.out.println(date);
    }
}
