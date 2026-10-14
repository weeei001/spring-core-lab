package dev.jianwii.borrow.service;

import dev.jianwii.api.equipment.EquipmentQueryParameter;
import dev.jianwii.borrow.dao.EquipmentDAO;
import dev.jianwii.common.exception.ConflictException;
import dev.jianwii.common.service.BaseService;
import dev.jianwii.entity.equipment.Equipment;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
public class EquipmentService extends BaseService<Equipment, EquipmentQueryParameter> {
    private final EquipmentDAO equipmentDAO;

    public EquipmentService(EquipmentDAO equipmentDAO) {
        super(equipmentDAO);
        this.equipmentDAO = equipmentDAO;
    }

    @Override
    protected void beforeCreate(Equipment request) {
        requireUniqueCode(request.getCode());
    }

    @Override
    protected void beforeUpdate(Equipment entity, Equipment request) {
        if (request.getCode() != null && !request.getCode().equals(entity.getCode())) {
            requireUniqueCode(request.getCode());
        }
    }

    @Override
    protected Set<String> sortableFields() {
        return Set.of("id", "code", "name", "createTime", "transTime");
    }

    private void requireUniqueCode(String code) {
        if (equipmentDAO.existsByCode(code)) {
            throw new ConflictException("設備代碼已存在：" + code);
        }
    }
}
