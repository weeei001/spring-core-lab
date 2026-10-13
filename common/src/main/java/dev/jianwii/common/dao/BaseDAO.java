package dev.jianwii.common.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.NoRepositoryBean;

import java.io.Serializable;
import java.util.List;

/**
 * 共用 DAO，內建 {@link JpaSpecificationExecutor} 供 QueryParameter 查詢。
 * 批次 DML 會略過 Entity Listener（審計欄位不會更新），因此停用。
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
