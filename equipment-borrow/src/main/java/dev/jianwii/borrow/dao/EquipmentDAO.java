package dev.jianwii.borrow.dao;

import dev.jianwii.borrow.entity.Equipment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EquipmentDAO extends JpaRepository<Equipment, Long> {
    Optional<Equipment> findByCode(String code);
}
