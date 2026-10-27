package dev.jianwii.basicdata.dao;

import dev.jianwii.common.dao.BaseDAO;
import dev.jianwii.entity.basicdata.Role;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RoleDAO extends BaseDAO<Role, Long> {

    /** 含已軟刪除的資料，與資料表的唯一約束一致；JPQL 會被 @SoftDelete 過濾，所以用原生 SQL。 */
    @Query(value = "select count(*) from sys_role where code = :code", nativeQuery = true)
    long countIncludingDeletedByCode(@Param("code") String code);

    Optional<Role> findByCode(String code);

    /** 指派角色、載入權限時，找出其中有效的角色。 */
    List<Role> findAllByIdIn(Collection<Long> ids);
}
