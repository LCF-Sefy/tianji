package com.tianji.common.utils.serializer;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.parser.ParserConfig;
import com.alibaba.fastjson.serializer.SerializerFeature;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;

import java.nio.charset.Charset;

/**
 * <p>
 * FastJson序列化
 * </p>
 *
 * @author yangxiaohui
 * @since 2020/5/18
 */
public class FastJsonRedisSerializer<T> implements RedisSerializer<T> {
    /**
     * 解决反序列化时Could not deserialize: autoType is not support. 的问题
     */
    private final static ParserConfig defaultRedisConfig = new ParserConfig();

    static {
        defaultRedisConfig.setAutoTypeSupport(true);
    }

    /**
     * DEFAULT_CHARSET <br>
     */
    public static final Charset DEFAULT_CHARSET = Charset.forName("UTF-8");
    /**
     * clazz 反序列化类<br>
     */
    private Class<T> clazz;

    public FastJsonRedisSerializer(Class<T> clazz) {
        super();
        this.clazz = clazz;
    }

    /**
     * 序列化
     *
     * @param t 对象
     * @return 字节码
     * @throws SerializationException 序列化异常
     */
    @Override
    public byte[] serialize(T t) throws SerializationException {
        if (t == null) {
            return new byte[0];
        }
        return JSON.toJSONString(t, SerializerFeature.WriteClassName).getBytes(DEFAULT_CHARSET);
    }

    /**
     * 反序列化
     *
     * @param bytes 字节码
     * @return 对象
     */
    @Override
    public T deserialize(byte[] bytes) throws SerializationException {
        if (bytes == null || bytes.length <= 0) {
            return null;
        }
        String str = new String(bytes, DEFAULT_CHARSET);
        try {
            return (T) JSON.parseObject(str, clazz, defaultRedisConfig);
        }catch (Exception e){
            // 如果报错，再次反序列化并返回
            return (T) JSON.parseObject(str, clazz, defaultRedisConfig);
        }
    }
}