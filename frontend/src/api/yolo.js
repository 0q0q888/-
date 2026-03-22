import axios from 'axios'

/**
 * 调用后端 YOLO 模块，根据商品主图给出标题/描述建议。
 *
 * 约定后端接口：
 *   POST /api/yolo/goods/title-suggest
 *   body: { imageUrl: string }
 *   返回: { titleSuggest: string, descTemplate?: string }
 *
 * @param {string} imageUrl 已上传到后端的商品图片 URL（如 /uploads/goods/xxx.png）
 */
export async function suggestGoodsTitleByImage(imageUrl) {
  if (!imageUrl) {
    throw new Error('imageUrl 不能为空')
  }
  const { data } = await axios.post('/api/yolo/goods/title-suggest', { imageUrl })
  return {
    titleSuggest: data?.titleSuggest || '',
    descTemplate: data?.descTemplate || '',
  }
}

