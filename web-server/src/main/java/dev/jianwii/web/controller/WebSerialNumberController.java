package dev.jianwii.web.controller;

import dev.jianwii.api.basicdata.SerialNumberApi;
import dev.jianwii.api.basicdata.SerialNumberQueryParameter;
import dev.jianwii.common.dto.PageResponse;
import dev.jianwii.common.validation.ValidGroup;
import dev.jianwii.common.web.Result;
import dev.jianwii.entity.basicdata.SerialNumber;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 前端用的編號規則維護：呼叫 basic-data 的 {@link SerialNumberApi}，包成 Result。
 * 取號（next）只給服務之間使用，不開放給前端。
 */
@RestController
@AllArgsConstructor
@RequestMapping(path = "/api/basic-data/serial-number")
public class WebSerialNumberController {
    private final SerialNumberApi serialNumberApi;

    @PostMapping("/query")
    public Result<List<SerialNumber>> query(
            @Valid @RequestBody SerialNumberQueryParameter parameter) {
        return Result.success(serialNumberApi.query(parameter));
    }

    @PostMapping("/queryPage")
    public Result<PageResponse<SerialNumber>> queryPage(
            @Valid @RequestBody SerialNumberQueryParameter parameter) {
        return Result.success(serialNumberApi.queryPage(parameter));
    }

    @PostMapping
    public Result<SerialNumber> create(
            @Validated(ValidGroup.Create.class) @RequestBody SerialNumber serialNumber) {
        return Result.success(serialNumberApi.create(serialNumber));
    }

    @PatchMapping("/{id}")
    public Result<SerialNumber> update(@PathVariable Long id,
            @Validated(ValidGroup.Update.class) @RequestBody SerialNumber serialNumber) {
        return Result.success(serialNumberApi.update(id, serialNumber));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        serialNumberApi.delete(id);
        return Result.success();
    }
}
