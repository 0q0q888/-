"""
deepseekapi 本地文本生成服务。
当前仅提供 `POST /generate` 接口。
"""

from typing import Optional

from fastapi import FastAPI
from pydantic import BaseModel
from transformers import AutoModelForCausalLM, AutoTokenizer
import torch

app = FastAPI()

# 你的模型目录（通常包含 config/tokenizer/model 权重文件等）。
model_path = "./deepseekr1-1.5b-merged"

# tokenizer 负责把文本 -> token ids；model 负责 token -> logits/生成序列。
tokenizer = AutoTokenizer.from_pretrained(model_path)

# 自动选择设备：有 CUDA 就用 GPU，否则退回 CPU。
device = "cuda" if torch.cuda.is_available() else "cpu"

# 加载因果语言模型并移动到指定设备上。
model = AutoModelForCausalLM.from_pretrained(model_path).to(device)


class GenerateRequest(BaseModel):
  prompt: Optional[str] = None


@app.post("/generate")
async def generate_text(request: GenerateRequest):
  """
  接收 `prompt` 并调用模型生成文本。
  返回结构为 `{ "generated_text": "..." }`。
  """

  prompt = (request.prompt or "").strip()
  if not prompt:
    return {"generated_text": ""}

  # 编码输入并放到模型设备。
  inputs = tokenizer(prompt, return_tensors="pt").to(device)

  # 生成并将序列解码为字符串。
  outputs = model.generate(inputs["input_ids"], max_length=150)
  generated_text = tokenizer.decode(outputs[0], skip_special_tokens=True)

  return {"generated_text": generated_text}


if __name__ == "__main__":
  # 本地启动命令入口。
  import uvicorn

  uvicorn.run(
    "main:app",
    host="0.0.0.0",
    port=9000,
    reload=False,
  )