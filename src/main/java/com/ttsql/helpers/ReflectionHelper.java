package com.ttsql.helpers;


import com.ttsql.Exception.NotFindPrimaryKeyException;
import com.ttsql.Exception.SetManyPrimaryKeyException;
import com.ttsql.anno.*;
import com.alibaba.fastjson.JSON;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

import java.beans.Introspector;
import java.lang.invoke.SerializedLambda;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


public class ReflectionHelper {

    private static Map<TTSQLFunction<?, ?>, Field> cache = new ConcurrentHashMap<>();

    public static <T, R> String getFieldName(TTSQLFunction<T, R> function) {
        Field field = ReflectionHelper.getField(function);
        return field.getName();
    }

    public <T, R> String getFieldName1(TTSQLFunction<T, R> function) {
        Field field = ReflectionHelper.getField(function);
        return field.getName();
    }

    public static <T, R> Field getField(TTSQLFunction<T, R> function) {
        return cache.computeIfAbsent(function, ReflectionHelper::findField);
    }

    public static <T, R> Field findField(TTSQLFunction<T, R> function) {
        Field field = null;
        String fieldName = null;
        try {
            // 第1步 获取SerializedLambda
            Method method = function.getClass().getDeclaredMethod("writeReplace");
            method.setAccessible(Boolean.TRUE);
            SerializedLambda serializedLambda = (SerializedLambda) method.invoke(function);
            // 第2步 implMethodName 即为Field对应的Getter方法名
            String implMethodName = serializedLambda.getImplMethodName();
            if (implMethodName.startsWith("get") && implMethodName.length() > 3) {
                fieldName = Introspector.decapitalize(implMethodName.substring(3));

            } else if (implMethodName.startsWith("is") && implMethodName.length() > 2) {
                fieldName = Introspector.decapitalize(implMethodName.substring(2));
            } else if (implMethodName.startsWith("lambda$")) {
                throw new IllegalArgumentException("SerializableFunction不能传递lambda表达式,只能使用方法引用");

            } else {
                throw new IllegalArgumentException(implMethodName + "不是Getter方法引用");
            }
            // 第3步 获取的Class是字符串，并且包名是“/”分割，需要替换成“.”，才能获取到对应的Class对象
            String declaredClass = serializedLambda.getImplClass().replace("/", ".");
            Class<?> aClass = Class.forName(declaredClass, false, ClassUtils.getDefaultClassLoader());

            // 第4步  Spring 中的反射工具类获取Class中定义的Field
            field = ReflectionUtils.findField(aClass, fieldName);

        } catch (Exception e) {
            e.printStackTrace();
        }
        // 第5步 如果没有找到对应的字段应该抛出异常
        if (field != null) {
            return field;
        }
        throw new NoSuchFieldError(fieldName);
    }

    //获取实体类主键 属性
    public static Field getPrimaryField(Class<?> clazz)  {
        Field[] fields = clazz.getDeclaredFields();
        Field item = null;

        for (Field field : fields) {

            Object autoId = field.getAnnotation(TTAutoPrimaryKey.class);
            field.setAccessible(true);

            if (autoId != null) {
                item = field;
                break;
            }
            Object snowFlakeId = field.getAnnotation(TTSnowFlakePrimaryKey.class);
            if (snowFlakeId != null) {
                item = field;
                break;
            }
            Object redisId = field.getAnnotation(TTRedisPrimaryKey.class);
            if (redisId != null) {
                item = field;
                break;
            }
            //如果多个注解都设置就抛出异常
            if (
                    (autoId != null)&&(snowFlakeId != null)||
                    (autoId != null)&&(redisId != null)||
                    (snowFlakeId != null)&&(redisId != null)
            ){
                throw new SetManyPrimaryKeyException("实体类上不能同时设置多种类型主键");
            }

        }
        if (item == null) {
            Class<?> superclass = clazz.getSuperclass();
            if (superclass != null) {
                item = getPrimaryField(superclass);
            }
        }


        return item;
    }


    //反射对象 获取
    //    属性 数组(不包含主键)
    // 和 属性值 数组(不包含主键)
    // 和 属性值 字符串 (不包含主键)
    // 和 主键Field

    public static List getIgnorePrimaryField(Object obj,boolean isInsert)  {


        StringBuilder fieldBuiler = new StringBuilder();
        List fieldsList = new ArrayList();
        List valuesList = new ArrayList();

        Field[] declaredFields = obj.getClass().getDeclaredFields();

        Field primaryField = getPrimaryField(obj.getClass());
        String pkName = primaryField.getName();
        if (pkName==null){
            throw new NotFindPrimaryKeyException("改实体类没有找到主键");
        }
        for (Field declaredField : declaredFields) {
            declaredField.setAccessible(true);
            try {
                //如果属性是主键 并且属性值为空
                if (pkName.equals(declaredField.getName()) && declaredField.get(obj)==null) {

                } else {

                    //如果是CreateField 并且是 insert语句
                    if(checkCreateField(declaredField)&&isInsert){
                        String filedName = StringHelper.smallHumpToUnderline(declaredField.getName());
                        fieldBuiler.append(filedName).append(",");
                        valuesList.add(new Date());
                        fieldsList.add(filedName);
                    }
                    //如果是UpdateField
                   else if(checkUpdateField(declaredField)){
                        String filedName = StringHelper.smallHumpToUnderline(declaredField.getName());
                        fieldBuiler.append(filedName).append(",");
                        valuesList.add(new Date());
                        fieldsList.add(filedName);
                    }

                    else {
                        declaredField.setAccessible(true);
                        Object value = declaredField.get(obj);
                        if (value==null){
                            continue;
                        }
                        String filedName = StringHelper.smallHumpToUnderline(declaredField.getName());
                        fieldBuiler.append(filedName).append(",");
                        valuesList.add(value);
                        fieldsList.add(filedName);
                    }



                }

            } catch (Exception e) {

            }
        }
        List list = new ArrayList();
        list.add(fieldsList);
        list.add(valuesList);

        if (fieldsList.size()>0){
            fieldBuiler.deleteCharAt(fieldBuiler.length()-1);
        }

        list.add(fieldBuiler.toString());

        list.add(primaryField);



        return list;
    }

    //反射对象 获取
    //获取所有属性
    //和所有 值
    public static List getField(Object obj,boolean isInsert)  {


        StringBuilder fieldBuiler = new StringBuilder();
        List fieldsList = new ArrayList();
        List valuesList = new ArrayList();

        Field[] declaredFields = obj.getClass().getDeclaredFields();

//        Field primaryField = getPrimaryField(obj.getClass());
//        String pkName = primaryField.getName();
//        if (pkName==null){
//            throw new NotFindPrimaryKeyException("改实体类没有找到主键");
//        }
        for (Field declaredField : declaredFields) {
            try {

                //如果是CreateField 并且是 insert语句
                if(checkCreateField(declaredField)&&isInsert){
                    String filedName = StringHelper.smallHumpToUnderline(declaredField.getName());
                    fieldBuiler.append(filedName).append(",");
                    valuesList.add(new Date());
                    fieldsList.add(filedName);
                }
                //如果是UpdateField
                else if(checkUpdateField(declaredField)){
                    String filedName = StringHelper.smallHumpToUnderline(declaredField.getName());
                    fieldBuiler.append(filedName).append(",");
                    valuesList.add(new Date());
                    fieldsList.add(filedName);
                }

                else {
                    declaredField.setAccessible(true);
                    Object value = declaredField.get(obj);
                    if (value==null){
                        continue;
                    }
                    String filedName = StringHelper.smallHumpToUnderline(declaredField.getName());
                    fieldBuiler.append(filedName).append(",");
                    valuesList.add(value);
                    fieldsList.add(filedName);
                }

            } catch (Exception e) {

            }
        }
        List list = new ArrayList();
        list.add(fieldsList);
        list.add(valuesList);

        if (fieldsList.size()>0){
            fieldBuiler.deleteCharAt(fieldBuiler.length()-1);
        }

        list.add(fieldBuiler.toString());

        return list;
    }



    //反射类型 获取
    // 属性拼接的字符串(含主键)
    public static String getClassFieldStr(Class cls)  {

        Field[] declaredFields = cls.getDeclaredFields();
        StringBuilder fieldBuiler = new StringBuilder();
        for (Field declaredField : declaredFields) {
            fieldBuiler.append(StringHelper.smallHumpToUnderline(declaredField.getName())).append(",");
        }
        if (declaredFields.length>0){
            fieldBuiler.deleteCharAt(fieldBuiler.length()-1);
        }
        return fieldBuiler.toString();
    }


    //反射给对象的主键 设置值
    public static void setValueToPrimaryField(Object obj,Object Value)  {

        Field[] declaredFields = obj.getClass().getDeclaredFields();
        String pkName = getPrimaryField(obj.getClass()).getName();
        if (pkName==null){
            throw new NotFindPrimaryKeyException("该实体类没有找到主键");
        }
        for (Field declaredField : declaredFields) {
            try {
                if (pkName.equals(declaredField.getName())) {
                    declaredField.setAccessible(true);
                    String value = JSON.toJSONString(Value);
                    declaredField.set(obj,JSON.parseObject(value, declaredField.getType()));
                    break;
                }

            } catch (Exception e) {

            }
        }

    }

    //判断Feild 是否 有 更新 自动填充字段 注解
    public static boolean checkUpdateField(Field field)  {

        TTUpdateField updateField = field.getAnnotation(TTUpdateField.class);
        if (updateField==null){
            return false;
        }
        return true;
    }

    //判断Feild 是否 有 添加 自动填充字段 注解
    public static boolean checkCreateField(Field field)  {

        TTCreateField createField = field.getAnnotation(TTCreateField.class);
        if (createField==null){
            return false;
        }
        return true;
    }

    //判断该类是否有逻辑删除字段
    // 并获取 注解中的DeleteValue值
    public static List getLogicFieldAndLogicAnnotationValue(Class cls)  {
        TTLogicField anno = null;
        Field field = null;
        for (Field declaredField : cls.getDeclaredFields()) {
            anno = declaredField.getAnnotation(TTLogicField.class);
            if (anno!=null){
                field = declaredField;
                break;
            }
        }
        if (anno==null){
            return null;
        }else {
            ArrayList result = new ArrayList<>();

            result.add(field);
            result.add(anno.deleteValue());
            return result;
        }

    }

}
