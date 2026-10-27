package dev.jianwii.web.basicdata;

import dev.jianwii.api.basicdata.api.SerialNumberApi;
import dev.jianwii.api.basicdata.dto.SerialNumberQueryParameter;
import dev.jianwii.common.dto.PageResponse;
import dev.jianwii.common.validation.ValidGroup;
import dev.jianwii.common.web.Result;
import dev.jianwii.entity.basicdata.SerialNumber;
import jakarta.validation.Valid;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 前端用的編號規則維護：呼叫 basic-data 的 {@link SerialNumberApi}，結果包成 Result。 取號（next）只給服務之間使用，不開放給前端。
 */
@RestController
@AllArgsConstructor
@RequestMapping(path = "/api/basic-data/serial-number")
public class WebSerialNumberController {
    private final SerialNumberApi serialNumberApi;

    @PreAuthorize("hasAuthority('serial-number:read')")
    @PostMapping("/query")
    public Result<List<SerialNumber>> query(@Valid @RequestBody SerialNumberQueryParameter parameter) {
        return Result.success(serialNumberApi.query(parameter));
    }

    @PreAuthorize("hasAuthority('serial-number:read')")
    @PostMapping("/queryPage")
    public Result<PageResponse<SerialNumber>> queryPage(@Valid @RequestBody SerialNumberQueryParameter parameter) {
        return Result.success(serialNumberApi.queryPage(parameter));
    }

    @PreAuthorize("hasAuthority('serial-number:write')")
    @PostMapping
    public Result<SerialNumber> create(@Validated(ValidGroup.Create.class) @RequestBody SerialNumber serialNumber) {
        return Result.success(serialNumberApi.create(serialNumber));
    }

    @PreAuthorize("hasAuthority('serial-number:write')")
    @PatchMapping("/{id}")
    public Result<SerialNumber> update(
            @PathVariable Long id, @Validated(ValidGroup.Update.class) @RequestBody SerialNumber serialNumber) {
        return Result.success(serialNumberApi.update(id, serialNumber));
    }

    @PreAuthorize("hasAuthority('serial-number:write')")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        serialNumberApi.delete(id);
        return Result.success();
    }
}
