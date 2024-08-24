package com.tianji.learning.service.impl;

import com.tianji.learning.constants.LearningConstants;
import com.tianji.learning.domain.po.PointsBoardSeason;
import com.tianji.learning.mapper.PointsBoardSeasonMapper;
import com.tianji.learning.service.IPointsBoardSeasonService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author sefy
 * @since 2024-05-04
 */
@Service
public class PointsBoardSeasonServiceImpl extends ServiceImpl<PointsBoardSeasonMapper, PointsBoardSeason> implements IPointsBoardSeasonService {

    /**
     * 创建上赛季榜单表
     *
     * @param id
     */
    @Override
    public void createPointsBoardLatestTable(Integer id) {
        getBaseMapper().createPointsBoardTable(LearningConstants.POINTS_BOARD_TABLE_PREFIX + id);
    }
}
