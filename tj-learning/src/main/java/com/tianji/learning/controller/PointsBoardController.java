package com.tianji.learning.controller;


import com.tianji.learning.domain.query.PointsBoardQuery;
import com.tianji.learning.domain.vo.PointsBoardVO;
import com.tianji.learning.service.IPointsBoardService;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 * 学霸天梯榜 前端控制器
 * </p>
 *
 * @author sefy
 * @since 2024-05-04
 */
@RestController
@RequestMapping("/boards")
@RequiredArgsConstructor
public class PointsBoardController {

    private final IPointsBoardService pointsBoardService;

    @ApiOperation("查询积分榜-当前赛季和历史赛季都可以用")
    @GetMapping
    public PointsBoardVO queryPointsBoardBySeason(PointsBoardQuery query) {
        return pointsBoardService.queryPointsBoardBySeason(query);
    }

}
