package com.tianji.learning.task;

import com.tianji.common.utils.CollUtils;
import com.tianji.learning.constants.RedisConstants;
import com.tianji.learning.domain.po.PointsBoard;
import com.tianji.learning.domain.po.PointsBoardSeason;
import com.tianji.learning.service.IPointsBoardSeasonService;
import com.tianji.learning.service.IPointsBoardService;
import com.tianji.learning.utils.TableInfoContext;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static com.tianji.learning.constants.LearningConstants.POINTS_BOARD_TABLE_PREFIX;

@Component
@RequiredArgsConstructor
@Slf4j
public class PointsBoardPersistentHandler {

    private final IPointsBoardSeasonService pointsBoardSeasonService;
    private final IPointsBoardService pointsBoardService;
    private final StringRedisTemplate redisTemplate;

    /**
     * 1.创建上赛季（上个月） 榜单表  表名：points_board_赛季id
     */
    @XxlJob("createTableJob")  //每个月1号凌晨3点运行
    public void createPointsBoardTableOfLastSeason() {
        log.debug("创建上赛季榜单表任务执行了");
        //1.获取上个月当前时间点
        LocalDate time = LocalDate.now().minusMonths(1);

        //2.查询赛季表获取上个月所对应的赛季id 条件 begin_time <= time and end_time >= time
        PointsBoardSeason one = pointsBoardSeasonService
                .lambdaQuery()
                .le(PointsBoardSeason::getBeginTime, time)
                .ge(PointsBoardSeason::getEndTime, time)
                .one();
        log.debug("上赛季信息 {}", one);
        if (one == null) {
            return;
        }

        //3.创建上赛季榜单表  表名：points_board_赛季id
        pointsBoardSeasonService.createPointsBoardLatestTable(one.getId());
    }

    /**
     * 2.持久化上赛季（上个月的）排行榜数据到db中
     */
    @XxlJob("savePointsBoard2DB")  //要和xxl-job里的jobHandler名字一致
    public void savePointsBoard2DB() {
        //1.获取上个月的当前时间点
        LocalDate time = LocalDate.now().minusMonths(1);

        //2.查询赛季表 points_board_season 获取赛季信息(id)  begin_time <= time and end_time >= time
        PointsBoardSeason one = pointsBoardSeasonService
                .lambdaQuery()
                .le(PointsBoardSeason::getBeginTime, time)
                .ge(PointsBoardSeason::getEndTime, time)
                .one();
        log.debug("上赛季信息 {}", one);
        if (one == null) {
            return;
        }

        //3.计算动态表名 并存入ThreadLocal
        String tableName = POINTS_BOARD_TABLE_PREFIX + one.getId();
        log.debug("动态表名为 {}", tableName);
        TableInfoContext.setInfo(tableName);

        //4.分页查询获取redis上赛季积分排行榜数据
        String format = time.format(DateTimeFormatter.ofPattern("yyyyMM"));
        String key = RedisConstants.POINTS_BOARD_KEY_PREFIX + format; //key= boards：上赛季的年月

        int shardIndex = XxlJobHelper.getShardIndex();  //当前分片的索引 从0开始
        int shardTotal = XxlJobHelper.getShardTotal();  //总分片数

        int pageNo = shardIndex + 1;//页码
        int pageSize = 5;   //一次持久化5条
        while (true) {
            log.debug("处理第{}页数据", pageNo);
            List<PointsBoard> pointsBoardList = pointsBoardService.queryCurrentBoard(key, pageNo, pageSize);
            if (CollUtils.isEmpty(pointsBoardList)) {
                break;
            }

            //5.将该页数据持久化到db相应的赛季榜单表中 批量新增数据
            pointsBoardList.forEach(b -> {
                b.setId(Long.valueOf(b.getRank()));
                b.setRank(null);
            });
            pointsBoardService.saveBatch(pointsBoardList);
            pageNo += shardTotal;
        }

        //6.清除threadLocal中的数据
        TableInfoContext.remove();
    }

    /**
     * 3.清理Redis中的历史赛季榜单数据
     */
    @XxlJob("clearPointsBoardFromRedis")
    public void clearPointsBoardFromRedis() {
        //1.获取上个月的当前时间点
        LocalDate time = LocalDate.now().minusMonths(1);

        //2.计算key
        String format = time.format(DateTimeFormatter.ofPattern("yyyyMM"));
        String key = RedisConstants.POINTS_BOARD_KEY_PREFIX + format; //key= boards：上赛季的年月

        //3.删除
        redisTemplate.unlink(key);
    }
}