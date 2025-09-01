package com.github.eacryo.mongoflex.lambda;

import java.io.Serializable;
import java.util.function.Function;

// 可序列化的函数接口
@FunctionalInterface
public interface SFunction<T, R> extends Function<T, R>, Serializable {

}
