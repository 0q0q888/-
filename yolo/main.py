"""
简易 YOLO 推理服务：
- 使用 FastAPI 对外暴露 HTTP 接口；
- 加载用户提供的 best.pt（请将 best.pt 放在本目录下）；
- 提供 POST /goods/title-suggest 接口，根据图片 URL 生成标题/描述建议。

启动方式：
  1. 安装依赖（建议在单独的 conda/venv 环境中）:
       pip install ultralytics fastapi uvicorn pillow requests
  2. 将你的 best.pt 放在当前 yolo/ 目录下；
  3. 在该目录执行：
       uvicorn infer_server:app --host 0.0.0.0 --port 8001

后端 Java 将调用：
  POST http://localhost:8001/goods/title-suggest
  body: { "imageUrl": "http://localhost:8080/uploads/goods/xxx.png" }
"""

from typing import List, Optional

import io

import requests
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from PIL import Image
from ultralytics import YOLO



LABEL_ZH = {
  "Person": "人",
  "Chair": "椅子",
  "Toothbrush": "牙刷",
  "Knife": "刀",
  "Bottle": "瓶子",
  "Cup": "杯子",
  "Spoon": "勺子",
  "Bench": "长凳",
  "Refrigerator": "冰箱",
  "Fork": "叉子",
  "Bus": "公交车",
  "Toilet": "马桶",
  "Bicycle": "自行车",
  "Airplane": "飞机",
  "Truck": "卡车",
  "Motorcycles": "摩托车",
  "Oven": "烤箱",
  "Dog": "狗",
  "Bed": "床",
  "Cat": "猫",
  "Traffic Light": "红绿灯",
  "Currency": "纸币/硬币",
  "Face": "人脸",
  "Stop Sign": "停车标志",
  "Car": "汽车",
  "Barriers": "路障/围栏",
  "Path Holes": "路面坑洞",
  "Stairs": "楼梯",
  "Train": "火车",
  "Bin": "垃圾桶",
  "Blind Stick": "盲杖",
  "Men Sign": "男厕标志",
  "Cell Phone": "手机",
  "Women Sign": "女厕标志",
  "Tap": "水龙头",
}


class TitleSuggestRequest(BaseModel):
  imageUrl: str


class TitleSuggestResponse(BaseModel):
  titleSuggest: str
  descTemplate: Optional[str] = ""


app = FastAPI(title="YOLO Goods Title Suggest Service")


@app.on_event("startup")
def load_model():
  """
  在服务启动时加载 YOLO 模型。
  请将 best.pt 放到与 infer_server.py 同一目录。
  """
  global model
  try:
    model = YOLO("best.pt")
  except Exception as e:
    raise RuntimeError(f"加载 YOLO 模型失败，请确认 best.pt 路径正确: {e}") from e


def _download_image(url: str) -> Image.Image:
  try:
    resp = requests.get(url, timeout=10)
    resp.raise_for_status()
  except Exception as e:
    raise HTTPException(status_code=400, detail=f"下载图片失败: {e}")
  try:
    img = Image.open(io.BytesIO(resp.content)).convert("RGB")
    return img
  except Exception as e:
    raise HTTPException(status_code=400, detail=f"解析图片失败: {e}")


@app.post("/goods/title-suggest", response_model=TitleSuggestResponse)
def goods_title_suggest(req: TitleSuggestRequest):
  """
  根据图片 URL 识别物品，并生成一个简短的标题/描述模板。
  """
  url = req.imageUrl.strip()
  if not url:
    raise HTTPException(status_code=400, detail="imageUrl 不能为空")

  img = _download_image(url)

  try:
    results = model(img, verbose=False)
  except Exception as e:
    raise HTTPException(status_code=500, detail=f"模型推理失败: {e}")

  if not results:
    return TitleSuggestResponse(titleSuggest="", descTemplate="")

  r = results[0]
  detections: List[str] = []
  for box in r.boxes:
    cls_id = int(box.cls[0])
    conf = float(box.conf[0])
    if conf < 0.25:
      continue
    en_label = r.names.get(cls_id, str(cls_id))
    zh_label = LABEL_ZH.get(en_label, en_label)
    detections.append(zh_label)

  # 去重，保持顺序
  seen = set()
  unique_labels: List[str] = []
  for name in detections:
    if name not in seen:
      seen.add(name)
      unique_labels.append(name)

  if not unique_labels:
    return TitleSuggestResponse(
      titleSuggest="",
      descTemplate="未检测到明显物品，请上传更清晰的商品图片，或手动填写描述。",
    )

  # 简单的标题策略：用「/」连接识别到的物品名称
  title = " / ".join(unique_labels)

  # 生成一个基础描述模板，方便前端直接填入 textarea
  items_cn = "、".join(unique_labels)
  desc = (
    f"【智能识别结果】检测到：{items_cn}（请仔细核对实物，以实物为准）。\n"
    "【物品情况】成色：___；使用时长：___；是否有明显划痕/损耗：___。\n"
    "【配件情况】包含配件：___；缺少配件：___。\n"
    "【交易说明】面交地点：___；是否可议价：是/否；其他备注：___。"
  )

  return TitleSuggestResponse(titleSuggest=title, descTemplate=desc)


@app.get("/health")
def health():
  return {"status": "ok"}


if __name__ == "__main__":
  import uvicorn

  uvicorn.run(
    "main:app",
    host="0.0.0.0",
    port=8001,
    reload=False,
  )

