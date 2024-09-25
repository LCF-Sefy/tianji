-- 判断优惠券是否存在，不存在证明活动未开始 返回1
if(redis.call('exists', KEYS[1]) == 0) then
    return 1
end
-- 判断库存是否充足
if(tonumber(redis.call('hget', KEYS[1], 'totalNum')) <= 0) then
    return 2
end
-- 判断是否结束
if(tonumber(redis.call('time')[1]) > tonumber(redis.call('hget', KEYS[1], 'issueEndTime'))) then
    return 3
end
-- 判断限领数量
if(tonumber(redis.call('hget', KEYS[1], 'userLimit')) <= tonumber(redis.call('hget', KEYS[2], ARGV[1]) or '0'))then
    return 4
end

-- 当前用户领券数量+1
redis.call('hincrby', KEYS[2], ARGV[1], 1)

-- 扣减库存
redis.call('hincrby', KEYS[1], "totalNum", "-1")
-- 返回0 代表可以下单
return 0