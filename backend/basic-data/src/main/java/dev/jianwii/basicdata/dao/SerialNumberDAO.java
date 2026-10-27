package dev.jianwii.basicdata.dao;

import dev.jianwii.common.dao.BaseDAO;
import dev.jianwii.entity.basicdata.SerialNumber;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SerialNumberDAO extends BaseDAO<SerialNumber, Long> {

    /** 含已軟刪除的資料，與資料表的唯一約束一致；JPQL 會被 @SoftDelete 過濾，所以用原生 SQL。 */
    @Query(value = "select count(*) from serial_number where serial_key = :serialKey", nativeQuery = true)
    long countIncludingDeletedBySerialKey(@Param("serialKey") String serialKey);

    /**
     * 悲觀鎖（SELECT ... FOR UPDATE），交易結束前同一規則的其他取號會等待；取號用。 方法名中的 WithLock 只是方便閱讀，Spring Data 會忽略 find
     * 與 By 之間的文字。
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<SerialNumber> findWithLockBySerialKey(String serialKey);
}
