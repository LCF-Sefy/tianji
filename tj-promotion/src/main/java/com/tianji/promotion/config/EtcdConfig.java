package com.tianji.promotion.config;

import com.jd.platform.hotkey.common.configcenter.IConfigCenter;
import com.jd.platform.hotkey.common.configcenter.etcd.JdEtcdBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author wuweifeng
 * @version 1.0
 * @date 2020-07-27
 */
@Configuration
public class EtcdConfig {

    @Value("${etcd.server}")
    private String etcd;


    @Bean
    public IConfigCenter client() {
        //连接多个时，逗号分隔
        return JdEtcdBuilder.build(etcd);
    }
}
