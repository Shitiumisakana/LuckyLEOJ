package com.lucky.luckyojbackendmodel.model.dto.email;

import lombok.Data;


@Data
public class LoginEmailRequest {
    private String email;

    private String code;
}
