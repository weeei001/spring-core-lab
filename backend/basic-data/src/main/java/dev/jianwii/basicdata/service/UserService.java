package dev.jianwii.basicdata.service;

import dev.jianwii.api.basicdata.dto.UserAuthorization;
import dev.jianwii.api.basicdata.dto.UserQueryParameter;
import dev.jianwii.basicdata.dao.RoleDAO;
import dev.jianwii.basicdata.dao.UserDAO;
import dev.jianwii.common.exception.BusinessException;
import dev.jianwii.common.exception.ForbiddenException;
import dev.jianwii.common.service.BaseService;
import dev.jianwii.entity.basicdata.Permission;
import dev.jianwii.entity.basicdata.Role;
import dev.jianwii.entity.basicdata.User;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.hibernate.Hibernate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

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
     * 登入同步：依 subject 找使用者，沒有就建立（只有預設角色）；有就以 OIDC 提供者的資料更新。 資料沒變時 JPA 不會送出 UPDATE，transTime 也不會變。
     */
    @Transactional
    public User login(User login) {
        String name = StringUtils.hasText(login.getName())
                ? login.getName()
                : StringUtils.hasText(login.getUsername()) ? login.getUsername() : login.getSubject();
        User user = userDAO.findBySubject(login.getSubject()).orElse(null);
        if (user == null) {
            if (userDAO.countDeletedBySubject(login.getSubject()) > 0) {
                throw new ForbiddenException("帳號已停用");
            }
            login.setName(name);
            // 其他角色只能由管理員指派，忽略呼叫端帶來的值
            Set<Long> roleIds = new HashSet<>();
            defaultRoleId().ifPresent(roleIds::add);
            login.setRoleIds(roleIds);
            return create(login);
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

    /** 把預設角色指派給還沒有任何角色的有效使用者，回傳人數；預設角色已刪除時不做事。 */
    @Transactional
    public int assignDefaultRoleToUsersWithoutRoles() {
        Long roleId = defaultRoleId().orElse(null);
        if (roleId == null) {
            return 0;
        }
        List<User> users = userDAO.findAllByRoleIdsIsEmpty();
        // 只改中間表，交易結束時寫入
        users.forEach(user -> user.getRoleIds().add(roleId));
        return users.size();
    }

    /** 使用者所有有效角色的權限 code 聯集；已從 {@link Permission} 移除的 code 不回傳。 */
    @Transactional(readOnly = true)
    public Set<String> permissions(Long id) {
        return authorization(id).permissions();
    }

    @Transactional(readOnly = true)
    public UserAuthorization authorization(Long id) {
        List<String> rows = userDAO.authorization(id);
        if (rows.isEmpty()) {
            return new UserAuthorization(false, Set.of());
        }
        Set<String> permissions = rows.stream()
                .filter(Objects::nonNull)
                .filter(Permission::exists)
                .collect(Collectors.toCollection(TreeSet::new));
        return new UserAuthorization(true, permissions);
    }

    @Override
    protected Specification<User> toSpecification(UserQueryParameter parameter) {
        Specification<User> specification = super.toSpecification(parameter);
        String permission = parameter.getPermissionCode();
        if (permission == null) {
            return specification;
        }
        if (!Permission.exists(permission)) {
            throw new BusinessException("功能權限不存在：" + permission);
        }
        return specification.and((root, query, builder) -> {
            Subquery<Long> eligible = query.subquery(Long.class);
            Root<User> user = eligible.from(User.class);
            Join<User, Long> roleId = user.join("roleIds");
            Root<Role> role = eligible.from(Role.class);
            eligible.select(user.get("id"))
                    .where(
                            builder.equal(user.get("id"), root.get("id")),
                            builder.equal(role.get("id"), roleId),
                            builder.isMember(permission, role.get("permissionCodes")));
            return builder.exists(eligible);
        });
    }

    @Override
    protected void initialize(User user) {
        Hibernate.initialize(user.getRoleIds());
    }

    private Optional<Long> defaultRoleId() {
        return roleDAO.findByCode(Role.DEFAULT_CODE).map(Role::getId);
    }

    /** roleIds 必須都是有效角色，否則列出不存在的 id。 */
    private void requireRoles(Set<Long> roleIds) {
        if (roleIds == null || roleIds.stream().anyMatch(Objects::isNull)) {
            throw new BusinessException("roleIds 不可為 null");
        }
        if (roleIds.isEmpty()) {
            return;
        }
        Set<Long> found =
                roleDAO.findAllByIdIn(roleIds).stream().map(Role::getId).collect(Collectors.toSet());
        List<Long> missing =
                roleIds.stream().filter(id -> !found.contains(id)).sorted().toList();
        if (!missing.isEmpty()) {
            throw new BusinessException("角色不存在：" + missing);
        }
    }
}
