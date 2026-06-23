package com.campus.management.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserForm {

    @NotBlank(message = "用户名不能为空")
    @Size(min = 4, max = 20, message = "用户名长度需在4到20位之间")
    private String username;

    @NotBlank(message = "真实姓名不能为空")
    @Size(max = 20, message = "真实姓名长度不能超过20位")
    private String realName;

    @NotBlank(message = "角色不能为空")
    private String role;

    @Size(max = 20, message = "密码长度不能超过20位")
    private String password;
}
