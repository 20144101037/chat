<template>
  <div class="page-header">
    <div class="ph-left">
      <div v-if="icon" class="ph-icon"><el-icon><component :is="icon" /></el-icon></div>
      <div class="ph-text">
        <h2 class="ph-title">{{ title }}</h2>
        <p v-if="subtitle" class="ph-sub">{{ subtitle }}</p>
      </div>
    </div>
    <div class="ph-actions"><slot name="actions" /></div>
  </div>
</template>

<script setup>
defineProps({
  title: { type: String, required: true },
  subtitle: { type: String, default: '' },
  icon: { type: String, default: '' },
});
</script>

<style scoped>
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 20px;
}
.ph-left { display: flex; align-items: center; gap: 14px; }
.ph-icon {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 22px;
  background: var(--brand-gradient);
  box-shadow: 0 6px 16px rgba(91, 124, 250, 0.35);
  flex-shrink: 0;
}
.ph-text { min-width: 0; }
.ph-title { margin: 0; font-size: 20px; font-weight: 700; color: var(--text-main); }
.ph-sub { margin: 2px 0 0; font-size: 13px; color: var(--text-secondary); }
.ph-actions { display: flex; gap: 10px; align-items: center; flex-wrap: wrap; }
</style>
