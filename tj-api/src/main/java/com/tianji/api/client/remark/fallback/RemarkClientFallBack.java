package com.tianji.api.client.remark.fallback;

import com.tianji.api.client.remark.RemarkClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;

import java.util.List;
import java.util.Set;

/**
 * RemarkClient降级类
 */
@Slf4j
public class RemarkClientFallBack implements FallbackFactory<RemarkClient> {

    //如果remark服务没启动，或者其他服务调用remark服务超时则走create降级
    @Override
    public RemarkClient create(Throwable cause) {
        log.error("调用remark服务降级了", cause);
        return new RemarkClient() {
            @Override
            public Set<Long> getLikesStatusByBizIds(List<Long> bizIds) {
                return null;
            }
        };
    }
}
