package com.tianji.learning.service;

import com.tianji.learning.domain.po.PointsBoard;
import com.baomidou.mybatisplus.extension.service.IService;
import com.tianji.learning.domain.query.PointsBoardQuery;
import com.tianji.learning.domain.vo.PointsBoardVO;

import java.util.List;

/**
 * <p>
 * 学霸天梯榜 服务类
 * </p>
 *
 * @author sefy
 * @since 2024-05-04
 */
public interface IPointsBoardService extends IService<PointsBoard> {

    //查询赛季积分排行榜
    PointsBoardVO queryPointsBoardBySeason(PointsBoardQuery query);

    //查询当前赛季 积分排名榜列表 redis中的zset中查询
    public List<PointsBoard> queryCurrentBoard(String key, Integer pageNo, Integer pageSize);
}
