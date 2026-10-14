package dev.jianwii.common.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import dev.jianwii.common.exception.BusinessException;
import dev.jianwii.common.query.QueryParameter;
import lombok.Getter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

@Getter
public class PageResponse<T> {

    private final List<T> items;
    private final int page;
    private final int size;
    private final long total;

    public <E> PageResponse(Page<E> source, Function<E, T> mapping) {
        this.items = source.getContent().stream().map(mapping).toList();
        // Spring Data 從 0 起算，對外統一從 1 起算。
        this.page = source.getNumber() + 1;
        this.size = source.getSize();
        this.total = source.getTotalElements();
    }

    /** 不轉型，直接回傳查到的物件（例如 Entity）。 */
    public PageResponse(Page<T> source) {
        this(source, Function.identity());
    }

    /** 呼叫端透過 XxxApi 收到 JSON 時還原用。 */
    @JsonCreator
    public PageResponse(@JsonProperty("items") List<T> items, @JsonProperty("page") int page,
            @JsonProperty("size") int size, @JsonProperty("total") long total) {
        this.items = items;
        this.page = page;
        this.size = size;
        this.total = total;
    }

    public static Pageable pageable(QueryParameter<?> parameter, Set<String> allowed) {
        if (parameter.getPage() < 1 || parameter.getSize() < 1 || parameter.getSize() > 100
                || (long) (parameter.getPage() - 1) * parameter.getSize() > Integer.MAX_VALUE) {
            throw new BusinessException("page 從 1 起算，size 介於 1 至 100");
        }
        List<Sort.Order> orders = new ArrayList<>();
        for (QueryParameter.Order order : parameter.getSort()) {
            if (!allowed.contains(order.getField()) || !Set.of("asc", "desc")
                    .contains(order.getDirection())) {
                throw new BusinessException("不支援的排序欄位或方向");
            }
            orders.add(new Sort.Order(
                    order.getDirection().equals("desc") ? Sort.Direction.DESC : Sort.Direction.ASC,
                    order.getField()));
        }
        if (orders.stream().noneMatch(order -> order.getProperty().equals("id"))) {
            orders.add(Sort.Order.asc("id"));
        }
        return PageRequest.of(parameter.getPage() - 1, parameter.getSize(), Sort.by(orders));
    }

}
