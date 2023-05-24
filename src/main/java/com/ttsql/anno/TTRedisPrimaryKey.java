package com.ttsql.anno;

import java.lang.annotation.*;

//根据 雪花算法 + redis 生成id
//推荐使用 解决分布式id 重复问题
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.ANNOTATION_TYPE})
public @interface TTRedisPrimaryKey {

}
