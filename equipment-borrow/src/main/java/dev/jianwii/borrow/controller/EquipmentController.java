package dev.jianwii.borrow.controller;

import dev.jianwii.api.equipment.EquipmentQueryParameter;
import dev.jianwii.borrow.service.EquipmentService;
import dev.jianwii.common.dto.PageResponse;
import dev.jianwii.common.validation.ValidGroup;
import dev.jianwii.common.web.Result;
import dev.jianwii.entity.equipment.Equipment;
import dev.jianwii.entity.user.User;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@AllArgsConstructor
@RequestMapping(path = "/api/equipment")
public class EquipmentController {
    private final EquipmentService equipmentService;

    @GetMapping
    public Result<List<Equipment>> getAll() {
        return Result.success(equipmentService.getAll());
    }

    @GetMapping("/{id}")
    public Result<Equipment> getById(@PathVariable Long id) {
        return Result.success(equipmentService.getById(id));
    }

    /** 跨服務查詢範例：從 user-service 取得保管人資料。 */
    @GetMapping("/{id}/keeper")
    public Result<User> getKeeper(@PathVariable Long id) {
        return Result.success(equipmentService.getKeeper(id));
    }

    @PostMapping("/query")
    public Result<List<Equipment>> query(@Valid @RequestBody EquipmentQueryParameter parameter) {
        return Result.success(equipmentService.query(parameter));
    }

    @PostMapping("/queryPage")
    public Result<PageResponse<Equipment>> queryPage(
            @Valid @RequestBody EquipmentQueryParameter parameter) {
        return Result.success(equipmentService.queryPage(parameter));
    }

    @PostMapping
    public Result<Equipment> create(
            @Validated(ValidGroup.Create.class) @RequestBody Equipment equipment) {
        return Result.success(equipmentService.create(equipment));
    }

    @PatchMapping("/{id}")
    public Result<Equipment> update(@PathVariable Long id,
            @Validated(ValidGroup.Update.class) @RequestBody Equipment equipment) {
        return Result.success(equipmentService.update(id, equipment));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        equipmentService.deleteById(id);
        return Result.success();
    }
}
