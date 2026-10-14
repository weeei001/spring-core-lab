package dev.jianwii.common.service;

import dev.jianwii.common.dao.BaseDAO;
import dev.jianwii.common.dao.QuerySpecifications;
import dev.jianwii.common.dto.PageResponse;
import dev.jianwii.common.entity.BaseEntity;
import dev.jianwii.common.exception.BusinessException;
import dev.jianwii.common.exception.NotFoundException;
import dev.jianwii.common.query.QueryParameter;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Transactional;

import java.beans.PropertyDescriptor;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

/**
 * 基礎 API：query、queryPage、create、update、delete（軟刪除），輸入輸出直接使用 Entity；查詢條件另用 {@link QueryParameter}。
 * 已軟刪除的資料（dbMark = INVALID）查不到、也不能修改或再刪除。
 * XxxService 繼承這個類別並加 @Service，只需提供 DAO；需要檢查時覆寫 before* 方法，
 * 可排序欄位覆寫 {@link #sortableFields()}。多出來的業務方法寫在 XxxService 上。
 */
@Transactional
public abstract class BaseService<E extends BaseEntity, Q extends QueryParameter<E>> {

    /** id、軟刪除標記與審計欄位由系統維護，更新時一律不從 request 複製。 */
    private static final Set<String> MANAGED_PROPERTIES =
            Set.of("id", "dbMark", "createId", "createTime", "transId", "transTime");

    protected final BaseDAO<E, Long> dao;

    protected BaseService(BaseDAO<E, Long> dao) {
        this.dao = dao;
    }

    @Transactional(readOnly = true)
    public List<E> query(Q parameter) {
        if (parameter.getPage() != 1) {
            throw new BusinessException("query 只提供有限選單，page 必須為 1");
        }
        return queryPage(parameter).getItems();
    }

    @Transactional(readOnly = true)
    public PageResponse<E> queryPage(Q parameter) {
        beforeList();
        Specification<E> valid =
                (root, query, builder) -> builder.equal(root.get("dbMark"), BaseEntity.VALID);
        return new PageResponse<>(dao.findAll(valid.and(toSpecification(parameter)),
                PageResponse.pageable(parameter, sortableFields())));
    }

    public E create(E request) {
        // 防止帶 id 時 save 變成 merge，覆蓋到既有資料。
        request.setId(null);
        request.setDbMark(BaseEntity.VALID);
        beforeCreate(request);
        return dao.saveAndFlush(request);
    }

    public E update(Long id, E request) {
        E entity = find(id);
        beforeUpdate(entity, request);
        applyUpdate(request, entity);
        return dao.saveAndFlush(entity);
    }

    /** 軟刪除：只把 dbMark 改為 INVALID，資料仍在資料表中。 */
    public void delete(Long id) {
        E entity = find(id);
        beforeDelete(entity);
        entity.setDbMark(BaseEntity.INVALID);
        dao.saveAndFlush(entity);
    }

    /** 取得有效資料；不存在或已軟刪除時丟 {@link NotFoundException}。 */
    protected E find(Long id) {
        return dao.findById(id).filter(entity -> entity.getDbMark() == BaseEntity.VALID)
                .orElseThrow(() -> new NotFoundException("查無資料，id：" + id));
    }

    /**
     * 把 request 中非 null 的欄位複製到 entity（PATCH 語意），略過 id、dbMark 與審計欄位。
     * 因此無法透過 update 把欄位設為 null；需要時覆寫此方法。
     */
    protected void applyUpdate(E request, E entity) {
        BeanWrapper source = new BeanWrapperImpl(request);
        String[] ignored = Stream.of(source.getPropertyDescriptors()).map(PropertyDescriptor::getName)
                .filter(name -> MANAGED_PROPERTIES.contains(name) || !source.isReadableProperty(
                        name) || source.getPropertyValue(name) == null)
                .toArray(String[]::new);
        BeanUtils.copyProperties(request, entity, ignored);
    }

    /**
     * 查詢條件，預設由 {@link QueryParameter#toQueryConditions} 轉換；軟刪除的過濾由 queryPage 另外加上，這裡不必處理。
     * 需要子查詢、join 等條件清單表達不了的情況時覆寫，以 {@code super.toSpecification(parameter).and(...)} 補上。
     */
    protected Specification<E> toSpecification(Q parameter) {
        return QuerySpecifications.of(parameter.toQueryConditions());
    }

    /** queryPage 允許的排序欄位；未指定排序時固定以 id 遞增。 */
    protected Set<String> sortableFields() {
        return Set.of("id", "createTime", "transTime");
    }

    protected void beforeCreate(E request) {
    }

    protected void beforeUpdate(E entity, E request) {
    }

    protected void beforeDelete(E entity) {
    }

    protected void beforeList() {
    }
}
