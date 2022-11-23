package com.ttsql.autoconfigure;


import com.ttsql.TTSQL;
import com.ttsql.TTSQLMapper;
import com.ttsql.helpers.TTRedisIdWorker;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;


@Configuration
@MapperScan({"com.ttsql"})
public class TTSQLAutoConfiguration implements ApplicationContextAware {

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        TTSQL.publicSqlMapper = applicationContext.getBean(TTSQLMapper.class);
        TTRedisIdWorker.stringRedisTemplate = applicationContext.getBean(StringRedisTemplate.class);
    }
}

