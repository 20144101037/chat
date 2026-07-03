package com.jin.chat.domain.ao;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 修改聊天室入参（字段为空表示不更新）。
 *
 * @author jinshuai
 */
@Data
public class RoomUpdateAO {

    @Size(max = 50, message = "聊天室名称长度不能超过 50 个字符")
    private String name;

    @Size(max = 255, message = "聊天室描述长度不能超过 255 个字符")
    private String description;

    @Min(value = 1, message = "最大用户数至少为 1")
    @Max(value = 100000, message = "最大用户数不能超过 100000")
    private Integer maxUsers;

    /** OPEN / APPROVAL */
    @Pattern(regexp = "OPEN|APPROVAL", message = "加入策略仅支持 OPEN 或 APPROVAL")
    private String joinPolicy;
}
