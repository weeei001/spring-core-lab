package dev.jianwii.basicdata.role;

import dev.jianwii.api.basicdata.RoleQueryParameter;
import dev.jianwii.basicdata.user.UserDAO;
import dev.jianwii.common.exception.BusinessException;
import dev.jianwii.common.exception.ConflictException;
import dev.jianwii.common.service.BaseService;
import dev.jianwii.entity.basicdata.Permission;
import dev.jianwii.entity.basicdata.Role;
import dev.jianwii.entity.basicdata.User;
import org.hibernate.Hibernate;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** 角色 CRUD；權限 code 必須是 {@link Permission} 定義過的，有使用者擁有時不可刪除。 */
@Service
public class RoleService extends BaseService<Role, RoleQueryParameter> {
    private final RoleDAO roleDAO;
    // 用 DAO 而非 UserService，避免兩個 Service 互相注入
    private final UserDAO userDAO;

    public RoleService(RoleDAO roleDAO, UserDAO userDAO) {
        super(roleDAO);
        this.roleDAO = roleDAO;
        this.userDAO = userDAO;
    }

    @Override
    protected void beforeCreate(Role request) {
        if (roleDAO.existsByCode(request.getCode())) {
            throw new ConflictException("角色代碼已存在（含已刪除）：" + request.getCode());
        }
        requirePermissions(request.getPermissionCodes());
        if (request.getPermissionCodes() == null) {
            request.setPermissionCodes(new HashSet<>());
        }
    }

    @Override
    protected void beforeUpdate(Role entity, Role request) {
        if (request.getCode() != null && !request.getCode().equals(entity.getCode())) {
            throw new BusinessException("角色代碼建立後不可修改");
        }
        if (request.getPermissionCodes() != null) {
            requirePermissions(request.getPermissionCodes());
            // 只改權限時主表不會 UPDATE，強制更新修改者與時間
            touch(entity);
        }
    }

    @Override
    protected void beforeDelete(Role entity) {
        if (userDAO.existsByRoleIdsContainingAndDbMark(entity.getId(), User.VALID)) {
            throw new ConflictException("仍有使用者擁有此角色：" + entity.getName());
        }
    }

    @Override
    protected void initialize(Role role) {
        Hibernate.initialize(role.getPermissionCodes());
    }

    /** 權限 code 必須都在 {@link Permission} 中，否則列出不存在的 code。 */
    private void requirePermissions(Set<String> codes) {
        if (codes == null) {
            return;
        }
        List<String> unknown = codes.stream().filter(code -> !Permission.exists(code)).sorted().toList();
        if (!unknown.isEmpty()) {
            throw new BusinessException("權限不存在：" + unknown);
        }
    }
}
