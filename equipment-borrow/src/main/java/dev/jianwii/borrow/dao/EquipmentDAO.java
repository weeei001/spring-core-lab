package dev.jianwii.borrow.dao;

import dev.jianwii.common.dao.BaseDAO;
import dev.jianwii.entity.equipment.Equipment;

import java.util.Optional;

public interface EquipmentDAO extends BaseDAO<Equipment, Long> {
    Optional<Equipment> findByCode(String code);

    boolean existsByCode(String code);
}
