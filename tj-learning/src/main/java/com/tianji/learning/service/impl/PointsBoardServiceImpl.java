package com.tianji.learning.service.impl;

import com.tianji.api.client.user.UserClient;
import com.tianji.api.dto.user.UserDTO;
import com.tianji.common.exceptions.BizIllegalException;
import com.tianji.common.utils.CollUtils;
import com.tianji.common.utils.StringUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.learning.constants.RedisConstants;
import com.tianji.learning.domain.po.PointsBoard;
import com.tianji.learning.domain.query.PointsBoardQuery;
import com.tianji.learning.domain.vo.PointsBoardItemVO;
import com.tianji.learning.domain.vo.PointsBoardVO;
import com.tianji.learning.mapper.PointsBoardMapper;
import com.tianji.learning.service.IPointsBoardService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>
 * 学霸天梯榜 服务实现类
 * </p>
 *
 * @author sefy
 * @since 2024-05-04
 */
@Service
@RequiredArgsConstructor
public class PointsBoardServiceImpl extends ServiceImpl<PointsBoardMapper, PointsBoard> implements IPointsBoardService {

    private final StringRedisTemplate stringRedisTemplate;
    private final UserClient userClient;

    /**
     * 查询赛季积分排行榜
     *
     * @param query
     * @return
     */
    @Override
    public PointsBoardVO queryPointsBoardBySeason(PointsBoardQuery query) {
        //1.获取当前登录用户id
        Long userId = UserContext.getUser();

        //2.判断是当前赛季还是历史赛季  query.season 赛季id，为null或者0则代表查询当前赛季
        //isCurrent = true表示查询当前赛季 false表示查询历史赛季
        boolean isCurrent = query.getSeason() == null || query.getSeason() == 0;
        LocalDate now = LocalDate.now();
        String format = now.format(DateTimeFormatter.ofPattern("yyyyMM"));
        String key = RedisConstants.POINTS_BOARD_KEY_PREFIX + format;
        Long season = query.getSeason();  //历史赛季id

        //3.查询我的排名和积分  根据season判断是 查redis还是db  如果是当前赛季的话查redis，如果是历史赛季查数据库（因为历史赛季的数据会被定时同步到db中）
        PointsBoard board = isCurrent ? queryMyCurrentBoard(key) : queryMyHistoryBoard(season);

        //4.分页查询赛季列表  根据season判断是 查redis还是db  如果是当前赛季的话查redis，如果是历史赛季查数据库（因为历史赛季的数据会被定时同步到db中）
        List<PointsBoard> list = isCurrent ? queryCurrentBoard(key, query.getPageNo(), query.getPageSize()) : queryHistoryBoard(query);

        //5.封装用户id集合 远程调用用户user服务 获取用户信息（name） 转成map
        //先封装用户id集合，批量查询
        Set<Long> uids = list.stream().map(PointsBoard::getUserId).collect(Collectors.toSet());
        List<UserDTO> userDTOS = userClient.queryUserByIds(uids);
        if (CollUtils.isEmpty(userDTOS)) {
            throw new BizIllegalException("用户不存在");
        }
        //用户信息集合转成map key：userId  value：name
        Map<Long, String> userDtoMap = userDTOS.stream().collect(Collectors.toMap(UserDTO::getId, c -> c.getName()));


        //5.封装VO返回
        PointsBoardVO vo = new PointsBoardVO();
        vo.setRank(board.getRank());   //我的排名
        vo.setPoints(board.getPoints());       //我的积分

        List<PointsBoardItemVO> voList = new ArrayList<>();
        for (PointsBoard pointsBoard : list) {
            PointsBoardItemVO itemVO = new PointsBoardItemVO();
            itemVO.setRank(pointsBoard.getRank());
            itemVO.setPoints(pointsBoard.getPoints());
            itemVO.setName(userDtoMap.get(pointsBoard.getUserId()));
            voList.add(itemVO);
        }
        vo.setBoardList(voList);
        return vo;
    }

    //查询历史赛季 积分排名榜 db中查询
    private List<PointsBoard> queryHistoryBoard(PointsBoardQuery query) {
        //TODO
        return null;
    }


    //查询当前赛季 积分排名榜列表 redis中的zset中查询
    public List<PointsBoard> queryCurrentBoard(String key, Integer pageNo, Integer pageSize) {
        //1.计算start和end 分页查
        int start = (pageNo - 1) * pageSize;
        int end = start + pageSize - 1;

        //2.利用zrevrange从redis查询 按照分数倒序
        Set<ZSetOperations.TypedTuple<String>> typedTuples = stringRedisTemplate.opsForZSet().reverseRangeWithScores(key, start, end);
        if (CollUtils.isEmpty(typedTuples)) {
            return CollUtils.emptyList();
        }
        int rank = start + 1;
        List<PointsBoard> list = new ArrayList<>();
        for (ZSetOperations.TypedTuple<String> typedTuple : typedTuples) {
            Double score = typedTuple.getScore();   //用户分积分
            String userId = typedTuple.getValue();  //用户id
            if (StringUtils.isBlank(userId) || score == null) {
                continue;
            }
            PointsBoard board = new PointsBoard();
            board.setUserId(Long.valueOf(userId));
            board.setPoints(score.intValue());
            board.setRank(rank++); //设置名次
            list.add(board);
        }
        //3.封装结果返回
        return list;
    }


    //查询历史赛季 我的积分排名 db中查询
    private PointsBoard queryMyHistoryBoard(Long season) {
        //TODO
        return null;
    }

    //查询当前赛季 我的积分排名 redis中查询
    private PointsBoard queryMyCurrentBoard(String key) {
        Long userId = UserContext.getUser();
        //从redis中获取分值
        Double score = stringRedisTemplate.opsForZSet().score(key, userId.toString());
        //获取排名  下标从0开始 需要 + 1
        Long rank = stringRedisTemplate.opsForZSet().reverseRank(key, userId.toString());
        PointsBoard board = new PointsBoard();
        board.setPoints(score == null ? 0 : score.intValue());
        board.setRank(rank == null ? 0 : rank.intValue() + 1);
        return board;
    }


}
