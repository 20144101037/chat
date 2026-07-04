package com.jin.chat.domain.ao;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 菜单新增/修改入参。
 *
 * @author jinshuai
 */
@Data
public class MenuAO {

    /** 父菜单 ID，0 表示顶级 */
    private Long parentId = 0L;

    @NotBlank(message = "菜单标识不能为空")
    private String menuKey;

    @NotBlank(message = "菜单名称不能为空")
    private String name;

    private String path;

    private Integer sort = 0;

    /** DIR / MENU */
    private String menuType = "MENU";
}
