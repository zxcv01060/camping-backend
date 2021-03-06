package tw.edu.ntub.imd.camping.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import tw.edu.ntub.birc.common.util.StringUtils;
import tw.edu.ntub.imd.camping.bean.ForgotPasswordBean;
import tw.edu.ntub.imd.camping.bean.UpdatePasswordBean;
import tw.edu.ntub.imd.camping.bean.UserBadRecordBean;
import tw.edu.ntub.imd.camping.bean.UserBean;
import tw.edu.ntub.imd.camping.config.util.SecurityUtils;
import tw.edu.ntub.imd.camping.databaseconfig.enumerate.Experience;
import tw.edu.ntub.imd.camping.databaseconfig.enumerate.UserBadRecordType;
import tw.edu.ntub.imd.camping.dto.CreditCard;
import tw.edu.ntub.imd.camping.exception.form.InvalidFormException;
import tw.edu.ntub.imd.camping.service.UserService;
import tw.edu.ntub.imd.camping.util.http.BindingResultUtils;
import tw.edu.ntub.imd.camping.util.http.ResponseEntityBuilder;
import tw.edu.ntub.imd.camping.util.json.array.ArrayData;
import tw.edu.ntub.imd.camping.util.json.object.ObjectData;
import tw.edu.ntub.imd.camping.validation.CreateUser;
import tw.edu.ntub.imd.camping.validation.UpdateUser;

import javax.validation.Valid;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@AllArgsConstructor
@Tag(name = "User", description = "使用者API")
@RestController
@RequestMapping(path = "/user")
public class UserController {
    private final UserService userService;

    @Operation(
            tags = "User",
            method = "POST",
            summary = "使用者註冊",
            description = "使用者註冊，預設權限為一般使用者",
            responses = @ApiResponse(
                    responseCode = "200",
                    description = "註冊成功",
                    content = @Content(
                            mediaType = "application/json"
                    )
            )
    )
    @PostMapping(path = "")
    public ResponseEntity<String> register(@Validated(CreateUser.class) @RequestBody UserBean userBean, BindingResult bindingResult) {
        BindingResultUtils.validate(bindingResult);
        userService.save(userBean);
        return ResponseEntityBuilder.buildSuccessMessage("註冊成功");
    }

    @Operation(
            tags = "User",
            method = "GET",
            summary = "查詢使用者資訊",
            description = "查詢使用者資訊",
            responses = @ApiResponse(
                    responseCode = "200",
                    description = "查詢成功",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = UserBean.class)
                    )
            )
    )
    @GetMapping(path = "")
    public ResponseEntity<String> getUserInfo() {
        String account = SecurityUtils.getLoginUserAccount();

        Optional<UserBean> optionalUser = userService.getById(account);
        if (optionalUser.isPresent()) {
            UserBean userBean = optionalUser.get();
            ObjectData data = new ObjectData();
            data.add("account", userBean.getAccount());
            data.add("notCompensate", userBean.isNotCompensate());
            data.add("firstName", userBean.getFirstName());
            data.add("lastName", userBean.getLastName());
            data.add("nickName", userBean.getNickName());
            data.add("gender", userBean.getGenderName());
            data.add("birthday", userBean.getBirthday());
            data.add("experience", userBean.getExperience().ordinal());
            data.add("cellPhone", userBean.getCellPhone());
            data.add("email", userBean.getEmail());
            data.add("address", userBean.getAddress());
            data.add("comment", userBean.getComment());
            data.add("bankAccount", userBean.getBankAccount());
            return ResponseEntityBuilder.success("查詢成功")
                    .data(data)
                    .build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(
            tags = "User",
            method = "PATCH",
            summary = "更新使用者資訊",
            description = "更新使用者資訊",
            responses = @ApiResponse(
                    responseCode = "200",
                    description = "更新成功",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE
                    )
            )
    )
    @PatchMapping(path = "")
    public ResponseEntity<String> updateUserInfo(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = UpdateUserInfoSchema.class)
                    )
            ) @Validated(UpdateUser.class) @RequestBody UserBean user,
            BindingResult bindingResult
    ) {
        String account = SecurityUtils.getLoginUserAccount();

        BindingResultUtils.validate(bindingResult);
        userService.update(account, user);
        return ResponseEntityBuilder.buildSuccessMessage("更新成功");
    }

    @Operation(
            tags = "User",
            method = "POST",
            summary = "修改密碼",
            description = "修改密碼",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = UpdatePasswordBean.class)
                    )
            ),
            responses = @ApiResponse(
                    responseCode = "200",
                    description = "修改成功",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE
                    )
            )
    )
    @PatchMapping(path = "/password")
    public ResponseEntity<String> changePassword(
            @Valid @RequestBody UpdatePasswordBean updatePasswordBean,
            BindingResult bindingResult
    ) {
        BindingResultUtils.validate(bindingResult);
        String password = updatePasswordBean.getPassword();
        String newPassword = updatePasswordBean.getNewPassword();
        String account = SecurityUtils.getLoginUserAccount();
        userService.updatePassword(account, password, newPassword);

        return ResponseEntityBuilder.success().message("更新成功").build();
    }

    @Operation(
            tags = "User",
            method = "GET",
            summary = "查詢露營經驗列表",
            description = "查詢露營經驗列表",
            responses = @ApiResponse(
                    responseCode = "200",
                    description = "查詢成功",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array = @ArraySchema(schema = @Schema(implementation = ExperienceSchema.class))
                    )
            )
    )
    @GetMapping(path = "/experience")
    public ResponseEntity<String> getExperienceList() {
        return ResponseEntityBuilder.success("查詢成功")
                .data(Arrays.asList(Experience.values()), this::addExperienceToObjectData)
                .build();
    }

    private void addExperienceToObjectData(ObjectData data, Experience experience) {
        data.add("id", experience.id);
        data.add("display", experience.toString());
    }

    @Operation(
            tags = "User",
            method = "PATCH",
            summary = "啟用使用者",
            description = "啟用使用者",
            responses = @ApiResponse(
                    responseCode = "200",
                    description = "啟用成功"
            )
    )
    @PatchMapping(path = "/{account}/enable")
    @PreAuthorize("hasAnyAuthority('Administrator', 'Manager')")
    public ResponseEntity<String> enable(@PathVariable String account) {
        userService.updateEnable(account, true);
        return ResponseEntityBuilder.buildSuccessMessage("啟用成功");
    }

    @Operation(
            tags = "User",
            method = "PATCH",
            summary = "禁用使用者",
            description = "禁用使用者",
            responses = @ApiResponse(
                    responseCode = "200",
                    description = "禁用成功"
            )
    )
    @PatchMapping(path = "/{account}/disable")
    @PreAuthorize("hasAnyAuthority('Administrator', 'Manager')")
    public ResponseEntity<String> disable(@PathVariable String account) {
        userService.updateEnable(account, false);
        return ResponseEntityBuilder.buildSuccessMessage("禁用成功");
    }

    @GetMapping(path = "/{account}/comment-and-bad-record")
    public ResponseEntity<String> searchBadRecord(@PathVariable String account) {
        ObjectData data = new ObjectData();
        addBadRecordData(account, data);
        data.add("comment", userService.getComment(account));
        return ResponseEntityBuilder.success("查詢成功")
                .data(data)
                .build();
    }

    private void addBadRecordData(String account, ObjectData data) {
        ArrayData badRecordArray = data.addArray("badRecordArray");
        for (UserBadRecordBean userBadRecord : userService.getBadRecord(account)) {
            ObjectData badRecordData = badRecordArray.addObject();
            UserBadRecordType type = userBadRecord.getType();
            badRecordData.add("type", type.ordinal());
            badRecordData.add("typeName", type.toString());
            badRecordData.add("count", userBadRecord.getCount());
        }
    }

    @GetMapping(path = "/bad-record")
    public ResponseEntity<String> searchBadRecordType() {
        return ResponseEntityBuilder.success("查詢成功")
                .data(List.of(UserBadRecordType.values()), (data, type) -> {
                    data.add("id", type.ordinal());
                    data.add("name", type.toString());
                })
                .build();
    }

    @PatchMapping(path = "/compensate")
    public ResponseEntity<String> compensate(@Valid @RequestBody CreditCard creditCard) {
        userService.compensate(SecurityUtils.getLoginUserAccount(), creditCard);
        return ResponseEntityBuilder.buildSuccessMessage("已賠償成功");
    }

    @PostMapping(path = "/forgot-password")
    public ResponseEntity<String> forgotPassword(@Valid @RequestBody ForgotPasswordBean forgotPasswordBean, BindingResult bindingResult) {
        BindingResultUtils.validate(bindingResult);
        userService.forgotPassword(forgotPasswordBean);
        return ResponseEntityBuilder.buildSuccessMessage("已寄送驗證信至您的信箱中，請前往信箱收信");
    }

    @PatchMapping(path = "/forgot-password/change")
    public ResponseEntity<String> forgotPassword(
            @RequestBody String requestBodyJsonString
    ) {
        ObjectData requestBody = new ObjectData(requestBodyJsonString);
        String token = requestBody.getString("token");
        String newPassword = requestBody.getString("newPassword");
        if (StringUtils.isBlank(token)) {
            throw new InvalidFormException("未填寫驗證碼");
        } else if (StringUtils.isBlank(newPassword)) {
            throw new InvalidFormException("未填寫新密碼");
        }
        userService.updatePasswordForForgotPassword(token, newPassword);
        return ResponseEntityBuilder.buildSuccessMessage("密碼已修改完成");
    }

    // |---------------------------------------------------------------------------------------------------------------------------------------------|
    // |----------------------------------------------------------以下為Swagger所需使用的Schema---------------------------------------------------------|
    // |---------------------------------------------------------------------------------------------------------------------------------------------|

    @Schema(name = "更新使用者資訊", description = "更新使用者資訊")
    @Data
    private static class UpdateUserInfoSchema {
        @Schema(description = "露營經驗(0: 新手/ 1: 有過幾次經驗)", type = "int", example = "0")
        private Experience experience;

        @Schema(description = "暱稱", example = "煞氣a小明")
        private String nickName;

        @Schema(description = "信箱", example = "10646000@ntub.edu.tw")
        private String email;

        @Schema(description = "地址", example = "台北市中正區濟南路321號")
        private String address;
    }

    @Schema(name = "露營經驗", description = "露營經驗")
    @Data
    private static class ExperienceSchema {
        @Schema(description = "編號", example = "0")
        private String id;

        @Schema(description = "顯示文字", example = "0~5次 新手")
        private String display;
    }
}
