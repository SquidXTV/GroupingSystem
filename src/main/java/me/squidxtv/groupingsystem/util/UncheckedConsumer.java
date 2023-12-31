package me.squidxtv.groupingsystem.util;

public interface UncheckedConsumer<T> {

    void accept(T t) throws Exception;

}
