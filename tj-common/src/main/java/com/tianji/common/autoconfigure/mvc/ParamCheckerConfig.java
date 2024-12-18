package com.tianji.common.autoconfigure.mvc;

import com.tianji.common.autoconfigure.mvc.aspects.CheckerAspect;
import com.tianji.common.ratelimiter.Aspect.RateLimiterAspect;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ParamCheckerConfig {

    @Bean
    public CheckerAspect checkerAspect(){
        return new CheckerAspect();
    }

}
