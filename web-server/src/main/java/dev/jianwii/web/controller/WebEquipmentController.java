package dev.jianwii.web.controller;

import dev.jianwii.api.equipment.EquipmentApi;
import dev.jianwii.api.equipment.EquipmentQueryParameter;
import dev.jianwii.common.dto.PageResponse;
import dev.jianwii.common.validation.ValidGroup;
import dev.jianwii.common.web.Result;
import dev.jianwii.entity.equipment.Equipment;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 前端用的設備 API：呼叫 equipment-borrow 的 {@link EquipmentApi}，包成 Result。
 * 這裡先驗證一次，格式錯誤不必送到後端服務；後端錯誤由 RemoteServiceException 原樣轉回。
 */
@RestController
@AllArgsConstructor
@RequestMapping(path = "/api/equipment")
public class WebEquipmentController {
    private final EquipmentApi equipmentApi;

    @GetMapping
    public Result<List<Equipment>> getAll() {
        return Result.success(equipmentApi.getAll());
    }

    @GetMapping("/{id}")
    public Result<Equipment> getById(@PathVariable Long id) {
        return Result.success(equipmentApi.getById(id));
    }

    @PostMapping("/query")
    public Result<List<Equipment>> query(@Valid @RequestBody EquipmentQueryParameter parameter) {
        return Result.success(equipmentApi.query(parameter));
    }

    @PostMapping("/queryPage")
    public Result<PageResponse<Equipment>> queryPage(
            @Valid @RequestBody EquipmentQueryParameter parameter) {
        return Result.success(equipmentApi.queryPage(parameter));
    }

    @PostMapping
    public Result<Equipment> create(
            @Validated(ValidGroup.Create.class) @RequestBody Equipment equipment) {
        return Result.success(equipmentApi.create(equipment));
    }

    @PatchMapping("/{id}")
    public Result<Equipment> update(@PathVariable Long id,
            @Validated(ValidGroup.Update.class) @RequestBody Equipment equipment) {
        return Result.success(equipmentApi.update(id, equipment));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        equipmentApi.delete(id);
        return Result.success();
    }
}
