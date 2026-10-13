package dev.jianwii.borrow.controller;

import dev.jianwii.borrow.entity.Equipment;
import dev.jianwii.borrow.service.EquipmentService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@AllArgsConstructor
@RequestMapping(path = "/api/equipment")
public class EquipmentController {
    private final EquipmentService equipmentService;

    @GetMapping
    public ResponseEntity<List<Equipment>> getEquipments() {
        List<Equipment> equipmentList = equipmentService.query();
        return ResponseEntity.ok(equipmentList);
    }

    @PostMapping("")
    public ResponseEntity<Void> createEquipment(@RequestBody Equipment equipment) {
        equipmentService.create(equipment);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteEquipment(@RequestBody Equipment equipment) {
        equipmentService.delete(equipment);
        return ResponseEntity.ok().build();
    }
}
