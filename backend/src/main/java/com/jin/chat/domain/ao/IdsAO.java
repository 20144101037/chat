package com.jin.chat.domain.ao;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 通用 ID 集合入参（用于分配角色/菜单等）。
 *
 * @author jinshuai
 */
@Data
public class IdsAO {

    private List<Long> ids = new ArrayList<>();
}
