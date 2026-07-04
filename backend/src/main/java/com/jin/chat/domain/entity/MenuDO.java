package com.jin.chat.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 菜单。parentId=0 表示顶级。
 *
 * @author jinshuai
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("menus")
public class MenuDO extends BaseDO {

    private Long parentId;

    /** 唯一标识键，如 rooms、system:users */
    private String menuKey;

    private String name;

    /** 前端路由路径，目录可为空 */
    private String path;

    private Integer sort;

    /** DIR 目录 / MENU 菜单 */
    private String menuType;
}
