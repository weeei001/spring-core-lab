package dev.jianwii.user.service;

import dev.jianwii.api.user.UserQueryParameter;
import dev.jianwii.common.exception.BusinessException;
import dev.jianwii.common.exception.ConflictException;
import dev.jianwii.common.service.BaseService;
import dev.jianwii.entity.user.User;
import dev.jianwii.user.dao.UserDAO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
public class UserService extends BaseService<User, UserQueryParameter> {
    private static final int MAX_BATCH = 100;

    private final UserDAO userDAO;

    public UserService(UserDAO userDAO) {
        super(userDAO);
        this.userDAO = userDAO;
    }

    /** 批次查詢；不存在的 id 直接略過，回傳順序不保證與 ids 相同。 */
    @Transactional(readOnly = true)
    public List<User> getByIds(List<Long> ids) {
        if (ids.size() > MAX_BATCH) {
            throw new BusinessException("一次最多查詢 " + MAX_BATCH + " 筆");
        }
        return userDAO.findAllById(ids.stream().distinct().toList());
    }

    @Override
    protected void beforeCreate(User request) {
        requireUniqueUsername(request.getUsername());
    }

    @Override
    protected void beforeUpdate(User entity, User request) {
        if (request.getUsername() != null && !request.getUsername().equals(entity.getUsername())) {
            requireUniqueUsername(request.getUsername());
        }
    }

    @Override
    protected Set<String> sortableFields() {
        return Set.of("id", "username", "name", "createTime", "transTime");
    }

    private void requireUniqueUsername(String username) {
        if (userDAO.existsByUsername(username)) {
            throw new ConflictException("帳號已存在：" + username);
        }
    }
}
