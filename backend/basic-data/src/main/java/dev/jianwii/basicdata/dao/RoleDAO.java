package dev.jianwii.basicdata.dao;

import dev.jianwii.common.dao.BaseDAO;
import dev.jianwii.entity.basicdata.Role;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface RoleDAO extends BaseDAO<Role, Long> {

    /** 含已軟刪除的資料，與資料表的唯一約束一致。 */
    boolean existsByCode(String code);

    Optional<Role> findByCodeAndDbMark(String code, Integer dbMark);

    /** 指派角色、載入權限時，找出其中有效的角色。 */
    List<Role> findAllByIdInAndDbMark(Collection<Long> ids, Integer dbMark);
}
