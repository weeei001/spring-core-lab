package dev.jianwii.borrow.service;

import dev.jianwii.api.equipment.EquipmentQueryParameter;
import dev.jianwii.api.user.UserApi;
import dev.jianwii.borrow.dao.EquipmentDAO;
import dev.jianwii.common.exception.ConflictException;
import dev.jianwii.common.exception.NotFoundException;
import dev.jianwii.common.service.BaseService;
import dev.jianwii.entity.equipment.Equipment;
import dev.jianwii.entity.user.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.Set;

@Service
public class EquipmentService extends BaseService<Equipment, EquipmentQueryParameter> {
    private final EquipmentDAO equipmentDAO;
    private final UserApi userApi;

    public EquipmentService(EquipmentDAO equipmentDAO, UserApi userApi) {
        super(equipmentDAO);
        this.equipmentDAO = equipmentDAO;
        this.userApi = userApi;
    }

    /** 查設備的保管人；使用者不存在時 user-service 回 404，原樣傳回前端。 */
    @Transactional(readOnly = true)
    public User getKeeper(Long id) {
        Equipment equipment = find(id);
        if (equipment.getKeeperId() == null) {
            throw new NotFoundException("此設備沒有保管人");
        }
        return userApi.getById(equipment.getKeeperId());
    }

    @Override
    protected void beforeCreate(Equipment request) {
        requireUniqueCode(request.getCode());
        requireKeeperExists(request.getKeeperId());
    }

    @Override
    protected void beforeUpdate(Equipment entity, Equipment request) {
        if (request.getCode() != null && !request.getCode().equals(entity.getCode())) {
            requireUniqueCode(request.getCode());
        }
        if (!Objects.equals(request.getKeeperId(), entity.getKeeperId())) {
            requireKeeperExists(request.getKeeperId());
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

    /** 使用者不存在時 UserApi 丟 RemoteServiceException（404「查無資料，id：x」）。 */
    private void requireKeeperExists(Long keeperId) {
        if (keeperId != null) {
            userApi.getById(keeperId);
        }
    }
}
