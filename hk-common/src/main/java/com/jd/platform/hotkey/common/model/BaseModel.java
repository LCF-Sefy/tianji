package com.jd.platform.hotkey.common.model;

import com.alibaba.fastjson.annotation.JSONField;
import com.jd.platform.hotkey.common.convert.LongAdderSerializer;
import com.jd.platform.hotkey.common.tool.IdGenerater;

import java.util.concurrent.atomic.LongAdder;

/**
 * 热key的定义
 * @author wuweifeng wrote on 2019-12-05
 * @version 1.0
 */
public class BaseModel {
    private String id = IdGenerater.generateId();
    /**
     * 创建的时间
     */
    private long createTime;
    /**
     * key的名字
     */
    private String key;
    /**
     * 该key出现的数量，如果一次一发那就是1，累积多次发那就是count
     * 使用 LongAdder 解决 多线程计数不准确的问题
     */
    @JSONField(serializeUsing = LongAdderSerializer.class)
    private LongAdder count;

    @Override
    public String toString() {
        return "BaseModel{" +
                "id='" + id + '\'' +
                ", createTime=" + createTime +
                ", key='" + key + '\'' +
                ", count=" + count +
                '}';
    }

    /**
     * 获取计数总数
     * @return 总数
     */
    public long getCount() {
        return count.sum();
    }

    /**
     * 设置计数
     * @param count 计数 LongAdder 对象
     */
    public void setCount(LongAdder count) {
        this.count = count;
    }

    /**
     * 计数自增指定数量
     * @param count 指定数量
     */
    public void add(long count){
        this.count.add(count);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public long getCreateTime() {
        return createTime;
    }

    public void setCreateTime(long createTime) {
        this.createTime = createTime;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

}
