package com.ttsql.builder;

import com.ttsql.bean.BuildSource;

import java.util.Collection;

public class JavaSqlInfoBuilder extends SqlInfoBuilder {
    private JavaSqlInfoBuilder() {
    }

    public static JavaSqlInfoBuilder newInstace(BuildSource source) {
        JavaSqlInfoBuilder builder = new JavaSqlInfoBuilder();
        builder.init(source);
        return builder;
    }

    public void buildInSqlByCollection(String fieldText, Collection<Object> values) {
        super.buildInSql(fieldText, values == null ? null : values.toArray());
    }
}
