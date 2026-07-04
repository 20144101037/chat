<template>
  <div class="page">
    <PageHeader title="角色管理" subtitle="维护角色并为其分配菜单权限" icon="UserFilled">
      <template #actions>
        <el-input v-model="query.keyword" placeholder="搜索角色编码/名称" style="width:200px" clearable :prefix-icon="Search" @keyup.enter="load" />
        <el-button type="primary" :icon="Search" @click="load">查询</el-button>
        <el-button type="success" :icon="Plus" @click="openCreate">新增角色</el-button>
      </template>
    </PageHeader>

    <el-card shadow="never">
    <el-table :data="records" v-loading="loading" style="width:100%" empty-text="暂无角色">
      <el-table-column prop="roleCode" label="角色编码" width="160" />
      <el-table-column prop="roleName" label="角色名称" width="160" />
      <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
      <el-table-column label="类型" width="90">
        <template #default="{ row }">
          <el-tag :type="row.builtIn ? 'info' : ''">{{ row.builtIn ? '内置' : '自定义' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="240">
        <template #default="{ row }">
          <el-button type="primary" link :icon="Menu" @click="openMenus(row)">分配菜单</el-button>
          <el-button type="primary" link :icon="Edit" @click="openEdit(row)">修改</el-button>
          <el-button type="danger" link :icon="Delete" :disabled="row.builtIn" @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      style="margin-top:16px;justify-content:flex-end"
      layout="prev, pager, next, total, sizes"
      :total="total"
      :page-size="query.pageSize"
      :current-page="query.pageNo"
      :page-sizes="[10, 20, 50]"
      @current-change="onPageChange"
      @size-change="onSizeChange"
    />
    </el-card>

    <!-- 新增/修改角色 -->
    <el-dialog v-model="dialog" :title="editingId ? '修改角色' : '新增角色'" width="420px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="编码"><el-input v-model="form.roleCode" :disabled="editingBuiltIn" /></el-form-item>
        <el-form-item label="名称"><el-input v-model="form.roleName" /></el-form-item>
        <el-form-item label="描述"><el-input v-model="form.description" type="textarea" :rows="2" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog = false">取消</el-button>
        <el-button type="primary" @click="submitForm">确定</el-button>
      </template>
    </el-dialog>

    <!-- 分配菜单 -->
    <el-dialog v-model="menuDialog" :title="`分配菜单 - ${current?.roleName || ''}`" width="460px">
      <el-tree
        ref="treeRef"
        :data="menuTree"
        show-checkbox
        node-key="id"
        :props="{ label: 'name', children: 'children' }"
        default-expand-all
      />
      <template #footer>
        <el-button @click="menuDialog = false">取消</el-button>
        <el-button type="primary" @click="saveMenus">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, nextTick } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { Search, Plus, Menu, Edit, Delete } from '@element-plus/icons-vue';
import { adminApi } from '../../api';
import PageHeader from '../../components/PageHeader.vue';

const loading = ref(false);
const records = ref([]);
const total = ref(0);
const query = reactive({ keyword: '', pageNo: 1, pageSize: 10 });

const dialog = ref(false);
const editingId = ref(null);
const editingBuiltIn = ref(false);
const form = reactive({ roleCode: '', roleName: '', description: '' });

const menuDialog = ref(false);
const menuTree = ref([]);
const current = ref(null);
const treeRef = ref(null);

async function load() {
  loading.value = true;
  try {
    const res = await adminApi.rolePage(query);
    records.value = res.records;
    total.value = res.total;
  } finally {
    loading.value = false;
  }
}

function onPageChange(p) { query.pageNo = p; load(); }
function onSizeChange(s) { query.pageSize = s; query.pageNo = 1; load(); }

function openCreate() {
  editingId.value = null;
  editingBuiltIn.value = false;
  Object.assign(form, { roleCode: '', roleName: '', description: '' });
  dialog.value = true;
}

function openEdit(row) {
  editingId.value = row.id;
  editingBuiltIn.value = !!row.builtIn;
  Object.assign(form, { roleCode: row.roleCode, roleName: row.roleName, description: row.description || '' });
  dialog.value = true;
}

async function submitForm() {
  if (!form.roleCode?.trim() || !form.roleName?.trim()) {
    ElMessage.warning('请填写角色编码与名称');
    return;
  }
  if (editingId.value) {
    await adminApi.roleUpdate(editingId.value, { ...form });
    ElMessage.success('修改成功');
  } else {
    await adminApi.roleCreate({ ...form });
    ElMessage.success('新增成功');
  }
  dialog.value = false;
  load();
}

async function remove(row) {
  await ElMessageBox.confirm(`确定删除角色「${row.roleName}」吗？`, '删除确认', { type: 'warning' });
  await adminApi.roleDelete(row.id);
  ElMessage.success('删除成功');
  load();
}

async function openMenus(row) {
  current.value = row;
  if (!menuTree.value.length) {
    menuTree.value = await adminApi.menuTree();
  }
  const checkedIds = await adminApi.roleMenus(row.id);
  menuDialog.value = true;
  // 等待弹框内 tree 渲染完成后再设置选中态
  await nextTick();
  treeRef.value?.setCheckedKeys(checkedIds);
}

async function saveMenus() {
  // 仅保存完全勾选的节点；父目录在后端解析用户菜单时会自动补齐祖先
  const ids = treeRef.value.getCheckedKeys();
  await adminApi.assignRoleMenus(current.value.id, ids);
  ElMessage.success('菜单权限已更新');
  menuDialog.value = false;
}

onMounted(load);
</script>

<style scoped>
.toolbar { display: flex; gap: 10px; margin-bottom: 16px; }
</style>
