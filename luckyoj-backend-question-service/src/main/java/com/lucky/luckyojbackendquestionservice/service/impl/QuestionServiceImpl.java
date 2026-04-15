package com.lucky.luckyojbackendquestionservice.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lucky.luckyojbackendcommon.common.ErrorCode;
import com.lucky.luckyojbackendcommon.constant.CommonConstant;
import com.lucky.luckyojbackendcommon.exception.BusinessException;
import com.lucky.luckyojbackendcommon.exception.ThrowUtils;
import com.lucky.luckyojbackendcommon.utils.SqlUtils;
import com.lucky.luckyojbackendmodel.model.dto.question.JudgeConfig;
import com.lucky.luckyojbackendmodel.model.dto.question.ProblemQueryRequest;
import com.lucky.luckyojbackendmodel.model.dto.question.QuestionQueryRequest;
import com.lucky.luckyojbackendmodel.model.entity.Question;
import com.lucky.luckyojbackendmodel.model.entity.QuestionSubmit;
import com.lucky.luckyojbackendmodel.model.entity.User;
import com.lucky.luckyojbackendmodel.model.enums.QuestionSubmitStatusEnum;
import com.lucky.luckyojbackendmodel.model.vo.QuestionVO;
import com.lucky.luckyojbackendmodel.model.vo.SafeQuestionVo;
import com.lucky.luckyojbackendmodel.model.vo.UserVO;
import com.lucky.luckyojbackendquestionservice.mapper.QuestionMapper;
import com.lucky.luckyojbackendquestionservice.mapper.QuestionSubmitMapper;
import com.lucky.luckyojbackendquestionservice.service.QuestionService;
import com.lucky.luckyojbackendserviceclient.service.UserFeignClient;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
* @description 针对表【question(题目)】的数据库操作Service实现
* @createDate 2023-08-07 20:58:00
*/
@Service
public class QuestionServiceImpl extends ServiceImpl<QuestionMapper, Question>
    implements QuestionService {

    @Resource
    private QuestionSubmitMapper questionSubmitMapper;

    @Resource
    private UserFeignClient userFeignClient;

    /**
     * 校验题目是否合法
     * @param question
     * @param add
     */
    @Override
    public void validQuestion(Question question, boolean add) {
        if (question == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        String title = question.getTitle();
        String content = question.getContent();
        String tags = question.getTags();
        String answer = question.getAnswer();
        String judgeCase = question.getJudgeCase();
        String judgeConfig = question.getJudgeConfig();
        // 创建时，参数不能为空
        if (add) {
            ThrowUtils.throwIf(StringUtils.isAnyBlank(title, content, tags), ErrorCode.PARAMS_ERROR);
        }
        // 有参数则校验
        if (StringUtils.isNotBlank(title) && title.length() > 80) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "标题过长");
        }
        if (StringUtils.isNotBlank(content) && content.length() > 8192) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "内容过长");
        }
        if (StringUtils.isNotBlank(answer) && answer.length() > 8192) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "答案过长");
        }
        if (StringUtils.isNotBlank(judgeCase) && judgeCase.length() > 8192) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "判题用例过长");
        }
        if (StringUtils.isNotBlank(judgeConfig) && judgeConfig.length() > 8192) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "判题配置过长");
        }
    }

    /**
     * 获取查询包装类（用户根据哪些字段查询，根据前端传来的请求对象，得到 mybatis 框架支持的查询 QueryWrapper 类）
     *
     * @param questionQueryRequest
     * @return
     */
    @Override
    public QueryWrapper<Question> getQueryWrapper(QuestionQueryRequest questionQueryRequest) {
        QueryWrapper<Question> queryWrapper = new QueryWrapper<>();
        if (questionQueryRequest == null) {
            return queryWrapper;
        }
        Long id = questionQueryRequest.getId();
        String title = questionQueryRequest.getTitle();
        String content = questionQueryRequest.getContent();
        List<String> tags = questionQueryRequest.getTags();
        String answer = questionQueryRequest.getAnswer();
        Long userId = questionQueryRequest.getUserId();
        String sortField = questionQueryRequest.getSortField();
        String sortOrder = questionQueryRequest.getSortOrder();

        // 拼接查询条件
        queryWrapper.like(StringUtils.isNotBlank(title), "title", title);
        queryWrapper.like(StringUtils.isNotBlank(content), "content", content);
        queryWrapper.like(StringUtils.isNotBlank(answer), "answer", answer);
        if (CollectionUtils.isNotEmpty(tags)) {
            for (String tag : tags) {
                queryWrapper.like("tags", "\"" + tag + "\"");
            }
        }
        queryWrapper.eq(ObjectUtils.isNotEmpty(id), "id", id);
        queryWrapper.eq(ObjectUtils.isNotEmpty(userId), "userId", userId);
        queryWrapper.eq("isDelete", false);
        queryWrapper.orderBy(SqlUtils.validSortField(sortField), sortOrder.equals(CommonConstant.SORT_ORDER_ASC),
                sortField);
        return queryWrapper;
    }

    @Override
    public Wrapper<Question> getProblemQueryWrapper(ProblemQueryRequest problemQueryRequest,HttpServletRequest httpServletRequest) {
        QueryWrapper<Question> queryWrapper = new QueryWrapper<>();

        String sortField = problemQueryRequest.getSortField();
        String sortOrder = problemQueryRequest.getSortOrder();
        String keyword = problemQueryRequest.getKeyword();
        String status = problemQueryRequest.getStatus();
        String difficulty = problemQueryRequest.getDifficulty();
        List<String> tags = problemQueryRequest.getTags();

        //不查询content和answer，因为很多时候不显示
        queryWrapper.select(Question.class, item -> !item.getColumn().equals("content") && !item.getColumn().equals("answer"));

        if(StrUtil.isNotBlank(status) && !status.equals("全部")){
            User loginUser = userFeignClient.getLoginUser(httpServletRequest);
            Set<Long> passedIds;
            Set<Long> triedIds;
            Long userId = loginUser.getId();

            switch (status){
                case "已通过":
                    // 只有包含 "Accepted" 的记录才算真正通过
                    passedIds = questionSubmitMapper.selectList(new LambdaQueryWrapper<QuestionSubmit>()
                                    .select(QuestionSubmit::getQuestionId)
                                    .eq(QuestionSubmit::getUserId, userId)
                                    .eq(QuestionSubmit::getStatus, QuestionSubmitStatusEnum.SUCCEED.getValue())
                                    .like(QuestionSubmit::getJudgeInfo, "\"message\":\"Accepted\""))
                            .stream().map(QuestionSubmit::getQuestionId).collect(Collectors.toSet());                   if(passedIds.isEmpty()){
                        return null;
                    }
                    queryWrapper.in("id", passedIds);
                    break;
                case "尝试过":
                    // 尝试过 = 有提交记录，但没有任何一条记录是 "Accepted"
                    // 先查出所有有过提交记录的题目 ID
                    Set<Long> allAttemptIds = questionSubmitMapper.selectList(new LambdaQueryWrapper<QuestionSubmit>()
                                    .select(QuestionSubmit::getQuestionId)
                                    .eq(QuestionSubmit::getUserId, userId))
                            .stream().map(QuestionSubmit::getQuestionId).collect(Collectors.toSet());

                    // 再查出所有真正通过的题目 ID
                    Set<Long> realPassedIds = questionSubmitMapper.selectList(new LambdaQueryWrapper<QuestionSubmit>()
                                    .select(QuestionSubmit::getQuestionId)
                                    .eq(QuestionSubmit::getUserId, userId)
                                    .eq(QuestionSubmit::getStatus, QuestionSubmitStatusEnum.SUCCEED.getValue())
                                    .like(QuestionSubmit::getJudgeInfo, "\"message\":\"Accepted\""))
                            .stream().map(QuestionSubmit::getQuestionId).collect(Collectors.toSet());

                    // 差集：有过提交但没通过的题目
                    Set<Long> triedOnlyIds = (Set<Long>) CollUtil.subtract(allAttemptIds, realPassedIds);

                    if(triedOnlyIds.isEmpty()){
                        return null;
                    }
                    queryWrapper.in("id", triedOnlyIds);
                    break;
                case "未开始":
                    // 3. 未开始 = 没有任何提交记录的题目
                    Set<Long> anyAttemptIds = questionSubmitMapper.selectList(new LambdaQueryWrapper<QuestionSubmit>()
                                    .select(QuestionSubmit::getQuestionId)
                                    .eq(QuestionSubmit::getUserId, userId))
                            .stream().map(QuestionSubmit::getQuestionId).collect(Collectors.toSet());
                    if(!anyAttemptIds.isEmpty()){
                        queryWrapper.notIn("id", anyAttemptIds);
                    }
                    break;
            }
        }

        // 使用 lambda 表达式实现 OR 嵌套
        if (StringUtils.isNotBlank(keyword)) {
            queryWrapper.and(qw -> qw.like("title", keyword)
                    .or()
                    .like("content", keyword)
                    .or()
                    .like("answer", keyword));
        }
        if (com.alibaba.nacos.common.utils.CollectionUtils.isNotEmpty(tags)) {
            for (String tag : tags) {
                queryWrapper.like("tags", "\"" + tag + "\"");
            }
        }
        queryWrapper.eq(StringUtils.isNotBlank(difficulty), "difficulty", difficulty);

        queryWrapper.orderBy(SqlUtils.validSortField(sortField), sortOrder.equals(CommonConstant.SORT_ORDER_ASC), sortField);
        return queryWrapper;
    }

    @Override
    public QuestionVO getQuestionVO(Question question, HttpServletRequest request) {
        QuestionVO questionVO = QuestionVO.objToVo(question);
        // 1. 关联查询用户信息
        Long userId = question.getUserId();
        User user = null;
        if (userId != null && userId > 0) {
            user = userFeignClient.getById(userId);
        }
        UserVO userVO = userFeignClient.getUserVO(user);
        questionVO.setUserVO(userVO);
        return questionVO;
    }

    @Override
    public Page<QuestionVO> getQuestionVOPage(Page<Question> questionPage, HttpServletRequest request) {
        List<Question> questionList = questionPage.getRecords();
        Page<QuestionVO> questionVOPage = new Page<>(questionPage.getCurrent(), questionPage.getSize(), questionPage.getTotal());
        if (CollectionUtils.isEmpty(questionList)) {
            return questionVOPage;
        }
        // 1. 关联查询用户信息
        Set<Long> userIdSet = questionList.stream().map(Question::getUserId).collect(Collectors.toSet());
        Map<Long, List<User>> userIdUserListMap = userFeignClient.listByIds(userIdSet).stream()
                .collect(Collectors.groupingBy(User::getId));
        // 填充信息
        List<QuestionVO> questionVOList = questionList.stream().map(question -> {
            QuestionVO questionVO = QuestionVO.objToVo(question);
            Long userId = question.getUserId();
            User user = null;
            if (userIdUserListMap.containsKey(userId)) {
                user = userIdUserListMap.get(userId).get(0);
            }
            questionVO.setUserVO(userFeignClient.getUserVO(user));
            return questionVO;
        }).collect(Collectors.toList());
        questionVOPage.setRecords(questionVOList);
        return questionVOPage;
    }

    @Override
    public List<String> getProblemTags() {
        return lambdaQuery().select(Question::getTags).list().stream()
                .flatMap(problem -> JSONUtil.toList(problem.getTags(), String.class).stream())
                .distinct().collect(Collectors.toList());
    }

    @Override
    public SafeQuestionVo objToVO(Question question, Long id) {
        if (question == null) {
            return null;
        }
        SafeQuestionVo safeQuestionVo = new SafeQuestionVo();
        BeanUtils.copyProperties(question, safeQuestionVo);
        safeQuestionVo.setTags(JSONUtil.toList(question.getTags(), String.class));
        safeQuestionVo.setJudgeConfig(JSONUtil.toBean(question.getJudgeConfig(), JudgeConfig.class));

        // 1. 先查询该用户是否在这道题上有“Accepted”的成功记录
        Long passCount = questionSubmitMapper.selectCount(new QueryWrapper<QuestionSubmit>().lambda()
                .eq(QuestionSubmit::getQuestionId, question.getId())
                .eq(QuestionSubmit::getUserId, id)
                .eq(QuestionSubmit::getStatus, QuestionSubmitStatusEnum.SUCCEED.getValue())
                // 核心修复：必须包含 Accepted 关键字
                .like(QuestionSubmit::getJudgeInfo, "\"message\":\"Accepted\""));

        if (passCount > 0) {
            safeQuestionVo.setStatus("已通过");
        } else {
            // 2. 如果没有通过，再查有没有提交记录（只要有记录，就是“尝试过”）
            Long attemptCount = questionSubmitMapper.selectCount(new QueryWrapper<QuestionSubmit>().lambda()
                    .eq(QuestionSubmit::getQuestionId, question.getId())
                    .eq(QuestionSubmit::getUserId, id));

            if (attemptCount > 0) {
                safeQuestionVo.setStatus("尝试过"); // 这里对应前端红叉的逻辑
            } else {
                safeQuestionVo.setStatus("未开始");
            }
        }

        return safeQuestionVo;
    }

}




