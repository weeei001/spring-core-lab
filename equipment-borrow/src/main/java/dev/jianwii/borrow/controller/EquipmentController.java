package dev.jianwii.borrow.controller;

import dev.jianwii.api.equipment.EquipmentApi;
import dev.jianwii.api.equipment.EquipmentQueryParameter;
import dev.jianwii.borrow.service.EquipmentService;
import dev.jianwii.common.dto.PageResponse;
import dev.jianwii.entity.equipment.Equipment;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 內部 API，路徑、參數與驗證都定義在 {@link EquipmentApi}，這裡只實作。
 */
@RestController
@AllArgsConstructor
public class EquipmentController implements EquipmentApi {
    private final EquipmentService equipmentService;

    @Override
    public List<Equipment> getAll() {
        return equipmentService.getAll();
    }

    @Override
    public Equipment getById(Long id) {
        return equipmentService.getById(id);
    }

    @Override
    public List<Equipment> query(EquipmentQueryParameter parameter) {
        return equipmentService.query(parameter);
    }

    @Override
    public PageResponse<Equipment> queryPage(EquipmentQueryParameter parameter) {
        return equipmentService.queryPage(parameter);
    }

    @Override
    public Equipment create(Equipment equipment) {
        return equipmentService.create(equipment);
    }

    @Override
    public Equipment update(Long id, Equipment equipment) {
        return equipmentService.update(id, equipment);
    }

    @Override
    public void delete(Long id) {
        equipmentService.deleteById(id);
    }
}
