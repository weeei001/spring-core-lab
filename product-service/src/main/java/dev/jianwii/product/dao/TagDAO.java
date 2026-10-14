package dev.jianwii.product.dao;

import dev.jianwii.common.dao.BaseDAO;
import dev.jianwii.entity.product.Tag;

import java.util.Collection;
import java.util.List;

public interface TagDAO extends BaseDAO<Tag, Long> {

    /** 商品設定 tagIds 時，找出其中有效的標籤。 */
    List<Tag> findAllByIdInAndDbMark(Collection<Long> ids, Integer dbMark);
}
