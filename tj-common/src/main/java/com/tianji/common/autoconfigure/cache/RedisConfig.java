package com.tianji.common.autoconfigure.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.tianji.common.cache.distribute.redis.RedisCacheService;
import com.tianji.common.serializer.JodaDateTimeJsonDeserializer;
import com.tianji.common.serializer.JodaDateTimeJsonSerializer;
import com.tianji.common.utils.serializer.FastJsonRedisSerializer;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
@ConditionalOnClass({RedisTemplate.class, StringRedisTemplate.class})
@AutoConfigureBefore(RedisAutoConfiguration.class)
public class RedisConfig {



    @Bean
    @ConditionalOnMissingBean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<String, Object>();
        redisTemplate.setConnectionFactory(redisConnectionFactory);
        //给key进行序列化
        StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();
        redisTemplate.setKeySerializer(stringRedisSerializer);

        //fastJson序列化器
        FastJsonRedisSerializer<Object> fastJsonRedisSerializer = new FastJsonRedisSerializer<>(Object.class);

        // 设置 value 的序列化器 GenericJackson2JsonRedisSerializer
        GenericJackson2JsonRedisSerializer jsonRedisSerializer  = new GenericJackson2JsonRedisSerializer();

        // 设置Redis中的键（key）序列化方式为字符串序列化
        redisTemplate.setKeySerializer(RedisSerializer.string());
        // 设置Redis中哈希类型数据的键（hash key）序列化方式为字符串序列化
        redisTemplate.setHashKeySerializer(RedisSerializer.string());

        // 设置Redis中的值（value）序列化方式为JSON序列化
        redisTemplate.setValueSerializer(fastJsonRedisSerializer);

        // 设置Redis中哈希类型数据的值（hash value）序列化方式为JSON序列化
        redisTemplate.setHashValueSerializer(fastJsonRedisSerializer);

        return redisTemplate;
    }

    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory redisConnectionFactory) {
        StringRedisTemplate stringRedisTemplate = new StringRedisTemplate();
        stringRedisTemplate.setConnectionFactory(redisConnectionFactory);
        return stringRedisTemplate;
    }

    @Bean
    @ConditionalOnClass(RedisTemplate.class)
    @ConditionalOnMissingBean
    public RedisCacheService redisCacheService() {
        return new RedisCacheService();
    }

}
