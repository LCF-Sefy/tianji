-- 计算key 对优惠券的用户领取情况进行了大key拆分
-- 1.先计算拆分桶的数量   由于优惠券为秒杀，就算没有限制用户只能领取一张，但大多数情况下一个人只能领到一章，所以key需要保存的field条数≈优惠券初始总数
-- 一个key最好不要超过1000个 所以 分桶数量 = 优惠券初始总数/1000
local initNum  = tonumber(redis.call('hget', KEYS[1], 'initNum'))     --当前领取的这个优惠券初始总数
local buckets = math.ceil(initNum / 1000)    --计算拆分桶的数量

-- 2.计算分key
local hashcode  = ARGV[2]     -- 根据当前用户计算出的hashcode，传入进来的参数
local suffix = hashcode % buckets
KEYS[2] = KEYS[2] .. ':' .. suffix

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