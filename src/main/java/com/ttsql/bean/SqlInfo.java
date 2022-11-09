package com.ttsql.bean;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class SqlInfo  {

    //是否按照jdbc 的 sql进行拼接 (默认按照mybatis)
    private boolean isJDBCSql = false;
    public boolean isJDBCSql() {
        return isJDBCSql;
    }
    public void setJDBCSql(boolean JDBCSql) {
        isJDBCSql = JDBCSql;
    }
    public String getPlaceholder (){
        if (!this.isJDBCSql)
            return  "#{"+"TTSqlparms"+this.getParams().size() +"}";
        else
            return "?";
    }



    public HashMap getMybatisMap() {

        HashMap map = new HashMap<>();
        map.put("sql",getSql());
        for (int i = 0; i < getParamsArr().length; i++) {
            map.put("TTSqlparms"+i, this.params.get(i));
        }
        return map;
    }



    private StringBuilder join;
    private List<Object> params;
    private String sql;


    private SqlInfo(StringBuilder join, List<Object> params) {
        this.join = join;
        this.params = params;
    }

    public static SqlInfo newInstance() {
        return new SqlInfo(new StringBuilder(""), new ArrayList());
    }

    public Object[] getParamsArr() {
        return this.params == null ? new Object[0] : this.params.toArray();
    }

    public SqlInfo removeIfExist(String subSql) {
        this.sql = subSql != null && this.sql.contains(subSql) ? this.sql.replaceAll(subSql, "") : this.sql;
        return this;
    }

    public StringBuilder getJoin() {
        return this.join;
    }

    public SqlInfo setJoin(StringBuilder join) {
        this.join = join;
        return this;
    }

    public List<Object> getParams() {
        return this.params;
    }

    public SqlInfo setParams(List<Object> params) {
        this.params = params;
        return this;
    }

    public String getSql() {
        return this.sql;
    }

    public SqlInfo setSql(String sql) {
        this.sql = sql;
        return this;
    }

}
