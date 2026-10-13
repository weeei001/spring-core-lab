package dev.jianwii.borrow.service;

import dev.jianwii.borrow.dao.EquipmentDAO;
import dev.jianwii.borrow.entity.Equipment;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EquipmentService {
    private final EquipmentDAO equipmentDAO;

    EquipmentService(EquipmentDAO equipmentDAO) {
        this.equipmentDAO = equipmentDAO;
    }

    public List<Equipment> query(){
        return equipmentDAO.findAll();
    }

    public void create(Equipment equipment){
        equipmentDAO.save(equipment);
    }

    public void update(Equipment equipment){
        equipmentDAO.save(equipment);
    }

    public void delete(Equipment equipment){
        equipmentDAO.delete(equipment);
    }
}
