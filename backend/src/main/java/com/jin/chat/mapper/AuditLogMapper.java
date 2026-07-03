package com.jin.chat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jin.chat.domain.entity.AuditLogDO;
import org.springframework.stereotype.Repository;

/**
 * <p>
 * 审核日志 Mapper。
 * </p>
 *
 * @author jinshuai
 */
@Repository
public interface AuditLogMapper extends BaseMapper<AuditLogDO> {
}
