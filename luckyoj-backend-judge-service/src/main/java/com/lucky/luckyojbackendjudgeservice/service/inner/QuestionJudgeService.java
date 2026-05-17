package com.lucky.luckyojbackendjudgeservice.service.inner;

import com.lucky.luckyojbackendmodel.model.dto.question.QuestionRunRequest;
import com.lucky.luckyojbackendmodel.model.entity.User;
import com.lucky.luckyojbackendmodel.model.vo.QuestionRunResult;


public interface QuestionJudgeService {
    QuestionRunResult doQuestionRun(QuestionRunRequest questionRunRequest, User loginUser);
}
