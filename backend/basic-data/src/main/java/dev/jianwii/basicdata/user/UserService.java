package dev.jianwii.basicdata.user;

import dev.jianwii.api.basicdata.UserQueryParameter;
import dev.jianwii.basicdata.role.RoleDAO;
import dev.jianwii.common.exception.BusinessException;
import dev.jianwii.common.exception.ForbiddenException;
import dev.jianwii.common.service.BaseService;
import dev.jianwii.entity.basicdata.Permission;
import dev.jianwii.entity.basicdata.Role;
import dev.jianwii.entity.basicdata.User;
import org.hibernate.Hibernate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/** 使用者查詢、登入同步、指派角色與載入權限。 */
@Service
public class UserService extends BaseService<User, UserQueryParameter> {
    private final UserDAO userDAO;
    private final RoleDAO roleDAO;

    public UserService(UserDAO userDAO, RoleDAO roleDAO) {
        super(userDAO);
        this.userDAO = userDAO;
        this.roleDAO = roleDAO;
    }

    /**
     * 登入同步：依 subject 找使用者，沒有就建立（沒有任何角色）；有就以 authentik 的資料更新。
     * 資料沒變時 JPA 不會送出 UPDATE，transTime 也不會變。
     */
    @Transactional
    public User login(User login) {
        String name = StringUtils.hasText(login.getName()) ? login.getName()
                : StringUtils.hasText(login.getUsername()) ? login.getUsername() : login.getSubject();
        User user = userDAO.findBySubject(login.getSubject()).orElse(null);
        if (user == null) {
            login.setName(name);
            // 角色只能由管理員指派，忽略呼叫端帶來的值
            login.setRoleIds(new HashSet<>());
            return create(login);
        }
        if (user.getDbMark() != User.VALID) {
            throw new ForbiddenException("帳號已停用");
        }
        user.setUsername(login.getUsername());
        user.setName(name);
        user.setEmail(login.getEmail());
        User saved = userDAO.saveAndFlush(user);
        initialize(saved);
        return saved;
    }

    /** 指派角色：整批取代為 roleIds，角色必須都是有效的。 */
    @Transactional
    public User updateRoles(Long id, Set<Long> roleIds) {
        User user = find(id);
        requireRoles(roleIds);
        user.getRoleIds().clear();
        user.getRoleIds().addAll(roleIds);
        // 只改中間表時主表不會 UPDATE，強制更新修改者與時間
        touch(user);
        User saved = userDAO.saveAndFlush(user);
        initialize(saved);
        return saved;
    }

    /** 使用者所有有效角色的權限 code 聯集；已從 {@link Permission} 移除的 code 不回傳。 */
    @Transactional(readOnly = true)
    public Set<String> permissions(Long id) {
        User user = userDAO.findById(id).filter(found -> found.getDbMark() == User.VALID)
                .orElse(null);
        if (user == null || user.getRoleIds().isEmpty()) {
            return Set.of();
        }
        return roleDAO.findAllByIdInAndDbMark(user.getRoleIds(), Role.VALID).stream()
                .flatMap(role -> role.getPermissionCodes().stream())
                .filter(Permission::exists)
                .collect(Collectors.toCollection(TreeSet::new));
    }

    @Override
    protected void initialize(User user) {
        Hibernate.initialize(user.getRoleIds());
    }

    /** roleIds 必須都是有效角色，否則列出不存在的 id。 */
    private void requireRoles(Set<Long> roleIds) {
        if (roleIds == null || roleIds.stream().anyMatch(Objects::isNull)) {
            throw new BusinessException("roleIds 不可為 null");
        }
        if (roleIds.isEmpty()) {
            return;
        }
        Set<Long> found = roleDAO.findAllByIdInAndDbMark(roleIds, Role.VALID).stream()
                .map(Role::getId).collect(Collectors.toSet());
        List<Long> missing = roleIds.stream().filter(id -> !found.contains(id)).sorted().toList();
        if (!missing.isEmpty()) {
            throw new BusinessException("角色不存在：" + missing);
        }
    }
}
