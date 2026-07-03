import { defineConfig } from 'vite';
import vue from '@vitejs/plugin-vue';

export default defineConfig({
  plugins: [vue()],
  // sockjs-client 依赖全局变量 global，在浏览器 ESM 环境需 polyfill 为 globalThis
  define: {
    global: 'globalThis',
  },
  server: {
    port: 5173,
    proxy: {
      // 显式使用 127.0.0.1，避免 Windows 上 localhost 解析为 IPv6(::1) 导致的 ECONNREFUSED
      '/api': {
        target: 'http://127.0.0.1:8080',
        changeOrigin: true,
      },
      '/ws': {
        target: 'http://127.0.0.1:8080',
        changeOrigin: true,
        ws: true,
      },
    },
  },
});
