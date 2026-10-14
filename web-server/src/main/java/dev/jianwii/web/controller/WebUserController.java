package dev.jianwii.web.controller;

import dev.jianwii.api.basicdata.UserApi;
import dev.jianwii.api.basicdata.UserQueryParameter;
import dev.jianwii.common.dto.PageResponse;
import dev.jianwii.common.web.Result;
import dev.jianwii.entity.basicdata.User;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 前端用的使用者查詢：呼叫 basic-data 的 {@link UserApi}，結果包成 Result。
 * 主要用來把 createId、transId 換成名字；登入同步（login）只給 web-server 內部使用，不開放。
 */
@RestController
@AllArgsConstructor
@RequestMapping(path = "/api/basic-data/user")
public class WebUserController {
    private final UserApi userApi;

    @PostMapping("/query")
    public Result<List<User>> query(@Valid @RequestBody UserQueryParameter parameter) {
        return Result.success(userApi.query(parameter));
    }

    @PostMapping("/queryPage")
    public Result<PageResponse<User>> queryPage(@Valid @RequestBody UserQueryParameter parameter) {
        return Result.success(userApi.queryPage(parameter));
    }
}
