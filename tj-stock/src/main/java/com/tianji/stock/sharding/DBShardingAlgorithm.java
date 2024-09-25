//package com.tianji.stock.sharding;//package com.tianji.stock.sharding;
//
//import com.tianji.common.exceptions.BadRequestException;
//import com.tianji.common.exceptions.CommonException;
//import lombok.Getter;
//import org.apache.shardingsphere.api.sharding.standard.PreciseShardingAlgorithm;
//import org.apache.shardingsphere.api.sharding.standard.PreciseShardingValue;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.stereotype.Component;
//
//
//import java.util.Collection;
//import java.util.List;
//import java.util.Properties;
//
///**
// * 基于 HashMod 方式自定义分库算法
// */
//@Component
//public class DBShardingAlgorithm implements PreciseShardingAlgorithm<Long> {
//
//
//    @Value("${sharding-count}")
//    private Integer shardingCount;
//
//
//    @Override
//    public String doSharding(Collection<String> availableTargetNames, PreciseShardingValue<Long> shardingValue) {
//        long id = shardingValue.getValue();
//        int dbSize = availableTargetNames.size();
//        int mod = (int) hashShardingValue(id) % shardingCount / (shardingCount / dbSize);
//        int index = 0;
//        for (String targetName : availableTargetNames) {
//            if (index == mod) {
//                return targetName;
//            }
//            index++;
//        }
//        throw new IllegalArgumentException("No target found for value: " + id);
//    }
//
//
//    private int getShardingCount() {
//        if(shardingCount == null){
//            throw new CommonException("sharding-count is not set");
//        }
//        return shardingCount;
//    }
//
//    private long hashShardingValue(final Comparable<?> shardingValue) {
//        return Math.abs((long) shardingValue.hashCode());
//    }
//
//}
