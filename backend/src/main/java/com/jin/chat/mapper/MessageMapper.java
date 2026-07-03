package com.jin.chat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jin.chat.domain.entity.MessageDO;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * <p>
 * 消息 Mapper。
 * </p>
 *
 * @author jinshuai
 */
@Repository
public interface MessageMapper extends BaseMapper<MessageDO> {

    /**
     * CAS 方式更新消息审核状态，保证并发下仅一次生效。
     *
     * @param id          消息 ID
     * @param targetStatus 目标状态（APPROVED/REJECTED/TIMEOUT）
     * @param reviewerId  审核人（系统超时为 null）
     * @param reviewedAt  审核时间
     * @return 影响行数，0 表示已被其他操作处理
     */
    int updateStatusCas(@Param("id") Long id,
                        @Param("targetStatus") String targetStatus,
                        @Param("reviewerId") Long reviewerId,
                        @Param("reviewedAt") OffsetDateTime reviewedAt);

    /**
     * 查询指定房间已通过消息（按提交时间升序），用于历史加载/断线补齐。
     */
    List<MessageDO> listApprovedByRoom(@Param("roomId") Long roomId,
                                       @Param("beforeId") Long beforeId,
                                       @Param("size") int size);
}
