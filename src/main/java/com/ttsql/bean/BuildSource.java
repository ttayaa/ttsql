package com.ttsql.bean;

public class BuildSource {
    private String nameSpace;
    private SqlInfo sqlInfo;
    private Object paramObj;
    private String prefix;
    private String suffix;

    public BuildSource(SqlInfo sqlInfo) {
        this.sqlInfo = sqlInfo;
        this.resetPrefix();
        this.resetSuffix();
    }

    public BuildSource(String nameSpace, SqlInfo sqlInfo, Object paramObj) {
        this.nameSpace = nameSpace;
        this.sqlInfo = sqlInfo;
        this.paramObj = paramObj;
        this.resetPrefix();
        this.resetSuffix();
    }

    public void resetPrefix() {
        this.prefix = " ";
        this.suffix = "";
    }

    public void resetSuffix() {
        this.suffix = " ";
    }

    public String getNameSpace() {
        return this.nameSpace;
    }

    public SqlInfo getSqlInfo() {
        return this.sqlInfo;
    }

    public BuildSource setSqlInfo(SqlInfo sqlInfo) {
        this.sqlInfo = sqlInfo;
        return this;
    }


    public Object getParamObj() {
        return this.paramObj;
    }

    public String getPrefix() {
        return this.prefix;
    }

    public BuildSource setPrefix(String prefix) {
        this.prefix = prefix;
        return this;
    }

    public String getSuffix() {
        return this.suffix;
    }

    public BuildSource setSuffix(String suffix) {
        this.suffix = suffix;
        return this;
    }

    public BuildSource setParamObj(Object paramObj) {
        this.paramObj = paramObj;
        return this;
    }
}
