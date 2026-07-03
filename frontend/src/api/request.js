import axios from 'axios';
import { ElMessage } from 'element-plus';
import router from '../router';

const request = axios.create({
  baseURL: '/api',
  timeout: 10000,
});

request.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

request.interceptors.response.use(
  (response) => {
    const res = response.data;
    if (res.code !== 0) {
      ElMessage.error(res.message || '请求失败');
      if (res.code === 2000 || res.code === 2004) {
        localStorage.clear();
        router.push('/login');
      }
      return Promise.reject(new Error(res.message));
    }
    return res.data;
  },
  (error) => {
    const status = error.response?.status;
    if (status === 401) {
      localStorage.clear();
      router.push('/login');
    }
    ElMessage.error(error.message || '网络异常');
    return Promise.reject(error);
  }
);

export default request;
