package com.jin.chat.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.jin.chat.domain.ao.MenuAO;
import com.jin.chat.domain.entity.MenuDO;
import com.jin.chat.domain.vo.MenuVO;

import java.util.List;

/**
 * <p>
 * 菜单服务：全量菜单树（管理用）、按用户角色解析可见菜单树、菜单 CRUD。
 * </p>
 *
 * @author jinshuai
 */
public interface MenuService extends IService<MenuDO> {

    /**
     * 全量菜单树（菜单权限管理用）。
     */
    List<MenuVO> listTree();

    /**
     * 按用户所属角色解析其可见菜单树（RBAC）。
     * SYS_ADMIN 可见全部菜单。
     */
    List<MenuVO> listByUser(Long userId);

    MenuDO create(MenuAO ao);

    MenuDO update(Long id, MenuAO ao);

    void delete(Long id);
}
