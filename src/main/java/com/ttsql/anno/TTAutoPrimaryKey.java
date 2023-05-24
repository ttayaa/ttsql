package com.ttsql.anno;

import java.lang.annotation.*;

//根据数据库自增  数据库主键必须加上 auto_increment
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.ANNOTATION_TYPE})
public @interface TTAutoPrimaryKey {

}
