package com.ttsql.builder;


import com.ttsql.bean.BuildSource;
import com.ttsql.bean.SqlInfo;
import com.ttsql.helpers.StringHelper;

import java.util.List;

public class SqlInfoBuilder {
    SqlInfo sqlInfo;
    private StringBuilder join;
    private List<Object> params;
    Object context;
    private String prefix;
    private String suffix;

    SqlInfoBuilder() {
    }

    public static SqlInfoBuilder newInstace(BuildSource source) {
        SqlInfoBuilder builder = new SqlInfoBuilder();
        builder.init(source);
        return builder;
    }

    void init(BuildSource source) {
        this.sqlInfo = source.getSqlInfo();
        this.join = this.sqlInfo.getJoin();
        this.params = this.sqlInfo.getParams();
        this.context = source.getParamObj();
        this.prefix = source.getPrefix();
        this.suffix = source.getSuffix();
    }

    public SqlInfo buildNormalSql(String fieldText, Object value, String suffix) {
        this.join.append(this.prefix).append(fieldText).append(suffix);
        this.params.add(value);
        return this.sqlInfo.setJoin(this.join).setParams(this.params);
    }

    public SqlInfo buildLikeSql(String fieldText, Object value) {
        this.suffix = StringHelper.isBlank(this.suffix) ? " LIKE " : this.suffix;
        this.join.append(this.prefix).append(fieldText).append(this.suffix).append(sqlInfo.getPlaceholder()+" ");
//        this.params.add("%" + value + "%");
        this.params.add(value);
        return this.sqlInfo.setJoin(this.join).setParams(this.params);
    }

    public SqlInfo buildLikePatternSql(String fieldText, String pattern) {
        this.suffix = StringHelper.isBlank(this.suffix) ? " LIKE " : this.suffix;
        this.join.append(this.prefix).append(fieldText).append(this.suffix).append("'").append(pattern).append("' ");
        return this.sqlInfo.setJoin(this.join);
    }

    public SqlInfo buildBetweenSql(String fieldText, Object startValue, Object endValue) {
        if (startValue != null && endValue == null) {
            this.join.append(this.prefix).append(fieldText).append(" >= "+sqlInfo.getPlaceholder()+" ");
            this.params.add(startValue);
        } else if (startValue == null && endValue != null) {
            this.join.append(this.prefix).append(fieldText).append(" <= "+sqlInfo.getPlaceholder()+" ");
            this.params.add(endValue);
        } else {
            this.join.append(this.prefix).append(fieldText).append(" BETWEEN "+sqlInfo.getPlaceholder());
            this.params.add(startValue);
            this.join.append(" AND "+sqlInfo.getPlaceholder()+" ");
            this.params.add(endValue);
        }

        return this.sqlInfo.setJoin(this.join).setParams(this.params);
    }


    public SqlInfo buildInSql(String fieldText, Object[] values) {
        if (values != null && values.length != 0) {
            this.suffix = StringHelper.isBlank(this.suffix) ? " IN " : this.suffix;
            this.join.append(this.prefix).append(fieldText).append(this.suffix).append("(");
            int len = values.length;

            for(int i = 0; i < len; ++i) {
                if (i == len - 1) {

                    this.join.append(sqlInfo.getPlaceholder()+") ");
                } else {
                    this.join.append(sqlInfo.getPlaceholder()+", ");

                }

                this.params.add(values[i]);
            }

            return this.sqlInfo.setJoin(this.join).setParams(this.params);
        } else {
            return this.sqlInfo;
        }
    }

    public SqlInfo buildValueSql( Object[] values) {
        if (values != null && values.length != 0) {
            this.join.append(" ").append("(");
            int len = values.length;

            for(int i = 0; i < len; ++i) {
                if (i == len - 1) {
                    this.join.append(sqlInfo.getPlaceholder()+") ");
                } else {
                    this.join.append(sqlInfo.getPlaceholder()+", ");
                }

                this.params.add(values[i]);
            }

            return this.sqlInfo.setJoin(this.join).setParams(this.params);
        } else {
            return this.sqlInfo;
        }
    }




    public SqlInfo buildIsNullSql(String fieldText) {
        this.suffix = StringHelper.isBlank(this.suffix) ? " IS NULL " : this.suffix;
        this.join.append(this.prefix).append(fieldText).append(this.suffix);
        return this.sqlInfo.setJoin(this.join);
    }
}
