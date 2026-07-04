<template>
  <div class="page">
    <PageHeader title="菜单权限管理" subtitle="维护系统菜单结构与路由" icon="Menu">
      <template #actions>
        <el-button :icon="Refresh" @click="load">刷新</el-button>
        <el-button type="success" :icon="Plus" @click="openCreate(null)">新增顶级菜单</el-button>
      </template>
    </PageHeader>

    <el-card shadow="never">
    <el-table :data="tree" v-loading="loading" row-key="id" default-expand-all
              :tree-props="{ children: 'children' }" style="width:100%" empty-text="暂无菜单">
      <el-table-column prop="name" label="菜单名称" min-width="180" />
      <el-table-column prop="menuKey" label="标识" min-width="140" />
      <el-table-column prop="path" label="路由" min-width="160" />
      <el-table-column label="类型" width="90">
        <template #default="{ row }">
          <el-tag :type="row.menuType === 'DIR' ? 'warning' : ''">{{ row.menuType === 'DIR' ? '目录' : '菜单' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="sort" label="排序" width="80" />
      <el-table-column label="操作" width="240">
        <template #default="{ row }">
          <el-button v-if="row.menuType === 'DIR'" type="success" link :icon="Plus" @click="openCreate(row)">添加子菜单</el-button>
          <el-button type="primary" link :icon="Edit" @click="openEdit(row)">修改</el-button>
          <el-button type="danger" link :icon="Delete" @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    </el-card>

    <el-dialog v-model="dialog" :title="editingId ? '修改菜单' : '新增菜单'" width="440px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="上级菜单">
          <el-select v-model="form.parentId" style="width:100%">
            <el-option label="顶级" :value="0" />
            <el-option v-for="d in dirOptions" :key="d.id" :label="d.name" :value="d.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="form.menuType" style="width:100%">
            <el-option label="菜单" value="MENU" />
            <el-option label="目录" value="DIR" />
          </el-select>
        </el-form-item>
        <el-form-item label="名称"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="标识"><el-input v-model="form.menuKey" placeholder="唯一，如 rooms、system:xxx" /></el-form-item>
        <el-form-item label="路由"><el-input v-model="form.path" placeholder="目录可留空，如 /app/xxx" /></el-form-item>
        <el-form-item label="排序"><el-input-number v-model="form.sort" :min="0" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog = false">取消</el-button>
        <el-button type="primary" @click="submitForm">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { Refresh, Plus, Edit, Delete } from '@element-plus/icons-vue';
import { adminApi } from '../../api';
import PageHeader from '../../components/PageHeader.vue';

const loading = ref(false);
const tree = ref([]);

const dialog = ref(false);
const editingId = ref(null);
const form = reactive({ parentId: 0, menuType: 'MENU', name: '', menuKey: '', path: '', sort: 0 });

const dirOptions = computed(() => tree.value.filter((m) => m.menuType === 'DIR'));

async function load() {
  loading.value = true;
  try {
    tree.value = await adminApi.menuTree();
  } finally {
    loading.value = false;
  }
}

function openCreate(parent) {
  editingId.value = null;
  Object.assign(form, {
    parentId: parent ? parent.id : 0,
    menuType: 'MENU', name: '', menuKey: '', path: '', sort: 0,
  });
  dialog.value = true;
}

function openEdit(row) {
  editingId.value = row.id;
  Object.assign(form, {
    parentId: row.parentId || 0,
    menuType: row.menuType,
    name: row.name,
    menuKey: row.menuKey,
    path: row.path || '',
    sort: row.sort || 0,
  });
  dialog.value = true;
}

async function submitForm() {
  if (!form.name?.trim() || !form.menuKey?.trim()) {
    ElMessage.warning('请填写菜单名称与标识');
    return;
  }
  if (editingId.value) {
    await adminApi.menuUpdate(editingId.value, { ...form });
    ElMessage.success('修改成功');
  } else {
    await adminApi.menuCreate({ ...form });
    ElMessage.success('新增成功');
  }
  dialog.value = false;
  load();
}

async function remove(row) {
  await ElMessageBox.confirm(`确定删除菜单「${row.name}」吗？`, '删除确认', { type: 'warning' });
  await adminApi.menuDelete(row.id);
  ElMessage.success('删除成功');
  load();
}

onMounted(load);
</script>

<style scoped>
.toolbar { display: flex; gap: 10px; margin-bottom: 16px; }
</style>
