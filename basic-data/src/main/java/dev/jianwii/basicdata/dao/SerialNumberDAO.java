package dev.jianwii.basicdata.dao;

import dev.jianwii.common.dao.BaseDAO;
import dev.jianwii.entity.basicdata.SerialNumber;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;

public interface SerialNumberDAO extends BaseDAO<SerialNumber, Long> {

    boolean existsBySerialKey(String serialKey);

    /** SELECT ... FOR UPDATE，交易結束前同一規則的其他取號會等待；取號用。 */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<SerialNumber> findWithLockBySerialKey(String serialKey);
}
