package com.jin.chat.common.api;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.Data;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * <p>
 * 统一分页结果。
 * </p>
 *
 * @author jinshuai
 */
@Data
public class PageResult<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    private long total;
    private long pageNo;
    private long pageSize;
    private List<T> records;

    public static <T> PageResult<T> of(long total, long pageNo, long pageSize, List<T> records) {
        PageResult<T> result = new PageResult<>();
        result.setTotal(total);
        result.setPageNo(pageNo);
        result.setPageSize(pageSize);
        result.setRecords(records == null ? Collections.emptyList() : records);
        return result;
    }

    /**
     * 将 MyBatis-Plus 的 IPage&lt;E&gt; 转换为 PageResult&lt;T&gt;
     */
    public static <E, T> PageResult<T> from(IPage<E> page, Function<E, T> converter) {
        List<T> records = page.getRecords().stream().map(converter).collect(Collectors.toList());
        return of(page.getTotal(), page.getCurrent(), page.getSize(), records);
    }
}
