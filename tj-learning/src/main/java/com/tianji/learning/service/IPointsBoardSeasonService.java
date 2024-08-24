package com.tianji.learning.service;

import com.tianji.learning.domain.po.PointsBoardSeason;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author sefy
 * @since 2024-05-04
 */
public interface IPointsBoardSeasonService extends IService<PointsBoardSeason> {

    /**
     * 创建上赛季榜单表
     * @param id
     */
    void createPointsBoardLatestTable(Integer id);
}
