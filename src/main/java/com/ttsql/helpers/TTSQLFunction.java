package com.ttsql.helpers;

import java.io.Serializable;
import java.util.function.Function;

@FunctionalInterface
public interface TTSQLFunction<T, R> extends Function<T, R>, Serializable {
}
