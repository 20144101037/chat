<template>
  <div class="page config">
    <PageHeader title="全局配置" subtitle="系统预置参数的运行期动态调整" icon="SetUp">
      <template #actions>
        <el-input v-model="query.keyword" placeholder="搜索配置键" style="width:180px" clearable :prefix-icon="Search" @keyup.enter="load" />
        <el-input v-model="query.configGroup" placeholder="分组" style="width:140px" clearable @keyup.enter="load" />
        <el-button type="primary" :icon="Search" @click="load">查询</el-button>
      </template>
    </PageHeader>

    <el-alert
      type="info"
      :closable="false"
      show-icon
      title="全局配置由系统预置，仅支持修改配置值，不支持新增或删除。"
      style="margin-bottom:16px"
    />

    <el-card shadow="never">
    <el-table :data="records" v-loading="loading" style="width:100%" empty-text="暂无配置">
      <el-table-column prop="configGroup" label="分组" width="120">
        <template #default="{ row }"><el-tag>{{ row.configGroup }}</el-tag></template>
      </el-table-column>
      <el-table-column prop="configKey" label="配置键" min-width="180" />
      <el-table-column prop="configValue" label="配置值" min-width="160" show-overflow-tooltip />
      <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
      <el-table-column label="操作" width="100">
        <template #default="{ row }">
          <el-button type="primary" link :disabled="row.editable === false" @click="openEdit(row)">修改</el-button>
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

    <el-dialog v-model="dialog" title="修改配置" width="460px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="配置键"><el-input v-model="form.configKey" disabled /></el-form-item>
        <el-form-item label="分组"><el-input v-model="form.configGroup" disabled /></el-form-item>
        <el-form-item label="描述"><el-input v-model="form.description" type="textarea" :rows="2" disabled /></el-form-item>
        <el-form-item label="配置值" prop="configValue">
          <el-input v-model="form.configValue" maxlength="1000" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog = false">取消</el-button>
        <el-button type="primary" @click="submitForm">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue';
import { ElMessage } from 'element-plus';
import { Search } from '@element-plus/icons-vue';
import { configApi } from '../api';
import PageHeader from '../components/PageHeader.vue';

const loading = ref(false);
const records = ref([]);
const total = ref(0);
const query = reactive({ keyword: '', configGroup: '', pageNo: 1, pageSize: 10 });

const dialog = ref(false);
const editingId = ref(null);
const formRef = ref(null);
const form = reactive({ configKey: '', configValue: '', configGroup: '', description: '' });

const rules = {
  configValue: [
    { required: true, message: '配置值不能为空', trigger: 'blur' },
    { max: 1000, message: '配置值长度不能超过 1000 个字符', trigger: 'blur' },
  ],
};

async function load() {
  loading.value = true;
  try {
    const res = await configApi.page(query);
    records.value = res.records;
    total.value = res.total;
  } finally {
    loading.value = false;
  }
}

function onPageChange(p) {
  query.pageNo = p;
  load();
}
function onSizeChange(s) {
  query.pageSize = s;
  query.pageNo = 1;
  load();
}

function openEdit(row) {
  editingId.value = row.id;
  Object.assign(form, {
    configKey: row.configKey,
    configValue: row.configValue,
    configGroup: row.configGroup,
    description: row.description || '',
  });
  dialog.value = true;
}

async function submitForm() {
  await formRef.value.validate();
  await configApi.update(editingId.value, { configValue: form.configValue });
  ElMessage.success('修改成功');
  dialog.value = false;
  load();
}

onMounted(load);
</script>

<style scoped>
.toolbar { display: flex; gap: 10px; margin-bottom: 16px; }
</style>
