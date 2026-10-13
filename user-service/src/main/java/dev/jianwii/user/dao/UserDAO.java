package dev.jianwii.user.dao;

import dev.jianwii.common.dao.BaseDAO;
import dev.jianwii.entity.user.User;

public interface UserDAO extends BaseDAO<User, Long> {
    boolean existsByUsername(String username);
}
