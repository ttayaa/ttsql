package com.ttsql;


import com.ttsql.Exception.NotCollectionOrArrayException;
import com.ttsql.anno.TTRedisPrimaryKey;
import com.ttsql.anno.TTSnowFlakePrimaryKey;
import com.ttsql.bean.BuildSource;
import com.ttsql.bean.SqlInfo;
import com.ttsql.builder.JavaSqlInfoBuilder;
import com.ttsql.builder.SqlInfoBuilder;
import com.ttsql.core.ICustomAction;
import com.ttsql.page.TTPage;
import com.ttsql.helpers.*;
import com.ttsql.datasource.DynamicDataSource;
import com.alibaba.fastjson.JSON;
import org.apache.ibatis.annotations.Options;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.*;

public final class TTSQL {

    public static TTIdWorker idWorker = new TTIdWorker();
    public static TTRedisIdWorker redisIdWorker = new TTRedisIdWorker();

    private BuildSource source = new BuildSource(SqlInfo.newInstance());

    private TTSQL() {}

    public static TTSQLMapper publicSqlMapper;


    public HashMap executeQueryOne(){
        end();
        List select = publicSqlMapper.select(source.getSqlInfo().getMybatisMap());
        if (select.size()>0){
            return (HashMap)select.get(0);
        }
        else
        return null;
    }
    public HashMap executeQueryOne(String db){
        try{
            DynamicDataSource.setDataSource(db);
            return executeQueryOne();
        }finally {
            DynamicDataSource.clearDataSource();
        }
    }

    public <T> T executeQueryOne(Class<T> cls){

        HashMap map = executeQueryOne();
        if (map!=null){
            String json = JSON.toJSONString(map);
            return JSON.parseObject(json, cls);
        }else
            return null;
    }
    public <T> T executeQueryOne(String db,Class<T> cls) {
        try{
            DynamicDataSource.setDataSource(db);
            return executeQueryOne(cls);
        }finally {
            DynamicDataSource.clearDataSource();
        }
    }

    public Integer executeQueryCount(){

        HashMap map = executeQueryOne();
        Object firstValue = map.values().iterator().next();

        Integer integer = Integer.valueOf(firstValue.toString());
        return integer;
    }
    public Integer executeQueryCount(String db){
        try{
            DynamicDataSource.setDataSource(db);
            return executeQueryCount();
        }finally {
            DynamicDataSource.clearDataSource();
        }

    }

    public List<HashMap> executeQueryList(){
        end();
        return publicSqlMapper.select(source.getSqlInfo().getMybatisMap());
    }
    public List<HashMap> executeQueryList(String db){
        try{
            DynamicDataSource.setDataSource(db);
            return executeQueryList();
        }finally {
            DynamicDataSource.clearDataSource();
        }
    }

    public <T> List<T> executeQueryList(Class<T> cls){
        List<HashMap> hashMaps = executeQueryList();
        List<T> modelList = JSON.parseArray(JSON.toJSONString(hashMaps),cls);
        return modelList;
    }
    public <T> List<T> executeQueryList(String db,Class<T> cls){
        try{
            DynamicDataSource.setDataSource(db);
            return executeQueryList(cls);
        }finally {
            DynamicDataSource.clearDataSource();
        }
    }

    public List<HashMap> executeQueryPage(TTPage page){
        end();

        SqlInfo sqlInfo = source.getSqlInfo();
        String afterFromSql = sqlInfo.getSql().split("FROM")[1];
        //查询数量
       TTSQL countSQl = TTSQL.start()
                .select("COUNT(*) FROM "+ afterFromSql);
        //由于sql我们自己拼接 所以 还是要模拟
       if(afterFromSql.contains("WHERE")){
           //随便写一个 非0数字 ,否则 WHERE会被删掉
           countSQl.whereConditionsCount = 1;
       }

        countSQl.source.getSqlInfo().setParams(sqlInfo.getParams());

        countSQl.end();

        List<HashMap> countMapList = countSQl.executeQueryList();
        Object obj = countMapList.get(0).get("COUNT(*)");
        Long total = Long.valueOf(String.valueOf(obj));
        page.setTotal(total);
        long totalPage = (total-1)/page.getPageSize() + 1;
        page.setTotalPage(totalPage);
        long pageIndex =(page.getPageNo()-1)* page.getPageSize();

//       为了防注入风险
        sqlInfo.getJoin()
                .append(" ")
                .append("LIMIT")
                .append(" ")
                .append(sqlInfo.getPlaceholder());

        sqlInfo.getParams().add(pageIndex);

        sqlInfo.getJoin()
                .append(" , ")
                .append(sqlInfo.getPlaceholder());

        sqlInfo.getParams().add(page.getPageSize());


        sqlInfo.setSql(StringHelper.replaceBlank(sqlInfo.getJoin().toString()));

        List<HashMap> select = publicSqlMapper.select(sqlInfo.getMybatisMap());
        return select;
    }
    public List<HashMap> executeQueryPage(String db,TTPage page){
        try{
            DynamicDataSource.setDataSource(db);
            return executeQueryPage(page);
        }finally {
            DynamicDataSource.clearDataSource();
        }
    }

    public <T> List<T> executeQueryPage(TTPage page, Class<T> cls){
        List<HashMap> hashMaps = executeQueryPage(page);

        List<T> modelList = JSON.parseArray(JSON.toJSONString(hashMaps),cls);
        return modelList;
    }
    public <T> List<T> executeQueryPage(String db,TTPage page, Class<T> cls){
        try{
            DynamicDataSource.setDataSource(db);
            return executeQueryPage(page,cls);
        }finally {
            DynamicDataSource.clearDataSource();
        }
    }

    public int executeUpdate()  {
        end();
        SqlInfo sqlInfo = source.getSqlInfo();
        int result = 0;
        if (sqlInfo.getSql().startsWith("INSERT")){

            Map map = sqlInfo.getMybatisMap();

           //如果没有主键值 那么就是使用数据库自增 那么就要做回显
            if(primaryValue==null){
                map.put("map_keyId",null);

                Method method;
                try {
                    //反射修改 update方法上面的Option注解
                    method =TTSQLMapper.class.getMethod("insert",Map.class);
                    Options annotation = method.getAnnotation(Options.class);
                    String keyColumn = StringHelper.smallHumpToUnderline(ReflectionHelper.getPrimaryField(entityCls).getName());
                    InvocationHandler invocationHandler = Proxy.getInvocationHandler(annotation);
                    Field declaredField = invocationHandler.getClass().getDeclaredField("memberValues");
                    declaredField.setAccessible(true);
                    Map memberValues = (Map)declaredField.get(invocationHandler);
                    memberValues.put("keyColumn",keyColumn);
                    memberValues.put("keyProperty","map_keyId");
//                System.out.println(annotation.keyColumn());
//                System.out.println(annotation.keyProperty());

                }catch(Exception e){

                    e.printStackTrace();

                }

            }


            result = publicSqlMapper.insert(map);

            if(primaryValue==null){
                //主键回显
                ReflectionHelper.setValueToPrimaryField(entityObj,map.get("map_keyId"));
            }else {
                ReflectionHelper.setValueToPrimaryField(entityObj,primaryValue);
            }

        }
        else if (sqlInfo.getSql().startsWith("UPDATE")){

            result = publicSqlMapper.update(sqlInfo.getMybatisMap());
        }
        else if (sqlInfo.getSql().startsWith("DELETE")){
            result = publicSqlMapper.delete(sqlInfo.getMybatisMap());
        }
        return result;
    }
    public int executeUpdate(String db) {
        try{
            DynamicDataSource.setDataSource(db);
            return executeUpdate();
        }finally {
            DynamicDataSource.clearDataSource();
        }
    }



    //sql开始 (使用mybatis  占位符使用 #{ } )
    public static TTSQL start() {
        TTSQL ttsql = new TTSQL();
        ttsql.source.getSqlInfo().setJDBCSql(false);
        return ttsql;
    }
    //sql 开始(jdbc 形式的字符串  占位符使用 ?)
    public static TTSQL startJDBCSql() {
        TTSQL ttsql = new TTSQL();
        ttsql.source.getSqlInfo().setJDBCSql(true);
        return ttsql;
    }



    //sql结束
    public SqlInfo end() {

        SqlInfo sqlInfo = this.source.getSqlInfo();

        if (whereConditionsCount==0){
            String sql = sqlInfo.getJoin().toString().replace("WHERE", "");
            StringBuilder join = new StringBuilder(sql);
            sqlInfo.setJoin(join);
        }

        sqlInfo.setSql(StringHelper.replaceBlank(sqlInfo.getJoin().toString()));
        return sqlInfo;
    }


    //合并多个
    private TTSQL concat(String sqlKey, String... params) {
        this.source.getSqlInfo().getJoin().append(" ").append(sqlKey).append(" ");
        if (params != null && params.length > 0) {
            String[] var3 = params;
            int var4 = params.length;

            for(int var5 = 0; var5 < var4; ++var5) {
                String s = var3[var5];
                this.source.getSqlInfo().getJoin().append(s).append(" ");
            }
        }

        return this;
    }

    private Class entityCls;

    public static TTSQL select(Class cls) {
        TTSQL ttsql = start();
        ttsql.entityCls = cls;
        String classFieldStr = ReflectionHelper.getClassFieldStr(cls);

        ttsql.select(classFieldStr).from(cls).where();
        //获取cls的逻辑删除字段 有则加上 逻辑删除
        List logicFieldAndLogicAnnotationValue = ReflectionHelper.getLogicFieldAndLogicAnnotationValue(cls);

        //如果有逻辑删除字段
        if (logicFieldAndLogicAnnotationValue!=null){

            Field field = (Field)logicFieldAndLogicAnnotationValue.get(0);
            String logicDeleteValue = (String)logicFieldAndLogicAnnotationValue.get(1);
            String logicFieldName = StringHelper.smallHumpToUnderline(field.getName());

            ttsql.notEqual(logicFieldName,logicDeleteValue);
        }else {
            //如果没有逻辑删除字段
        }


        return ttsql;
    }
    public static TTSQL selectById(Class cls,Object idValue) {

        TTSQL ttsql = select(cls);
        String primaryField = StringHelper.smallHumpToUnderline(ReflectionHelper.getPrimaryField(cls).getName());

        return ttsql.andEqual(primaryField,idValue);

    }
    public static TTSQL selectByIds(Class cls,Object[] ids) {

        TTSQL ttsql = select(cls);
        String primaryField = StringHelper.smallHumpToUnderline(ReflectionHelper.getPrimaryField(cls).getName());
        if (ids.length==1){
            return ttsql.andEqual(primaryField,ids[0]);
        }else {
            return ttsql.andIn(primaryField,ids);
        }

    }
    public static TTSQL selectByIds(Class cls,String ids) {

        TTSQL ttsql = select(cls);
        String primaryField = StringHelper.smallHumpToUnderline(ReflectionHelper.getPrimaryField(cls).getName());

        if (!ids.contains(",")){
            return ttsql.andEqual(primaryField,ids);
        }else {
            return ttsql.andIn(primaryField,ids.split(","));
        }

    }

    public static <T> TTSQL delete(Class cls) {
        TTSQL ttsql = start();
        ttsql.entityCls = cls;
        String tableName = StringHelper.bigHumpToUnderline(cls.getSimpleName());
       return ttsql.deleteFrom(tableName);
    }

    //根据注解上的逻辑删除字段
    public static <T> TTSQL deleteById(Class cls,Object idValue) {
        TTSQL ttsql = start();
        ttsql.entityCls = cls;
        String tableName = StringHelper.bigHumpToUnderline(cls.getSimpleName());
        String primaryField = StringHelper.smallHumpToUnderline(ReflectionHelper.getPrimaryField(cls).getName());

        List logicFieldAndLogicAnnotationValue = ReflectionHelper.getLogicFieldAndLogicAnnotationValue(cls);
        //如果没有逻辑删除 那么就直接删除数据
        if (logicFieldAndLogicAnnotationValue==null){

            ttsql.deleteFrom(tableName)
                .where(primaryField,idValue);
        }
        //如果有逻辑删除字段 那么就变成update 逻辑删除字段
        else {

            Field field = (Field)logicFieldAndLogicAnnotationValue.get(0);
            String logicDeleteValue = (String)logicFieldAndLogicAnnotationValue.get(1);
            String logicFieldName = StringHelper.smallHumpToUnderline(field.getName());

            ttsql.update(tableName)
                    .set()
                    .equal(logicFieldName,logicDeleteValue)
                    .where(primaryField,idValue);
        }

        return ttsql;
    }
    //传入逻辑删除字段 和 删除的的值
    public static <T> TTSQL deleteByIdOnField(Class cls, Object idValue, TTSQLFunction<T,?> function, Object logicDeleteValue) {
        TTSQL ttsql = start();
        ttsql.entityCls = cls;
        String tableName = StringHelper.bigHumpToUnderline(cls.getSimpleName());
        String primaryField = StringHelper.smallHumpToUnderline(ReflectionHelper.getPrimaryField(cls).getName());
        if (function!=null){
            String fieldName = StringHelper.smallHumpToUnderline(ReflectionHelper.getFieldName(function));
            return ttsql.update(tableName)
                    .set(fieldName+" = "+logicDeleteValue.toString())
                    .where(primaryField,idValue);
        }
        else {
            return ttsql.deleteFrom(tableName);
        }
    }
    public static <T> TTSQL deleteByIds(Class cls,Object[] ids) {

        if (ids.length==1){
           return deleteById(cls,ids[0]);
        }else {
            TTSQL ttsql = start();
            ttsql.entityCls = cls;
            String tableName = StringHelper.bigHumpToUnderline(cls.getSimpleName());
            String primaryField = StringHelper.smallHumpToUnderline(ReflectionHelper.getPrimaryField(cls).getName());

            List logicFieldAndLogicAnnotationValue = ReflectionHelper.getLogicFieldAndLogicAnnotationValue(cls);
            //如果没有逻辑删除 那么就直接删除数据
            if (logicFieldAndLogicAnnotationValue==null){

                ttsql.deleteFrom(tableName)
                        .where()
                        .in(primaryField,ids);;
            }
            else {
                //如果有逻辑删除字段 那么就变成update 逻辑删除字段
                Field field = (Field)logicFieldAndLogicAnnotationValue.get(0);
                String logicDeleteValue = (String)logicFieldAndLogicAnnotationValue.get(1);
                String logicFieldName = StringHelper.smallHumpToUnderline(field.getName());

                ttsql.update(tableName)
                        .set()
                        .equal(logicFieldName,logicDeleteValue)
                        .where()
                        .in(primaryField,ids);;
            }
        }




        TTSQL ttsql = start();
        ttsql.entityCls = cls;
        String tableName = StringHelper.bigHumpToUnderline(cls.getSimpleName());
        String primaryField = StringHelper.smallHumpToUnderline(ReflectionHelper.getPrimaryField(cls).getName());

        String[] split=null;
        if (ids[0].toString().contains(",")){
            split = ids[0].toString().split(",");
        }

        List logicFieldAndLogicAnnotationValue = ReflectionHelper.getLogicFieldAndLogicAnnotationValue(cls);
        //如果没有逻辑删除 那么就直接删除数据
        if (logicFieldAndLogicAnnotationValue==null){

            ttsql.deleteFrom(tableName);
            if (split==null){
                ttsql.where().in(primaryField,ids);
            }else {
                ttsql.where().in(primaryField,split);
            }

        }
        //如果有逻辑删除字段 那么就变成update 逻辑删除字段
        else {

            Field field = (Field)logicFieldAndLogicAnnotationValue.get(0);
            String logicDeleteValue = (String)logicFieldAndLogicAnnotationValue.get(1);
            String logicFieldName = StringHelper.smallHumpToUnderline(field.getName());

            ttsql.update(tableName)
                    .set()
                    .equal(logicFieldName,logicDeleteValue);
            if (split==null){
                ttsql.where().in(primaryField,ids);
            }else {
                ttsql.where().in(primaryField,split);
            }
        }

        return ttsql;
    }
    public static <T> TTSQL deleteByIds(Class cls,String ids) {

        if (!ids.contains(",")){
            return deleteById(cls,ids);
        } else {
            TTSQL ttsql = start();
            ttsql.entityCls = cls;
            String tableName = StringHelper.bigHumpToUnderline(cls.getSimpleName());
            String primaryField = StringHelper.smallHumpToUnderline(ReflectionHelper.getPrimaryField(cls).getName());

            List logicFieldAndLogicAnnotationValue = ReflectionHelper.getLogicFieldAndLogicAnnotationValue(cls);
            //如果没有逻辑删除 那么就直接删除数据
            if (logicFieldAndLogicAnnotationValue==null){

                return ttsql.deleteFrom(tableName)
                        .where()
                        .in(primaryField,ids.split(","));
            }
            else {
                //如果有逻辑删除字段 那么就变成update 逻辑删除字段
                Field field = (Field)logicFieldAndLogicAnnotationValue.get(0);
                String logicDeleteValue = (String)logicFieldAndLogicAnnotationValue.get(0);
                String logicFieldName = StringHelper.smallHumpToUnderline(field.getName());

                return ttsql.update(tableName)
                        .set()
                        .equal(logicFieldName,logicDeleteValue)
                        .where()
                        .in(primaryField,ids.split(","));
            }
        }


    }
    public static <T> TTSQL deleteByIdsOnField(Class cls,Object[] ids,TTSQLFunction<T,?> function,Object logicDeleteValue) {

        if (function==null){
           return deleteByIds(cls,ids);
        }
        else {
            if (ids.length==1){
                return deleteByIdOnField(cls,ids[0],function,logicDeleteValue);
            }else {
                TTSQL ttsql = start();
                ttsql.entityCls = cls;
                String tableName = StringHelper.bigHumpToUnderline(cls.getSimpleName());
                String primaryField = StringHelper.smallHumpToUnderline(ReflectionHelper.getPrimaryField(cls).getName());
                String fieldName = StringHelper.smallHumpToUnderline(ReflectionHelper.getFieldName(function));
                return ttsql.update(tableName)
                        .set(fieldName+" = "+logicDeleteValue.toString())
                        .where().in(primaryField,ids);
            }

        }


    }
    public static <T> TTSQL deleteByIdsOnField(Class cls,String ids,TTSQLFunction<T,?> function,Object logicDeleteValue) {

        if (function==null){
            return deleteByIds(cls,ids);
        }
        else {

            if (!ids.contains(",")){
               return deleteByIdOnField(cls,ids,function,logicDeleteValue);
            }
            else {

                TTSQL ttsql = start();
                ttsql.entityCls = cls;
                String tableName = StringHelper.bigHumpToUnderline(cls.getSimpleName());
                String primaryField = StringHelper.smallHumpToUnderline(ReflectionHelper.getPrimaryField(cls).getName());
                String fieldName = StringHelper.smallHumpToUnderline(ReflectionHelper.getFieldName(function));
                return ttsql.update(tableName)
                        .set(fieldName+" = "+logicDeleteValue.toString())
                        .where()
                        .in(primaryField,ids.split(","));

            }
        }


    }


    private Object entityObj;
    private Field primaryField;
    private Object primaryValue = null;
    public static TTSQL insertInto(Object obj) {
        TTSQL ttsql = start();
        ttsql.entityCls =obj.getClass();
        ttsql.entityObj = obj;
        String tableName = StringHelper.bigHumpToUnderline(ttsql.entityCls.getSimpleName());


        //如果传入的obj有主键id 那么就使用传入的
        //没有就使用数据库设置的
        List ignorePrimaryField = ReflectionHelper.getIgnorePrimaryField(obj,true);

//        List fieldsList = ignorePrimaryField.get(0);
        List valuesList = (List) ignorePrimaryField.get(1);
        String fieldstr = (String)ignorePrimaryField.get(2);
        ttsql.primaryField = (Field)ignorePrimaryField.get(3);


        TTSnowFlakePrimaryKey snowFlakePrimaryKey = ttsql.primaryField.getAnnotation(TTSnowFlakePrimaryKey.class);
        TTRedisPrimaryKey redisPrimaryKey = ttsql.primaryField.getAnnotation(TTRedisPrimaryKey.class);

        try {
            ttsql.primaryValue = ttsql.primaryField.get(obj);
        }
        catch (Exception e){
            e.printStackTrace();
        }

        //如果使用了TTSnowFlakePrimaryKey注解 并且 主键没有值
        //那么使用雪花算法
        if (snowFlakePrimaryKey!=null && ttsql.primaryValue==null){
            fieldstr = StringHelper.smallHumpToUnderline(ttsql.primaryField.getName())+","+fieldstr;
            //使用雪花算法
            ttsql.primaryValue = ttsql.idWorker.nextId()+"";
            valuesList.add(0, ttsql.primaryValue);
        }
        //如果使用了TTRedisPrimaryKey注解 并且 主键没有值
        //那么使用redis id 算法(也是类似雪花自增)
        if (redisPrimaryKey!=null && ttsql.primaryValue==null){
            fieldstr = StringHelper.smallHumpToUnderline(ttsql.primaryField.getName())+","+fieldstr;
            ttsql.primaryValue = redisIdWorker.generateNextId();
            valuesList.add(0, ttsql.primaryValue);
        }

        return ttsql.insertInto(tableName+"("+fieldstr+")")
                .values(valuesList.toArray());

    }

    public static TTSQL update(Class cls) {
        TTSQL ttsql = start();
        ttsql.entityCls =cls;
        String tableName = StringHelper.bigHumpToUnderline(ttsql.entityCls.getSimpleName());
        return ttsql.update(tableName);
    }
    public static TTSQL updateById(Object obj) {
        TTSQL ttsql = start();
        ttsql.entityCls =obj.getClass();
        ttsql.entityObj = obj;
        String tableName = StringHelper.bigHumpToUnderline(ttsql.entityCls.getSimpleName());

        List ignorePrimaryField = ReflectionHelper.getIgnorePrimaryField(obj,false);
        List fieldsList = (List) ignorePrimaryField.get(0);
        List valuesList = (List) ignorePrimaryField.get(1);

        ttsql.update(tableName)
                .set();
        //拼接 ,
        for (int i = 0; i < fieldsList.size(); i++) {
            ttsql.equal(fieldsList.get(i).toString(),valuesList.get(i));
            if (i+1!=fieldsList.size()){
                ttsql.text(",");
            }
        }

        String primaryFieldName = StringHelper.smallHumpToUnderline(ReflectionHelper.getPrimaryField(ttsql.entityCls).getName());
        Field primaryField = (Field) ignorePrimaryField.get(3);
        Object primaryFieldValue =null;
        try {
            primaryFieldValue = primaryField.get(obj);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        ttsql.where(primaryFieldName,primaryFieldValue);

        List logicFieldAndLogicAnnotationValue = ReflectionHelper.getLogicFieldAndLogicAnnotationValue(ttsql.entityCls);

        if (logicFieldAndLogicAnnotationValue!=null){
            Field field = (Field)logicFieldAndLogicAnnotationValue.get(0);
            String logicDeleteValue = (String)logicFieldAndLogicAnnotationValue.get(1);
            String logicFieldName = StringHelper.smallHumpToUnderline(field.getName());

            ttsql.andNotEqual(logicFieldName,logicDeleteValue);

        }


        return ttsql;
    }
    public static TTSQL updateNoPrimary(Object obj) {
        TTSQL ttsql = start();
        ttsql.entityCls =obj.getClass();
        ttsql.entityObj = obj;
        String tableName = StringHelper.bigHumpToUnderline(ttsql.entityCls.getSimpleName());

        List ignorePrimaryField = ReflectionHelper.getField(obj,false);
        List fieldsList = (List) ignorePrimaryField.get(0);
        List valuesList = (List) ignorePrimaryField.get(1);

        ttsql.update(tableName)
                .set();
        for (int i = 0; i < fieldsList.size(); i++) {
            ttsql.equal(fieldsList.get(i).toString(),valuesList.get(i));
            if (i+1!=fieldsList.size()){
                ttsql.text(",");
            }
        }


        List logicFieldAndLogicAnnotationValue = ReflectionHelper.getLogicFieldAndLogicAnnotationValue(ttsql.entityCls);

        if (logicFieldAndLogicAnnotationValue!=null){
            Field field = (Field)logicFieldAndLogicAnnotationValue.get(0);
            String logicDeleteValue = (String)logicFieldAndLogicAnnotationValue.get(1);
            String logicFieldName = StringHelper.smallHumpToUnderline(field.getName());

            ttsql.where().notEqual(logicFieldName,logicDeleteValue);

        }


        return ttsql;
    }
    public static TTSQL updateNoPrimaryNoLogic(Object obj) {
        TTSQL ttsql = start();
        ttsql.entityCls =obj.getClass();
        ttsql.entityObj = obj;
        String tableName = StringHelper.bigHumpToUnderline(ttsql.entityCls.getSimpleName());

        List ignorePrimaryField = ReflectionHelper.getField(obj,false);
        List fieldsList = (List) ignorePrimaryField.get(0);
        List valuesList = (List) ignorePrimaryField.get(1);

        ttsql.update(tableName)
                .set();
        for (int i = 0; i < fieldsList.size(); i++) {
            ttsql.equal(fieldsList.get(i).toString(),valuesList.get(i));
            if (i+1!=fieldsList.size()){
                ttsql.text(",");
            }
        }
//        ttsql.where();

        return ttsql;
    }



    public TTSQL insertInto(String text) {
        return this.concat("INSERT INTO", text);
    }
    public TTSQL deleteFrom(String text) {
        return this.concat("DELETE FROM", text);
    }
    public TTSQL update(String text) {
        return this.concat("UPDATE", text);
    }
    public TTSQL values(String text) {
        return this.concat("VALUES", text);
    }
    private TTSQL values(Object[] values)  {

        this.concat("VALUES", "");
        return this.doValue(values);

    }

    public TTSQL select(String text) {
        return this.concat("SELECT", text);
    }
    public TTSQL select() {
        return this.concat("SELECT", "*");
    }
    public static TTSQL selectCount() {
       TTSQL ttsql = start();
        return ttsql.concat("SELECT", "COUNT(*)");
    }

    public TTSQL from(String text) {
        return this.concat("FROM", text);
    }

    public TTSQL from(Class cls) {
        return this.from(StringHelper.bigHumpToUnderline(cls.getSimpleName()));
    }


    public TTSQL where() {
        return this.concat("WHERE", null);
    }



    public TTSQL where(String text) {
        return this.concat("WHERE", text);
    }


    public TTSQL where(String text,Object value) {
        return this.doNormal(" WHERE ", text, value, " = "+this.source.getSqlInfo().getPlaceholder()+" ", true);
    }

    public TTSQL where(String text,Object... value) {
        this.concat("WHERE", text);
        return this.param(value);
    }

    public <T> TTSQL where(TTSQLFunction<T,?> function,Object value) {
        String fieldName = StringHelper.smallHumpToUnderline(ReflectionHelper.getFieldName(function));
        return this.where(fieldName,value);
    }

    public TTSQL and(String text) {
        return this.concat("AND", text);
    }
    public TTSQL and() {
        return this.concat("AND", null);
    }

    public TTSQL or(String text) {
        return this.concat("OR", text);
    }
    public TTSQL or(){
        return this.concat("OR", null);
    }

    public TTSQL as(String text) {
        return this.concat("AS", text);
    }
    public TTSQL as(){
        return this.concat("AS", null);
    }

    public TTSQL set(String text) {
        return this.concat("SET", text);
    }
    public TTSQL set(){
        return this.concat("SET", null);
    }

    public TTSQL innerJoin(String text) {
        return this.concat("INNER JOIN", text);
    }

    public TTSQL leftJoin(String text) {
        return this.concat("LEFT JOIN", text);
    }

    public TTSQL rightJoin(String text) {
        return this.concat("RIGHT JOIN", text);
    }

    public TTSQL fullJoin(String text) {
        return this.concat("FULL JOIN", text);
    }

    public TTSQL on(String text) {
        return this.concat("ON", text);
    }

    public TTSQL orderBy(String text) {
        return this.concat("ORDER BY", text);
    }
    public TTSQL orderBy(String text,boolean match) {
        if (match){
            concat("ORDER BY", text);
        }
        return this;
    }
    public TTSQL orderByAsc(String text) {
       return orderByAsc(text,true);
    }
    public TTSQL orderByAsc(String text,boolean match) {
        if (match){
            concat("ORDER BY", text);
            concat("ASC");
        }
        return this;
    }
    public TTSQL orderByDesc(String text) {
        return orderByDesc(text,true);
    }
    public TTSQL orderByDesc(String text,boolean match) {
        if (match){
            concat("ORDER BY", text);
            concat("DESC");
        }
        return this;
    }
    public <T> TTSQL orderBy(TTSQLFunction<T,?> function) {
        return orderBy(function,true);
    }
    public <T> TTSQL orderBy(TTSQLFunction<T,?> function,boolean match) {
        if (match){
            String field = StringHelper.smallHumpToUnderline(ReflectionHelper.getFieldName(function));
            return this.concat("ORDER BY", field);
        }
        return this;
    }
    public <T> TTSQL orderByAsc(TTSQLFunction<T,?> function) {
        return orderByAsc(function,true);
    }
    public <T> TTSQL orderByAsc(TTSQLFunction<T,?> function,boolean match) {
        if (match){
            String field = StringHelper.smallHumpToUnderline(ReflectionHelper.getFieldName(function));
            concat("ORDER BY", field);
            concat("ASC");
        }
        return this;
    }
    public <T> TTSQL orderByDesc(TTSQLFunction<T,?> function) {
        return orderByDesc(function,true);
    }
    public <T> TTSQL orderByDesc(TTSQLFunction<T,?> function,boolean match) {
        if (match){
            String field = StringHelper.smallHumpToUnderline(ReflectionHelper.getFieldName(function));
            concat("ORDER BY", field);
            concat("DESC");
        }
        return this;
    }

    public TTSQL groupBy(String text) {
        return this.concat("GROUP BY", text);
    }
    public <T> TTSQL groupBy(TTSQLFunction<T,?> function) {
        String field = StringHelper.smallHumpToUnderline(ReflectionHelper.getFieldName(function));
        return this.concat("ORDER BY", field);
    }


    public TTSQL having(String text) {
        return this.concat("HAVING", text);
    }

    public TTSQL limit(String text) {
        return this.concat("LIMIT", text);
    }
    public TTSQL limit(int text) {
        String field = String.valueOf(text);
        return this.concat("LIMIT", field);
    }

    public TTSQL offset(String text) {
        return this.concat("OFFSET", text);
    }

    public TTSQL asc() {
        return this.concat("ASC");
    }

    public TTSQL desc() {
        return this.concat("DESC");
    }

    public TTSQL union() {
        return this.concat("UNION");
    }

    public TTSQL unionAll() {
        return this.concat("UNION ALL");
    }

    public TTSQL text(String text, Object... values) {
        this.source.getSqlInfo().getJoin().append(text);
        this.appendParams(values, 1);
        return this;
    }

    public TTSQL text(boolean match, String text, Object... values) {
        return match ? this.text(text, values) : this;
    }

    private TTSQL appendParams(Object value, int objType) {
        Object[] values = CollectionHelper.toArray(value, objType);
        if (CollectionHelper.isNotEmpty(values)) {
            Collections.addAll(this.source.getSqlInfo().getParams(), values);
        }

        return this;
    }

    public TTSQL param(Object... values) {
        return this.appendParams(values, 1);
    }

    public TTSQL param(Collection<?> values) {
        return this.appendParams(values, 2);
    }

    public TTSQL doAnything(ICustomAction action) {
        SqlInfo sqlInfo = this.source.getSqlInfo();
        action.execute(sqlInfo.getJoin(), sqlInfo.getParams());
        return this;
    }

    public TTSQL doAnything(boolean match, ICustomAction action) {
        return match ? this.doAnything(action) : this;
    }

    //是否需要去掉 ( AND OR WHERE  )
    private Boolean isRemoveAfterWhereIsWord(String prefix){
        StringBuilder sb = this.source.getSqlInfo().getJoin();
        boolean curSQL_is_whereSufffix = sb.substring(sb.length() - 8).contains("WHERE");

        //说明是 有链接词进来AND OR等包括 WHERE 进来
        if (prefix.length()>2){
            //如果当前 sql 以Where结尾
            //去掉and 或者 or
            if(curSQL_is_whereSufffix){
                return true;
            }
        }
        return false;
    }

    //where 后的条件 数量 拼接一次 就加1
    private int whereConditionsCount=0;

    private TTSQL doNormal(String prefix, String field, Object value, String suffix, boolean match) {
        if (match) {
            if (isRemoveAfterWhereIsWord(prefix)){
                prefix = " ";
            }

            SqlInfoBuilder.newInstace(this.source.setPrefix(prefix)).buildNormalSql(field, value, suffix);
            this.source.resetPrefix();
            whereConditionsCount++;
        }
        return this;
    }
    private TTSQL doLike(String prefix, String field, Object value, boolean match, boolean positive) {

        if (match) {
            if (isRemoveAfterWhereIsWord(prefix)){
                prefix = " ";
            }
            String suffix = positive ? " LIKE " : " NOT LIKE ";
            SqlInfoBuilder.newInstace(this.source.setPrefix(prefix).setSuffix(suffix)).buildLikeSql(field, value);
            this.source.resetPrefix();
            whereConditionsCount++;
        }
        return this;
    }
    private TTSQL doLikePattern(String prefix, String field, String pattern, boolean match, boolean positive) {
        if (match) {
            if (isRemoveAfterWhereIsWord(prefix)){
                prefix = " ";
            }
            String suffix = positive ? " LIKE " : " NOT LIKE ";
            SqlInfoBuilder.newInstace(this.source.setPrefix(prefix).setSuffix(suffix)).buildLikePatternSql(field, pattern);
            this.source.resetPrefix();
            whereConditionsCount++;
        }
        return this;
    }
    private TTSQL doBetween(String prefix, String field, Object startValue, Object endValue, boolean match) {
        if (match) {
            if (isRemoveAfterWhereIsWord(prefix)){
                prefix = " ";
            }
            SqlInfoBuilder.newInstace(this.source.setPrefix(prefix)).buildBetweenSql(field, startValue, endValue);
            this.source.resetPrefix();
            whereConditionsCount++;
        }
        return this;
    }
    private TTSQL doInByType(String prefix, String field, Object value, boolean match, int objType, boolean positive) {
        if (match) {
            if (isRemoveAfterWhereIsWord(prefix)){
                prefix = " ";
            }
            this.source.setPrefix(prefix).setSuffix(positive ? " IN " : " NOT IN ");
            switch(objType) {
                case 1:
                    SqlInfoBuilder.newInstace(this.source).buildInSql(field, (Object[])((Object[])value));
                    break;
                case 2:
                    JavaSqlInfoBuilder.newInstace(this.source).buildInSqlByCollection(field, (Collection)value);
                    break;
                default:
                    throw new NotCollectionOrArrayException("in查询的值不是有效的集合或数组!");
            }
            this.source.resetPrefix();
            whereConditionsCount++;
        }

        return this;
    }
    private TTSQL doIn(String prefix, String field, Object[] values, boolean match, boolean positive) {
        return this.doInByType(prefix, field, values, match, 1, positive);
    }
    private TTSQL doIn(String prefix, String field, Collection<?> values, boolean match, boolean positive) {
        return this.doInByType(prefix, field, values, match, 2, positive);
    }
    private TTSQL doIsNull(String prefix, String field, boolean match, boolean positive) {
        if (match) {
            if (isRemoveAfterWhereIsWord(prefix)){
                prefix = " ";
            }
            this.source = this.source.setPrefix(prefix).setSuffix(positive ? " IS NULL " : " IS NOT NULL ");
            SqlInfoBuilder.newInstace(this.source).buildIsNullSql(field);
            this.source.resetPrefix();
            whereConditionsCount++;
        }
        return this;
    }

    private TTSQL doValue(Object value) {
        SqlInfoBuilder.newInstace(this.source).buildValueSql((Object[])((Object[])value));
        return this;
    }


//--------------生成    , xxx = xxxx
    public TTSQL dotEqual(String field, Object value) {
        return this.text(",").equal(field,value);
    }
    public TTSQL dotEqual(String field, Object value, boolean match) {
        return this.text(",").equal(field,value,match);
    }
    public <T> TTSQL dotEqual(TTSQLFunction<T,?> function,Object value) {
        return this.text(",").equal(function,value);
    }
    public <T> TTSQL dotEqual(TTSQLFunction<T,?> function,Object value, boolean match) {
        return this.text(",").equal(function,value,match);
    }



    public TTSQL equal(String field, Object value) {
        return this.doNormal(" ", field, value, " = "+this.source.getSqlInfo().getPlaceholder()+" ", true);
    }
    public TTSQL equal(String field, Object value, boolean match) {
        return this.doNormal(" ", field, value, " = "+this.source.getSqlInfo().getPlaceholder()+" ", match);
    }
    public <T> TTSQL equal(TTSQLFunction<T,?> function,Object value) {
        String field = StringHelper.smallHumpToUnderline(ReflectionHelper.getFieldName(function));
        return this.doNormal(" ", field, value, " = "+this.source.getSqlInfo().getPlaceholder()+" ", true);
    }
    public <T> TTSQL equal(TTSQLFunction<T,?> function,Object value, boolean match) {
        String field = StringHelper.smallHumpToUnderline(ReflectionHelper.getFieldName(function));
        return this.doNormal(" ", field, value, " = "+this.source.getSqlInfo().getPlaceholder()+" ", match);
    }



    public TTSQL andEqual(String field, Object value) {
        return this.doNormal(" AND ", field, value, " = "+this.source.getSqlInfo().getPlaceholder()+" ", true);
    }
    public TTSQL andEqual(String field, Object value, boolean match) {
        return this.doNormal(" AND ", field, value, " = "+this.source.getSqlInfo().getPlaceholder()+" ", match);
    }
    public <T> TTSQL andEqual(TTSQLFunction<T,?> function,Object value) {
        String field = StringHelper.smallHumpToUnderline(ReflectionHelper.getFieldName(function));
        return this.doNormal(" AND ", field, value, " = "+this.source.getSqlInfo().getPlaceholder()+" ", true);
    }
    public <T> TTSQL andEqual(TTSQLFunction<T,?> function,Object value, boolean match) {
        String field = StringHelper.smallHumpToUnderline(ReflectionHelper.getFieldName(function));
        return this.doNormal(" AND ", field, value, " = "+this.source.getSqlInfo().getPlaceholder()+" ", match);
    }
    public TTSQL orEqual(String field, Object value) {
        return this.doNormal(" OR ", field, value, " = "+this.source.getSqlInfo().getPlaceholder()+" ", true);
    }
    public TTSQL orEqual(String field, Object value, boolean match) {
        return this.doNormal(" OR ", field, value, " = "+this.source.getSqlInfo().getPlaceholder()+" ", match);
    }
    public <T> TTSQL orEqual(TTSQLFunction<T,?> function,Object value) {
        String field = StringHelper.smallHumpToUnderline(ReflectionHelper.getFieldName(function));
        return this.doNormal(" OR ", field, value, " = "+this.source.getSqlInfo().getPlaceholder()+" ", true);
    }
    public <T> TTSQL orEqual(TTSQLFunction<T,?> function,Object value, boolean match) {
        String field = StringHelper.smallHumpToUnderline(ReflectionHelper.getFieldName(function));
        return this.doNormal(" OR ", field, value, " = "+this.source.getSqlInfo().getPlaceholder()+" ", match);
    }
    public TTSQL notEqual(String field, Object value) {
        return this.doNormal(" ", field, value, " <> "+this.source.getSqlInfo().getPlaceholder()+" ", true);
    }
    public TTSQL notEqual(String field, Object value, boolean match) {
        return this.doNormal(" ", field, value, " <> "+this.source.getSqlInfo().getPlaceholder()+" ", match);
    }
    public <T> TTSQL notEqual(TTSQLFunction<T,?> function,Object value) {
        String field = StringHelper.smallHumpToUnderline(ReflectionHelper.getFieldName(function));
        return this.doNormal(" ", field, value, " <> "+this.source.getSqlInfo().getPlaceholder()+" ", true);
    }
    public <T> TTSQL notEqual(TTSQLFunction<T,?> function,Object value, boolean match) {
        String field = StringHelper.smallHumpToUnderline(ReflectionHelper.getFieldName(function));
        return this.doNormal(" ", field, value, " <> "+this.source.getSqlInfo().getPlaceholder()+" ", match);
    }
    public TTSQL andNotEqual(String field, Object value) {
        return this.doNormal(" AND ", field, value, " <> "+this.source.getSqlInfo().getPlaceholder()+" ", true);
    }
    public TTSQL andNotEqual(String field, Object value, boolean match) {
        return this.doNormal(" AND ", field, value, " <> "+this.source.getSqlInfo().getPlaceholder()+" ", match);
    }
    public <T> TTSQL andNotEqual(TTSQLFunction<T,?> function,Object value) {
        String field = StringHelper.smallHumpToUnderline(ReflectionHelper.getFieldName(function));
        return this.doNormal(" AND ", field, value, " <> "+this.source.getSqlInfo().getPlaceholder()+" ", true);
    }
    public <T> TTSQL andNotEqual(TTSQLFunction<T,?> function,Object value, boolean match) {
        String field = StringHelper.smallHumpToUnderline(ReflectionHelper.getFieldName(function));
        return this.doNormal(" AND ", field, value, " <> "+this.source.getSqlInfo().getPlaceholder()+" ", match);
    }
    public TTSQL orNotEqual(String field, Object value) {
        return this.doNormal(" OR ", field, value, " <> "+this.source.getSqlInfo().getPlaceholder()+" ", true);
    }
    public TTSQL orNotEqual(String field, Object value, boolean match) {
        return this.doNormal(" OR ", field, value, " <> "+this.source.getSqlInfo().getPlaceholder()+" ", match);
    }
    public <T> TTSQL orNotEqual(TTSQLFunction<T,?> function,Object value) {
        String field = StringHelper.smallHumpToUnderline(ReflectionHelper.getFieldName(function));
        return this.doNormal(" OR ", field, value, " <> "+this.source.getSqlInfo().getPlaceholder()+" ", true);
    }
    public <T> TTSQL orNotEqual(TTSQLFunction<T,?> function,Object value, boolean match) {
        String field = StringHelper.smallHumpToUnderline(ReflectionHelper.getFieldName(function));
        return this.doNormal(" OR ", field, value, " <> "+this.source.getSqlInfo().getPlaceholder()+" ", match);
    }
    public TTSQL moreThan(String field, Object value) {
        return this.doNormal(" ", field, value, " > "+this.source.getSqlInfo().getPlaceholder()+" ", true);
    }
    public TTSQL moreThan(String field, Object value, boolean match) {
        return this.doNormal(" ", field, value, " > "+this.source.getSqlInfo().getPlaceholder()+" ", match);
    }
    public <T> TTSQL moreThan(TTSQLFunction<T,?> function,Object value) {
        String field = StringHelper.smallHumpToUnderline(ReflectionHelper.getFieldName(function));
        return this.doNormal(" ", field, value, " > "+this.source.getSqlInfo().getPlaceholder()+" ", true);
    }
    public <T> TTSQL moreThan(TTSQLFunction<T,?> function,Object value, boolean match) {
        String field = StringHelper.smallHumpToUnderline(ReflectionHelper.getFieldName(function));
        return this.doNormal(" ", field, value, " > "+this.source.getSqlInfo().getPlaceholder()+" ", match);
    }
    public TTSQL andMoreThan(String field, Object value) {
        return this.doNormal(" AND ", field, value, " > "+this.source.getSqlInfo().getPlaceholder()+" ", true);
    }
    public TTSQL andMoreThan(String field, Object value, boolean match) {
        return this.doNormal(" AND ", field, value, " > "+this.source.getSqlInfo().getPlaceholder()+" ", match);
    }
    public <T> TTSQL andMoreThan(TTSQLFunction<T,?> function,Object value) {
        String field = StringHelper.smallHumpToUnderline(ReflectionHelper.getFieldName(function));
        return this.doNormal(" AND ", field, value, " > "+this.source.getSqlInfo().getPlaceholder()+" ", true);
    }
    public <T> TTSQL andMoreThan(TTSQLFunction<T,?> function,Object value, boolean match) {
        String field = StringHelper.smallHumpToUnderline(ReflectionHelper.getFieldName(function));
        return this.doNormal(" AND ", field, value, " > "+this.source.getSqlInfo().getPlaceholder()+" ", match);
    }
    public TTSQL orMoreThan(String field, Object value) {
        return this.doNormal(" OR ", field, value, " > "+this.source.getSqlInfo().getPlaceholder()+" ", true);
    }
    public TTSQL orMoreThan(String field, Object value, boolean match) {
        return this.doNormal(" OR ", field, value, " > "+this.source.getSqlInfo().getPlaceholder()+" ", match);
    }
    public <T> TTSQL orMoreThan(TTSQLFunction<T,?> function,Object value) {
        String field = StringHelper.smallHumpToUnderline(ReflectionHelper.getFieldName(function));
        return this.doNormal(" OR ", field, value, " > "+this.source.getSqlInfo().getPlaceholder()+" ", true);
    }
    public <T> TTSQL orMoreThan(TTSQLFunction<T,?> function,Object value, boolean match) {
        String field = StringHelper.smallHumpToUnderline(ReflectionHelper.getFieldName(function));
        return this.doNormal(" OR ", field, value, " > "+this.source.getSqlInfo().getPlaceholder()+" ", match);
    }
    public TTSQL lessThan(String field, Object value) {
        return this.doNormal(" ", field, value, " < "+this.source.getSqlInfo().getPlaceholder()+" ", true);
    }
    public TTSQL lessThan(String field, Object value, boolean match) {
        return this.doNormal(" ", field, value, " < "+this.source.getSqlInfo().getPlaceholder()+" ", match);
    }
    public <T> TTSQL lessThan(TTSQLFunction<T,?> function,Object value) {
        String field = StringHelper.smallHumpToUnderline(ReflectionHelper.getFieldName(function));
        return this.doNormal(" ", field, value, " < "+this.source.getSqlInfo().getPlaceholder()+" ", true);
    }
    public <T> TTSQL lessThan(TTSQLFunction<T,?> function,Object value, boolean match) {
        String field = StringHelper.smallHumpToUnderline(ReflectionHelper.getFieldName(function));
        return this.doNormal(" ", field, value, " < "+this.source.getSqlInfo().getPlaceholder()+" ", match);
    }
    public TTSQL andLessThan(String field, Object value) {
        return this.doNormal(" AND ", field, value, " < "+this.source.getSqlInfo().getPlaceholder()+" ", true);
    }
    public TTSQL andLessThan(String field, Object value, boolean match) {
        return this.doNormal(" AND ", field, value, " < "+this.source.getSqlInfo().getPlaceholder()+" ", match);
    }
    public <T> TTSQL andLessThan(TTSQLFunction<T,?> function,Object value) {
        String field = StringHelper.smallHumpToUnderline(ReflectionHelper.getFieldName(function));
        return this.doNormal(" AND ", field, value, " < "+this.source.getSqlInfo().getPlaceholder()+" ", true);
    }
    public <T> TTSQL andLessThan(TTSQLFunction<T,?> function,Object value, boolean match) {
        String field = StringHelper.smallHumpToUnderline(ReflectionHelper.getFieldName(function));
        return this.doNormal(" AND ", field, value, " < "+this.source.getSqlInfo().getPlaceholder()+" ", match);
    }
    public TTSQL orLessThan(String field, Object value) {
        return this.doNormal(" OR ", field, value, " < "+this.source.getSqlInfo().getPlaceholder()+" ", true);
    }
    public TTSQL orLessThan(String field, Object value, boolean match) {
        return this.doNormal(" OR ", field, value, " < "+this.source.getSqlInfo().getPlaceholder()+" ", match);
    }
    public <T> TTSQL orLessThan(TTSQLFunction<T,?> function,Object value) {
        String field = StringHelper.smallHumpToUnderline(ReflectionHelper.getFieldName(function));
        return this.doNormal(" OR ", field, value, " < "+this.source.getSqlInfo().getPlaceholder()+" ", true);
    }
    public <T> TTSQL orLessThan(TTSQLFunction<T,?> function,Object value, boolean match) {
        String field = StringHelper.smallHumpToUnderline(ReflectionHelper.getFieldName(function));
        return this.doNormal(" OR ", field, value, " < "+this.source.getSqlInfo().getPlaceholder()+" ", match);
    }
    public TTSQL moreEqual(String field, Object value) {
        return this.doNormal(" ", field, value, " >= "+this.source.getSqlInfo().getPlaceholder()+" ", true);
    }
    public TTSQL moreEqual(String field, Object value, boolean match) {
        return this.doNormal(" ", field, value, " >= "+this.source.getSqlInfo().getPlaceholder()+" ", match);
    }
    public <T> TTSQL moreEqual(TTSQLFunction<T,?> function,Object value) {
        String field = StringHelper.smallHumpToUnderline(ReflectionHelper.getFieldName(function));
        return this.doNormal(" ", field, value, " >= "+this.source.getSqlInfo().getPlaceholder()+" ", true);
    }
    public <T> TTSQL moreEqual(TTSQLFunction<T,?> function,Object value, boolean match) {
        String field = StringHelper.smallHumpToUnderline(ReflectionHelper.getFieldName(function));
        return this.doNormal(" ", field, value, " >= "+this.source.getSqlInfo().getPlaceholder()+" ", match);
    }
    public TTSQL andMoreEqual(String field, Object value) {
        return this.doNormal(" AND ", field, value, " >= "+this.source.getSqlInfo().getPlaceholder()+" ", true);
    }
    public TTSQL andMoreEqual(String field, Object value, boolean match) {
        return this.doNormal(" AND ", field, value, " >= "+this.source.getSqlInfo().getPlaceholder()+" ", match);
    }
    public <T> TTSQL andMoreEqual(TTSQLFunction<T,?> function,Object value) {
        String field = StringHelper.smallHumpToUnderline(ReflectionHelper.getFieldName(function));
        return this.doNormal(" AND ", field, value, " >= "+this.source.getSqlInfo().getPlaceholder()+" ", true);
    }
    public <T> TTSQL andMoreEqual(TTSQLFunction<T,?> function,Object value, boolean match) {
        String field = StringHelper.smallHumpToUnderline(ReflectionHelper.getFieldName(function));
        return this.doNormal(" AND ", field, value, " >= "+this.source.getSqlInfo().getPlaceholder()+" ", match);
    }
    public TTSQL orMoreEqual(String field, Object value) {
        return this.doNormal(" OR ", field, value, " >= "+this.source.getSqlInfo().getPlaceholder()+" ", true);
    }
    public TTSQL orMoreEqual(String field, Object value, boolean match) {
        return this.doNormal(" OR ", field, value, " >= "+this.source.getSqlInfo().getPlaceholder()+" ", match);
    }
    public <T> TTSQL orMoreEqual(TTSQLFunction<T,?> function,Object value) {
        String field = StringHelper.smallHumpToUnderline(ReflectionHelper.getFieldName(function));
        return this.doNormal(" OR ", field, value, " >= "+this.source.getSqlInfo().getPlaceholder()+" ", true);
    }
    public <T> TTSQL orMoreEqual(TTSQLFunction<T,?> function,Object value, boolean match) {
        String field = StringHelper.smallHumpToUnderline(ReflectionHelper.getFieldName(function));
        return this.doNormal(" OR ", field, value, " >= "+this.source.getSqlInfo().getPlaceholder()+" ", match);
    }
    public TTSQL lessEqual(String field, Object value) {
        return this.doNormal(" ", field, value, " <= "+this.source.getSqlInfo().getPlaceholder()+" ", true);
    }
    public TTSQL lessEqual(String field, Object value, boolean match) {
        return this.doNormal(" ", field, value, " <= "+this.source.getSqlInfo().getPlaceholder()+" ", match);
    }
    public <T> TTSQL lessEqual(TTSQLFunction<T,?> function,Object value) {
        String field = StringHelper.smallHumpToUnderline(ReflectionHelper.getFieldName(function));
        return this.doNormal(" ", field, value, " <= "+this.source.getSqlInfo().getPlaceholder()+" ", true);
    }
    public <T> TTSQL lessEqual(TTSQLFunction<T,?> function,Object value, boolean match) {
        String field = StringHelper.smallHumpToUnderline(ReflectionHelper.getFieldName(function));
        return this.doNormal(" ", field, value, " <= "+this.source.getSqlInfo().getPlaceholder()+" ", match);
    }
    public TTSQL andLessEqual(String field, Object value) {
        return this.doNormal(" AND ", field, value, " <= "+this.source.getSqlInfo().getPlaceholder()+" ", true);
    }
    public TTSQL andLessEqual(String field, Object value, boolean match) {
        return this.doNormal(" AND ", field, value, " <= "+this.source.getSqlInfo().getPlaceholder()+" ", match);
    }
    public <T> TTSQL andLessEqual(TTSQLFunction<T,?> function,Object value) {
        String field = StringHelper.smallHumpToUnderline(ReflectionHelper.getFieldName(function));
        return this.doNormal(" AND ", field, value, " <= "+this.source.getSqlInfo().getPlaceholder()+" ", true);
    }
    public <T> TTSQL andLessEqual(TTSQLFunction<T,?> function,Object value, boolean match) {
        String field = StringHelper.smallHumpToUnderline(ReflectionHelper.getFieldName(function));
        return this.doNormal(" AND ", field, value, " <= "+this.source.getSqlInfo().getPlaceholder()+" ", match);
    }
    public TTSQL orLessEqual(String field, Object value) {
        return this.doNormal(" OR ", field, value, " <= "+this.source.getSqlInfo().getPlaceholder()+" ", true);
    }
    public TTSQL orLessEqual(String field, Object value, boolean match) {
        return this.doNormal(" OR ", field, value, " <= "+this.source.getSqlInfo().getPlaceholder()+" ", match);
    }
    public <T> TTSQL orLessEqual(TTSQLFunction<T,?> function,Object value) {
        String field = StringHelper.smallHumpToUnderline(ReflectionHelper.getFieldName(function));
        return this.doNormal(" OR ", field, value, " <= "+this.source.getSqlInfo().getPlaceholder()+" ", true);
    }
    public <T> TTSQL orLessEqual(TTSQLFunction<T,?> function,Object value, boolean match) {
        String field = StringHelper.smallHumpToUnderline(ReflectionHelper.getFieldName(function));
        return this.doNormal(" OR ", field, value, " <= "+this.source.getSqlInfo().getPlaceholder()+" ", match);
    }


    public TTSQL like(String field, Object value) {
        return this.doLike(" ", field, value, true, true);
    }
    public TTSQL like(String field, Object value, boolean match) {
        return this.doLike(" ", field, value, match, true);
    }
    public <T> TTSQL like(TTSQLFunction<T,?> function,Object value) {
        String field = StringHelper.smallHumpToUnderline(ReflectionHelper.getFieldName(function));
        return this.doLike(" ", field, value, true, true);
    }
    public <T> TTSQL like(TTSQLFunction<T,?> function,Object value, boolean match) {
        String field = StringHelper.smallHumpToUnderline(ReflectionHelper.getFieldName(function));
        return this.doLike(" ", field, value, match, true);
    }
    public TTSQL andLike(String field, Object value) {
        return this.doLike(" AND ", field, value, true, true);
    }
    public TTSQL andLike(String field, Object value, boolean match) {
        return this.doLike(" AND ", field, value, match, true);
    }
    public <T> TTSQL andLike(TTSQLFunction<T,?> function,Object value) {
        String field = StringHelper.smallHumpToUnderline(ReflectionHelper.getFieldName(function));
        return this.doLike(" AND ", field, value, true, true);
    }
    public <T> TTSQL andLike(TTSQLFunction<T,?> function,Object value, boolean match) {
        String field = StringHelper.smallHumpToUnderline(ReflectionHelper.getFieldName(function));
        return this.doLike(" AND ", field, value, match, true);
    }
    public TTSQL orLike(String field, Object value) {
        return this.doLike(" OR ", field, value, true, true);
    }
    public TTSQL orLike(String field, Object value, boolean match) {
        return this.doLike(" OR ", field, value, match, true);
    }
    public <T> TTSQL orLike(TTSQLFunction<T,?> function,Object value) {
        String field = StringHelper.smallHumpToUnderline(ReflectionHelper.getFieldName(function));
        return this.doLike(" OR ", field, value, true, true);
    }
    public <T> TTSQL orLike(TTSQLFunction<T,?> function,Object value, boolean match) {
        String field = StringHelper.smallHumpToUnderline(ReflectionHelper.getFieldName(function));
        return this.doLike(" OR ", field, value, match, true);
    }
    public TTSQL notLike(String field, Object value) {
        return this.doLike(" ", field, value, true, false);
    }
    public TTSQL notLike(String field, Object value, boolean match) {
        return this.doLike(" ", field, value, match, false);
    }
    public <T> TTSQL notLike(TTSQLFunction<T,?> function,Object value) {
        String field = StringHelper.smallHumpToUnderline(ReflectionHelper.getFieldName(function));
        return this.doLike(" ", field, value, true, false);
    }
    public <T> TTSQL notLike(TTSQLFunction<T,?> function,Object value, boolean match) {
        String field = StringHelper.smallHumpToUnderline(ReflectionHelper.getFieldName(function));
        return this.doLike(" ", field, value, true, false);
    }
    public TTSQL andNotLike(String field, Object value) {
        return this.doLike(" AND ", field, value, true, false);
    }
    public TTSQL andNotLike(String field, Object value, boolean match) {
        return this.doLike(" AND ", field, value, match, false);
    }
    public <T> TTSQL andNotLike(TTSQLFunction<T,?> function,Object value) {
        String field = StringHelper.smallHumpToUnderline(ReflectionHelper.getFieldName(function));
        return this.doLike(" AND ", field, value, true, false);
    }
    public <T> TTSQL andNotLike(TTSQLFunction<T,?> function,Object value, boolean match) {
        String field = StringHelper.smallHumpToUnderline(ReflectionHelper.getFieldName(function));
        return this.doLike(" AND ", field, value, match, false);
    }
    public TTSQL orNotLike(String field, Object value) {
        return this.doLike(" OR ", field, value, true, false);
    }
    public TTSQL orNotLike(String field, Object value, boolean match) {
        return this.doLike(" OR ", field, value, match, false);
    }
    public <T> TTSQL orNotLike(TTSQLFunction<T,?> function,Object value) {
        String field = StringHelper.smallHumpToUnderline(ReflectionHelper.getFieldName(function));
        return this.doLike(" OR ", field, value, true, false);
    }
    public <T> TTSQL orNotLike(TTSQLFunction<T,?> function,Object value, boolean match) {
        String field = StringHelper.smallHumpToUnderline(ReflectionHelper.getFieldName(function));
        return this.doLike(" OR ", field, value, match, false);
    }

    public TTSQL likePattern(String field, String pattern) {
        return this.doLikePattern(" ", field, pattern, true, true);
    }
    public TTSQL likePattern(String field, String pattern, boolean match) {
        return this.doLikePattern(" ", field, pattern, match, true);
    }
    public <T> TTSQL likePattern(TTSQLFunction<T,?> function, String pattern,Object value) {
        String field = StringHelper.smallHumpToUnderline(ReflectionHelper.getFieldName(function));
        return this.doLikePattern(" ", field, pattern, true, true);
    }
    public <T> TTSQL likePattern(TTSQLFunction<T,?> function, String pattern,Object value, boolean match) {
        String field = StringHelper.smallHumpToUnderline(ReflectionHelper.getFieldName(function));
        return this.doLikePattern(" ", field, pattern, match, true);
    }
    public TTSQL andLikePattern(String field, String pattern) {
        return this.doLikePattern(" AND ", field, pattern, true, true);
    }
    public TTSQL andLikePattern(String field, String pattern, boolean match) {
        return this.doLikePattern(" AND ", field, pattern, match, true);
    }
    public <T> TTSQL andLikePattern(TTSQLFunction<T,?> function, String pattern,Object value) {
        String field = StringHelper.smallHumpToUnderline(ReflectionHelper.getFieldName(function));
        return this.doLikePattern(" AND ", field, pattern, true, true);
    }
    public <T> TTSQL andLikePattern(TTSQLFunction<T,?> function, String pattern,Object value, boolean match) {
        String field = StringHelper.smallHumpToUnderline(ReflectionHelper.getFieldName(function));
        return this.doLikePattern(" AND ", field, pattern, match, true);
    }
    public TTSQL orLikePattern(String field, String pattern) {
        return this.doLikePattern(" OR ", field, pattern, true, true);
    }
    public TTSQL orLikePattern(String field, String pattern, boolean match) {
        return this.doLikePattern(" OR ", field, pattern, match, true);
    }
    public <T> TTSQL orLikePattern(TTSQLFunction<T,?> function, String pattern,Object value) {
        String field = StringHelper.smallHumpToUnderline(ReflectionHelper.getFieldName(function));
        return this.doLikePattern(" OR ", field, pattern, true, true);
    }
    public <T> TTSQL orLikePattern(TTSQLFunction<T,?> function, String pattern,Object value, boolean match) {
        String field = StringHelper.smallHumpToUnderline(ReflectionHelper.getFieldName(function));
        return this.doLikePattern(" OR ", field, pattern, match, true);
    }
    public TTSQL notLikePattern(String field, String pattern) {
        return this.doLikePattern(" ", field, pattern, true, false);
    }
    public TTSQL notLikePattern(String field, String pattern, boolean match) {
        return this.doLikePattern(" ", field, pattern, match, false);
    }
    public <T> TTSQL notLikePattern(TTSQLFunction<T,?> function, String pattern,Object value) {
        String field = StringHelper.smallHumpToUnderline(ReflectionHelper.getFieldName(function));
        return this.doLikePattern(" ", field, pattern, true, false);
    }
    public <T> TTSQL notLikePattern(TTSQLFunction<T,?> function, String pattern,Object value, boolean match) {
        String field = StringHelper.smallHumpToUnderline(ReflectionHelper.getFieldName(function));
        return this.doLikePattern(" ", field, pattern, match, false);
    }
    public TTSQL andNotLikePattern(String field, String pattern) {
        return this.doLikePattern(" AND ", field, pattern, true, false);
    }
    public TTSQL andNotLikePattern(String field, String pattern, boolean match) {
        return this.doLikePattern(" AND ", field, pattern, match, false);
    }
    public <T> TTSQL andNotLikePattern(TTSQLFunction<T,?> function, String pattern,Object value) {
        String field = StringHelper.smallHumpToUnderline(ReflectionHelper.getFieldName(function));
        return this.doLikePattern(" AND ", field, pattern, true, false);
    }
    public <T> TTSQL andNotLikePattern(TTSQLFunction<T,?> function, String pattern,Object value, boolean match) {
        String field = StringHelper.smallHumpToUnderline(ReflectionHelper.getFieldName(function));
        return this.doLikePattern(" AND ", field, pattern, match, false);
    }
    public TTSQL orNotLikePattern(String field, String pattern) {
        return this.doLikePattern(" OR ", field, pattern, true, false);
    }
    public TTSQL orNotLikePattern(String field, String pattern, boolean match) {
        return this.doLikePattern(" OR ", field, pattern, match, false);
    }
    public <T> TTSQL orNotLikePattern(TTSQLFunction<T,?> function, String pattern,Object value) {
        String field = StringHelper.smallHumpToUnderline(ReflectionHelper.getFieldName(function));
        return this.doLikePattern(" OR ", field, pattern, true, false);
    }
    public <T> TTSQL orNotLikePattern(TTSQLFunction<T,?> function, String pattern,Object value, boolean match) {
        String field = StringHelper.smallHumpToUnderline(ReflectionHelper.getFieldName(function));
        return this.doLikePattern(" OR ", field, pattern, match, false);
    }

    public TTSQL between(String field, Object startValue, Object endValue) {
        return this.doBetween(" ", field, startValue, endValue, true);
    }
    public TTSQL between(String field, Object startValue, Object endValue, boolean match) {
        return this.doBetween(" ", field, startValue, endValue, match);
    }
    public <T> TTSQL between(TTSQLFunction<T,?> function,Object startValue, Object endValue) {
        String field = StringHelper.smallHumpToUnderline(ReflectionHelper.getFieldName(function));
        return this.doBetween(" ", field, startValue, endValue, true);
    }
    public <T> TTSQL between(TTSQLFunction<T,?> function,Object startValue, Object endValue, boolean match) {
        String field = StringHelper.smallHumpToUnderline(ReflectionHelper.getFieldName(function));
        return this.doBetween(" ", field, startValue, endValue, match);
    }
    public TTSQL andBetween(String field, Object startValue, Object endValue) {
        return this.doBetween(" AND ", field, startValue, endValue, true);
    }
    public TTSQL andBetween(String field, Object startValue, Object endValue, boolean match) {
        return this.doBetween(" AND ", field, startValue, endValue, match);
    }
    public <T> TTSQL andBetween(TTSQLFunction<T,?> function,Object startValue, Object endValue) {
        String field = StringHelper.smallHumpToUnderline(ReflectionHelper.getFieldName(function));
        return this.doBetween(" AND ", field, startValue, endValue, true);
    }
    public <T> TTSQL andBetween(TTSQLFunction<T,?> function,Object startValue, Object endValue, boolean match) {
        String field = StringHelper.smallHumpToUnderline(ReflectionHelper.getFieldName(function));
        return this.doBetween(" AND ", field, startValue, endValue, match);
    }
    public TTSQL orBetween(String field, Object startValue, Object endValue) {
        return this.doBetween(" OR ", field, startValue, endValue, true);
    }
    public TTSQL orBetween(String field, Object startValue, Object endValue, boolean match) {
        return this.doBetween(" OR ", field, startValue, endValue, match);
    }
    public <T> TTSQL orBetween(TTSQLFunction<T,?> function,Object startValue, Object endValue) {
        String field = StringHelper.smallHumpToUnderline(ReflectionHelper.getFieldName(function));
        return this.doBetween(" OR ", field, startValue, endValue, true);
    }
    public <T> TTSQL orBetween(TTSQLFunction<T,?> function,Object startValue, Object endValue, boolean match) {
        String field = StringHelper.smallHumpToUnderline(ReflectionHelper.getFieldName(function));
        return this.doBetween(" OR ", field, startValue, endValue, match);
    }

    public TTSQL in(String field, Object[] values) {
        return this.doIn(" ", field, values, true, true);
    }
    public TTSQL in(String field, Object[] values, boolean match) {
        return this.doIn(" ", field, values, match, true);
    }
    public <T> TTSQL in(TTSQLFunction<T,?> function,Object[] values) {
        String field = StringHelper.smallHumpToUnderline(ReflectionHelper.getFieldName(function));
        return this.doIn(" ", field, values, true, true);
    }
    public <T> TTSQL in(TTSQLFunction<T,?> function,Object[] values, boolean match) {
        String field = StringHelper.smallHumpToUnderline(ReflectionHelper.getFieldName(function));
        return this.doIn(" ", field, values, match, true);
    }
    public TTSQL in(String field, Collection<?> values) {
        return this.doIn(" ", field, values, true, true);
    }
    public TTSQL in(String field, Collection<?> values, boolean match) {
        return this.doIn(" ", field, values, match, true);
    }
    public <T> TTSQL in(TTSQLFunction<T,?> function,Collection<?> values) {
        String field = StringHelper.smallHumpToUnderline(ReflectionHelper.getFieldName(function));
        return this.doIn(" ", field, values, true, true);
    }
    public <T> TTSQL in(TTSQLFunction<T,?> function,Collection<?> values, boolean match) {
        String field = StringHelper.smallHumpToUnderline(ReflectionHelper.getFieldName(function));
        return this.doIn(" ", field, values, match, true);
    }
    public TTSQL andIn(String field, Object[] values) {
        return this.doIn(" AND ", field, values, true, true);
    }
    public TTSQL andIn(String field, Object[] values, boolean match) {
        return this.doIn(" AND ", field, values, match, true);
    }
    public <T> TTSQL andIn(TTSQLFunction<T,?> function,Object[] values) {
        String field = StringHelper.smallHumpToUnderline(ReflectionHelper.getFieldName(function));
        return this.doIn(" AND ", field, values, true, true);
    }
    public <T> TTSQL andIn(TTSQLFunction<T,?> function,Object[] values, boolean match) {
        String field = StringHelper.smallHumpToUnderline(ReflectionHelper.getFieldName(function));
        return this.doIn(" AND ", field, values, match, true);
    }
    public TTSQL andIn(String field, Collection<?> values) {
        return this.doIn(" AND ", field, values, true, true);
    }
    public TTSQL andIn(String field, Collection<?> values, boolean match) {
        return this.doIn(" AND ", field, values, match, true);
    }
    public <T> TTSQL andIn(TTSQLFunction<T,?> function,Collection<?> values) {
        String field = StringHelper.smallHumpToUnderline(ReflectionHelper.getFieldName(function));
        return this.doIn(" AND ", field, values, true, true);
    }
    public <T> TTSQL andIn(TTSQLFunction<T,?> function,Collection<?> values, boolean match) {
        String field = StringHelper.smallHumpToUnderline(ReflectionHelper.getFieldName(function));
        return this.doIn(" AND ", field, values, match, true);
    }
    public TTSQL orIn(String field, Object[] values) {
        return this.doIn(" OR ", field, values, true, true);
    }
    public TTSQL orIn(String field, Object[] values, boolean match) {
        return this.doIn(" OR ", field, values, match, true);
    }
    public <T> TTSQL orIn(TTSQLFunction<T,?> function,Object[] values) {
        String field = StringHelper.smallHumpToUnderline(ReflectionHelper.getFieldName(function));
        return this.doIn(" OR ", field, values, true, true);
    }
    public <T> TTSQL orIn(TTSQLFunction<T,?> function,Object[] values, boolean match) {
        String field = StringHelper.smallHumpToUnderline(ReflectionHelper.getFieldName(function));
        return this.doIn(" OR ", field, values, match, true);
    }
    public TTSQL orIn(String field, Collection<?> values) {
        return this.doIn(" OR ", field, values, true, true);
    }
    public TTSQL orIn(String field, Collection<?> values, boolean match) {
        return this.doIn(" OR ", field, values, match, true);
    }
    public <T> TTSQL orIn(TTSQLFunction<T,?> function,Collection<?> values) {
        String field = StringHelper.smallHumpToUnderline(ReflectionHelper.getFieldName(function));
        return this.doIn(" OR ", field, values, true, true);
    }
    public <T> TTSQL orIn(TTSQLFunction<T,?> function,Collection<?> values, boolean match) {
        String field = StringHelper.smallHumpToUnderline(ReflectionHelper.getFieldName(function));
        return this.doIn(" OR ", field, values, match, true);
    }
    public TTSQL notIn(String field, Object[] values) {
        return this.doIn(" ", field, values, true, false);
    }
    public TTSQL notIn(String field, Object[] values, boolean match) {
        return this.doIn(" ", field, values, match, false);
    }
    public <T> TTSQL notIn(TTSQLFunction<T,?> function,Object[] values) {
        String field = StringHelper.smallHumpToUnderline(ReflectionHelper.getFieldName(function));
        return this.doIn(" ", field, values, true, false);
    }
    public <T> TTSQL notIn(TTSQLFunction<T,?> function,Object[] values, boolean match) {
        String field = StringHelper.smallHumpToUnderline(ReflectionHelper.getFieldName(function));
        return this.doIn(" ", field, values, match, false);
    }
    public TTSQL notIn(String field, Collection<?> values) {
        return this.doIn(" ", field, values, true, false);
    }
    public TTSQL notIn(String field, Collection<?> values, boolean match) {
        return this.doIn(" ", field, values, match, false);
    }
    public <T> TTSQL notIn(TTSQLFunction<T,?> function,Collection<?> values) {
        String field = StringHelper.smallHumpToUnderline(ReflectionHelper.getFieldName(function));
        return this.doIn(" ", field, values, true, false);
    }
    public <T> TTSQL notIn(TTSQLFunction<T,?> function,Collection<?> values, boolean match) {
        String field = StringHelper.smallHumpToUnderline(ReflectionHelper.getFieldName(function));
        return this.doIn(" ", field, values, match, false);
    }
    public TTSQL andNotIn(String field, Object[] values) {
        return this.doIn(" AND ", field, values, true, false);
    }
    public TTSQL andNotIn(String field, Object[] values, boolean match) {
        return this.doIn(" AND ", field, values, match, false);
    }
    public <T> TTSQL andNotIn(TTSQLFunction<T,?> function,Object[] values) {
        String field = StringHelper.smallHumpToUnderline(ReflectionHelper.getFieldName(function));
        return this.doIn(" AND ", field, values, true, false);
    }
    public <T> TTSQL andNotIn(TTSQLFunction<T,?> function,Object[] values, boolean match) {
        String field = StringHelper.smallHumpToUnderline(ReflectionHelper.getFieldName(function));
        return this.doIn(" AND ", field, values, match, false);
    }
    public TTSQL andNotIn(String field, Collection<?> values) {
        return this.doIn(" AND ", field, values, true, false);
    }
    public TTSQL andNotIn(String field, Collection<?> values, boolean match) {
        return this.doIn(" AND ", field, values, match, false);
    }
    public <T> TTSQL andNotIn(TTSQLFunction<T,?> function,Collection<?> values) {
        String field = StringHelper.smallHumpToUnderline(ReflectionHelper.getFieldName(function));
        return this.doIn(" AND ", field, values, true, false);
    }
    public <T> TTSQL andNotIn(TTSQLFunction<T,?> function,Collection<?> values, boolean match) {
        String field = StringHelper.smallHumpToUnderline(ReflectionHelper.getFieldName(function));
        return this.doIn(" AND ", field, values, match, false);
    }
    public TTSQL orNotIn(String field, Object[] values) {
        return this.doIn(" OR ", field, values, true, false);
    }
    public TTSQL orNotIn(String field, Object[] values, boolean match) {
        return this.doIn(" OR ", field, values, match, false);
    }
    public <T> TTSQL orNotIn(TTSQLFunction<T,?> function,Object[] values) {
        String field = StringHelper.smallHumpToUnderline(ReflectionHelper.getFieldName(function));
        return this.doIn(" OR ", field, values, true, false);
    }
    public <T> TTSQL orNotIn(TTSQLFunction<T,?> function,Object[] values, boolean match) {
        String field = StringHelper.smallHumpToUnderline(ReflectionHelper.getFieldName(function));
        return this.doIn(" OR ", field, values, match, false);
    }
    public TTSQL orNotIn(String field, Collection<?> values) {
        return this.doIn(" OR ", field, values, true, false);
    }
    public TTSQL orNotIn(String field, Collection<?> values, boolean match) {
        return this.doIn(" OR ", field, values, match, false);
    }
    public <T> TTSQL orNotIn(TTSQLFunction<T,?> function,Collection<?> values) {
        String field = StringHelper.smallHumpToUnderline(ReflectionHelper.getFieldName(function));
        return this.doIn(" OR ", field, values, true, false);
    }
    public <T> TTSQL orNotIn(TTSQLFunction<T,?> function,Collection<?> values, boolean match) {
        String field = StringHelper.smallHumpToUnderline(ReflectionHelper.getFieldName(function));
        return this.doIn(" OR ", field, values, match, false);
    }

    public TTSQL isNull(String field) {
        return this.doIsNull(" ", field, true, true);
    }
    public TTSQL isNull(String field, boolean match) {
        return this.doIsNull(" ", field, match, true);
    }
    public <T> TTSQL isNull(TTSQLFunction<T,?> function) {
        String field = StringHelper.smallHumpToUnderline(ReflectionHelper.getFieldName(function));
        return this.doIsNull(" ", field, true, true);
    }
    public <T> TTSQL isNull(TTSQLFunction<T,?> function, boolean match) {
        String field = StringHelper.smallHumpToUnderline(ReflectionHelper.getFieldName(function));
        return this.doIsNull(" ", field, match, true);
    }
    public TTSQL andIsNull(String field) {
        return this.doIsNull(" AND ", field, true, true);
    }
    public TTSQL andIsNull(String field, boolean match) {
        return this.doIsNull(" AND ", field, match, true);
    }
    public <T> TTSQL andIsNull(TTSQLFunction<T,?> function) {
        String field = StringHelper.smallHumpToUnderline(ReflectionHelper.getFieldName(function));
        return this.doIsNull(" AND ", field, true, true);
    }
    public <T> TTSQL andIsNull(TTSQLFunction<T,?> function, boolean match) {
        String field = StringHelper.smallHumpToUnderline(ReflectionHelper.getFieldName(function));
        return this.doIsNull(" AND ", field, match, true);
    }
    public TTSQL orIsNull(String field) {
        return this.doIsNull(" OR ", field, true, true);
    }
    public TTSQL orIsNull(String field, boolean match) {
        return this.doIsNull(" OR ", field, match, true);
    }
    public <T> TTSQL orIsNull(TTSQLFunction<T,?> function) {
        String field = StringHelper.smallHumpToUnderline(ReflectionHelper.getFieldName(function));
        return this.doIsNull(" OR ", field, true, true);
    }
    public <T> TTSQL orIsNull(TTSQLFunction<T,?> function, boolean match) {
        String field = StringHelper.smallHumpToUnderline(ReflectionHelper.getFieldName(function));
        return this.doIsNull(" OR ", field, match, true);
    }
    public TTSQL isNotNull(String field) {
        return this.doIsNull(" ", field, true, false);
    }
    public TTSQL isNotNull(String field, boolean match) {
        return this.doIsNull(" ", field, match, false);
    }
    public <T> TTSQL isNotNull(TTSQLFunction<T,?> function) {
        String field = StringHelper.smallHumpToUnderline(ReflectionHelper.getFieldName(function));
        return this.doIsNull(" ", field, true, false);
    }
    public <T> TTSQL isNotNull(TTSQLFunction<T,?> function, boolean match) {
        String field = StringHelper.smallHumpToUnderline(ReflectionHelper.getFieldName(function));
        return this.doIsNull(" ", field, match, false);
    }
    public TTSQL andIsNotNull(String field) {
        return this.doIsNull(" AND ", field, true, false);
    }
    public TTSQL andIsNotNull(String field, boolean match) {
        return this.doIsNull(" AND ", field, match, false);
    }
    public <T> TTSQL andIsNotNull(TTSQLFunction<T,?> function) {
        String field = StringHelper.smallHumpToUnderline(ReflectionHelper.getFieldName(function));
        return this.doIsNull(" AND ", field, true, false);
    }
    public <T> TTSQL andIsNotNull(TTSQLFunction<T,?> function, boolean match) {
        String field = StringHelper.smallHumpToUnderline(ReflectionHelper.getFieldName(function));
        return this.doIsNull(" AND ", field, match, false);
    }
    public TTSQL orIsNotNull(String field) {
        return this.doIsNull(" OR ", field, true, false);
    }
    public TTSQL orIsNotNull(String field, boolean match) {
        return this.doIsNull(" OR ", field, match, false);
    }
    public <T> TTSQL orIsNotNull(TTSQLFunction<T,?> function) {
        String field = StringHelper.smallHumpToUnderline(ReflectionHelper.getFieldName(function));
        return this.doIsNull(" OR ", field, true, false);
    }
    public <T> TTSQL orIsNotNull(TTSQLFunction<T,?> function, boolean match) {
        String field = StringHelper.smallHumpToUnderline(ReflectionHelper.getFieldName(function));
        return this.doIsNull(" OR ", field, match, false);
    }



    public static <T> T convert(Object obj,Class<T> cls){
        String json = JSON.toJSONString(obj);
        return JSON.parseObject(json, cls);
    }
}
