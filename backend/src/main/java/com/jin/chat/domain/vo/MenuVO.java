package com.jin.chat.domain.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 菜单视图对象（树形）。
 *
 * @author jinshuai
 */
@Data
public class MenuVO {

    private Long id;

    private Long parentId;

    private String menuKey;

    private String name;

    private String path;

    private Integer sort;

    private String menuType;

    private List<MenuVO> children = new ArrayList<>();
}
