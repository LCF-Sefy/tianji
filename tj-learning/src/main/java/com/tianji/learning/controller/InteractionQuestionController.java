package com.tianji.learning.controller;


import com.tianji.common.domain.dto.PageDTO;
import com.tianji.learning.domain.dto.QuestionFormDTO;
import com.tianji.learning.domain.query.QuestionPageQuery;
import com.tianji.learning.domain.vo.QuestionVO;
import com.tianji.learning.service.IInteractionQuestionService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * <p>
 * 互动提问的问题表 前端控制器
 * </p>
 *
 * @author sefy
 * @since 2024-04-19
 */
@RestController
@Api(tags = "互动问题相关接口")
@RequestMapping("/questions")
@RequiredArgsConstructor
public class InteractionQuestionController {

    private final IInteractionQuestionService questionService;

    @PostMapping
    @ApiOperation("新增互动问题")
    public void saveQuestion(@RequestBody @Validated QuestionFormDTO dto) {
        questionService.saveQuestion(dto);
    }

    @PutMapping("{id}")
    @ApiOperation("修改互动问题")
    public void updateQuestion(@PathVariable(value = "id") Long id, @RequestBody QuestionFormDTO dto) {
        questionService.updateQuestion(id,dto);
    }

    @GetMapping("page")
    @ApiOperation("分页查询互动问题-用户端")
    public PageDTO<QuestionVO> queryQuestionPage(QuestionPageQuery query){
        return questionService.queryQuestionPage(query);
    }

    @ApiOperation("查询问题详情-用户端")
    @GetMapping("{id}")
    public QuestionVO queryQuestionById(@PathVariable("id") Long id){
        return questionService.queryQuestionById(id);
    }
}
