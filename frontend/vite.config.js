import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import legacy from '@vitejs/plugin-legacy'

// https://vite.dev/config/
export default defineConfig({
  plugins: [
    vue(),
    legacy({
      targets: ['defaults', 'not dead', 'iOS >= 11', 'ChromeAndroid >= 64'],
      modernPolyfills: true,
    }),
  ],
  build: {
    target: 'es2015',
    cssTarget: 'chrome61',
  },
  server: {
    host: '127.0.0.1', // 明确绑定到本地，确保可通过 127.0.0.1:5173 访问
    port: 5173,
    strictPort: false, // 若 5173 被占用可自动尝试下一端口
    allowedHosts: true,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
      '/uploads': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
      // WebSocket 代理：前端用当前域名的 /ws/chat 即可，Vite 转发到后端 8080
      '/ws': {
        target: 'ws://localhost:8080',
        ws: true,
        changeOrigin: true,
      },
    },
  },
})
