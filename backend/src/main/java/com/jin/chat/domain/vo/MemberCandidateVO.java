package com.jin.chat.domain.vo;

import lombok.Data;

/**
 * 可拉入聊天室的用户候选项。
 *
 * @author jinshuai
 */
@Data
public class MemberCandidateVO {

    private Long userId;

    private String username;

    private String nickname;
}
