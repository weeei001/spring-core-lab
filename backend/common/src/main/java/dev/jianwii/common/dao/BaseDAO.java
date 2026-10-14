package dev.jianwii.common.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.NoRepositoryBean;

import java.io.Serializable;
import java.util.List;

/**
 * 共用 DAO（Spring Data JPA Repository），XxxDAO 繼承此介面即可。
 * 多繼承 {@link JpaSpecificationExecutor} 是為了用 QueryParameter 組出的動態條件查詢。
 * update* 只是 save* 的別名，讓呼叫端讀起來清楚是更新。
 * *InBatch 刪除會直接下 SQL、略過 JPA 的 Entity Listener（審計欄位不會更新），因此停用。
 */
@NoRepositoryBean
public interface BaseDAO<E, ID extends Serializable>
        extends
            JpaRepository<E, ID>,
            JpaSpecificationExecutor<E> {

    default <S extends E> S update(S entity) {
        return save(entity);
    }

    default <S extends E> List<S> updateAll(Iterable<S> entities) {
        return saveAll(entities);
    }

    default <S extends E> S updateAndFlush(S entity) {
        return saveAndFlush(entity);
    }

    @Override
    default void deleteAllInBatch() {
        throw new UnsupportedOperationException("Bulk DML bypasses entity listeners");
    }

    @Override
    default void deleteAllInBatch(Iterable<E> entities) {
        throw new UnsupportedOperationException("Bulk DML bypasses entity listeners");
    }

    @Override
    default void deleteAllByIdInBatch(Iterable<ID> ids) {
        throw new UnsupportedOperationException("Bulk DML bypasses entity listeners");
    }
}
