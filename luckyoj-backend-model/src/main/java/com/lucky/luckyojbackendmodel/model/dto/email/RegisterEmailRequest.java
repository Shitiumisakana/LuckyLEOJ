package com.lucky.luckyojbackendmodel.model.dto.email;

import lombok.Data;

import javax.validation.constraints.NotEmpty;


@Data
public class RegisterEmailRequest {

    private static final long serialVersionUID = 3191241716373120793L;
    /**
     * 邮箱
     */
    private String email;

    /**
     * 用户密码
     */
    private String userPassword;

    /**
     * 确认密码
     */
    @NotEmpty(message = "确认密码不能为空")
    private String checkPassword;

    /**
     * 验证码
     */
    @NotEmpty(message = "验证码不能为空")
    private String code;
}
