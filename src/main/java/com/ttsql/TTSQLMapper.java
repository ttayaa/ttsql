package com.ttsql;

import org.apache.ibatis.annotations.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Mapper
public interface TTSQLMapper {

    /**
     * 例子 传入map
     * {
     * 在传入sql要做防注入校验
     * 	"sql": "select * from article where author=#{author} and title like concat('%', #{title}, '%')",
     * 	"author": "zs",
     * 	"title": "领导"
     * }
     */


    @Select("${sql}")
    List<HashMap<String,Object>> select(Map<String, Object> map);

    @Insert("${sql}")
    @Options(useGeneratedKeys = true, keyProperty = "map_keyId", keyColumn = "table_id")
    int insert(Map<String, Object> map);

//    @Insert("${sql}")
//    @Options(useGeneratedKeys = true, keyProperty = "TTKeyID", keyColumn = "student_id")
//    int insert(Object obj);

    @Update("${sql}")
    int update(Map<String, Object> map);

    @Delete("${sql}")
    int delete(Map<String, Object> map);
}

