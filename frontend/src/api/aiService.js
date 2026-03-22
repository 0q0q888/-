import axios from 'axios'

/**
 * 发送用户消息到 AI 客服接口。
 * 返回后端响应对象，默认是 `{ reply: string }`。
 */
export async function sendAiCustomerMessage(payload) {
  const { data } = await axios.post('/api/ai-service/chat', payload || {})
  return data || {}
}

