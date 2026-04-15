package com.lucky.luckyojbackendjudgeservice.controller;

import com.lucky.luckyojbackendcommon.common.BaseResponse;
import com.lucky.luckyojbackendcommon.common.ErrorCode;
import com.lucky.luckyojbackendcommon.common.ResultUtils;
import com.lucky.luckyojbackendcommon.exception.BusinessException;
import com.lucky.luckyojbackendjudgeservice.service.inner.QuestionJudgeService;
import com.lucky.luckyojbackendmodel.model.dto.question.QuestionRunRequest;
import com.lucky.luckyojbackendmodel.model.entity.User;
import com.lucky.luckyojbackendmodel.model.vo.QuestionRunResult;
import com.lucky.luckyojbackendserviceclient.service.UserFeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

/**
 * 暴露给前端使用的端口
 *
 * @author: 七海鱼
 * Time: 2026/3/26 13:05
 * Description:
 */
@RestController
@RequestMapping("/")
public class JudgeController {
    @Resource
    private UserFeignClient userFeignClient;

    @Resource
    private QuestionJudgeService questionJudgeService;
    @PostMapping("/run")
    public BaseResponse<QuestionRunResult> doProblemRun(@RequestBody QuestionRunRequest questionRunRequest, HttpServletRequest httpServletRequest) {
        User loginUser = userFeignClient.getLoginUser(httpServletRequest);
        if(loginUser == null){
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        return ResultUtils.success(questionJudgeService.doQuestionRun(questionRunRequest, loginUser));
    }
}
