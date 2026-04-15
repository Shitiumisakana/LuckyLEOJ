package com.lucky.luckyojbackendjudgeservice.service.inner.impl;

import com.lucky.luckyojbackendjudgeservice.judge.codesandbox.CodeSandbox;
import com.lucky.luckyojbackendjudgeservice.judge.codesandbox.CodeSandboxFactory;
import com.lucky.luckyojbackendjudgeservice.service.inner.QuestionJudgeService;
import com.lucky.luckyojbackendmodel.model.codesandbox.ExecuteCodeRequest;
import com.lucky.luckyojbackendmodel.model.codesandbox.ExecuteCodeResponse;
import com.lucky.luckyojbackendmodel.model.dto.question.QuestionRunRequest;
import com.lucky.luckyojbackendmodel.model.entity.User;
import com.lucky.luckyojbackendmodel.model.enums.ExecuteCodeStatusEnum;
import com.lucky.luckyojbackendmodel.model.vo.QuestionRunResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * @author: 小飞的电脑
 * @Date: 2023/10/8 - 10 - 08 - 16:53
 * @Description: com.lucky.luckyojbackendjudgeservice.service.inner.impl
 * @version: 1.0
 */
@Service
@Slf4j
public class QuestionJudgeServiceImpl implements QuestionJudgeService {
    @Value("${codesandbox.type:remote}")
    private String type;

    @Override
    public QuestionRunResult doQuestionRun(QuestionRunRequest questionRunRequest, User loginUser) {
        String code = questionRunRequest.getCode();
        String language = questionRunRequest.getLanguage();
        List<String> inputList = Collections.singletonList(questionRunRequest.getInput());

        CodeSandbox codeSandbox = CodeSandboxFactory.newInstance(type);
        ExecuteCodeRequest executeCodeRequest = ExecuteCodeRequest.builder()
                .code(code)
                .language(language)
                .inputList(inputList)
                .build();
        ExecuteCodeResponse response = codeSandbox.executeCode(executeCodeRequest);

        log.info("========== 判题链路追踪 ==========");
        log.info("1. 沙箱原始响应: {}", response);

        Integer sandBoxStatus = response.getStatus();
        Integer successValue = ExecuteCodeStatusEnum.SUCCESS.getValue();

        log.info("2. 类型对比检查: 沙箱Status类型={}, 枚举Success类型={}",
                (sandBoxStatus != null ? sandBoxStatus.getClass().getSimpleName() : "null"),
                (successValue != null ? successValue.getClass().getSimpleName() : "null"));

        log.info("3. 数值对比检查: 沙箱Status={}, 枚举SuccessValue={}", sandBoxStatus, successValue);
        log.info("4. 比较结果: {}", (sandBoxStatus != null && sandBoxStatus.equals(successValue)));

        QuestionRunResult questionRunResult = new QuestionRunResult();
        questionRunResult.setInput(questionRunRequest.getInput());
        //执行成功
        if(response.getStatus().equals(ExecuteCodeStatusEnum.SUCCESS.getValue())){
            log.info("5. 命中逻辑: [执行成功]");
            questionRunResult.setCode(ExecuteCodeStatusEnum.SUCCESS.getValue());
            // 不要用 .toString()，改用 String.join
            List<String> outputList = response.getOutputList();
            if (outputList != null && !outputList.isEmpty()) {
                // 用换行符连接多个输出结果，这样既去掉了方括号，也能支持多行输出
                String pureOutput = String.join("\n", outputList);
                questionRunResult.setOutput(pureOutput);
            } else {
                questionRunResult.setOutput("");
            }
        } else if(response.getStatus().equals(ExecuteCodeStatusEnum.RUN_FAILED.getValue())){
            log.info("5. 命中逻辑: [运行失败]");
            questionRunResult.setCode(ExecuteCodeStatusEnum.RUN_FAILED.getValue());
            questionRunResult.setOutput(response.getMessage());
        } else if(response.getStatus().equals(ExecuteCodeStatusEnum.COMPILE_FAILED.getValue())){
            log.info("5. 命中逻辑: [编译失败]");
            questionRunResult.setCode(ExecuteCodeStatusEnum.COMPILE_FAILED.getValue());
            questionRunResult.setOutput(response.getMessage());
        } else {
            log.warn("5. 警告: 未命中任何已知枚举状态！");
            questionRunResult.setCode(sandBoxStatus);
            questionRunResult.setOutput("未知状态响应，详情：" + response.getMessage());
        }

        log.info("6. 最终封装对象: {}", questionRunResult);
        log.info("==================================");

        System.out.println("沙箱执行完成");

        return questionRunResult;
    }
}
