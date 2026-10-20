package dev.jianwii.basicdata.dao;

import dev.jianwii.common.dao.BaseDAO;
import dev.jianwii.entity.basicdata.SerialNumber;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;

public interface SerialNumberDAO extends BaseDAO<SerialNumber, Long> {

    /** 含已軟刪除的資料，與資料表的唯一約束一致。 */
    boolean existsBySerialKey(String serialKey);

    /**
     * 悲觀鎖（SELECT ... FOR UPDATE），交易結束前同一規則的其他取號會等待；取號用。 方法名中的 WithLock 只是方便閱讀，Spring Data 會忽略 find
     * 與 By 之間的文字。
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<SerialNumber> findWithLockBySerialKeyAndDbMark(String serialKey, Integer dbMark);
}
