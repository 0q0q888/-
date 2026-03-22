## deepseekapi（本地 DeepSeek FastAPI 服务）

该目录用于部署你微调后的 DeepSeek 模型，并暴露 HTTP 接口供后端调用。

### 接口兼容性

本服务提供 `POST /chat/completions`，返回格式包含 `choices[0].message.content`，与你后端 `AiService` 的解析逻辑兼容。

### 安装依赖

在本目录执行：

```bash
pip install -r requirements.txt
```

### 启动（默认 mock 模式）

```bash
python main.py
```

- 默认端口：`9000`
- 健康检查：`GET http://localhost:9000/health`

mock 模式下不会加载模型，会返回提示文本，用于先打通前后端链路。

### 启动真实模型

设置环境变量：

- `MOCK_MODE=0`
- `MODEL_PATH=你的微调模型目录（包含 config/tokenizer/model.safetensors 等）`

然后启动：

```bash
python main.py
```

### 后端对接提示

将后端 `application.yml` 中的 `deepseek.base-url` 改为本服务地址，例如：

```yaml
deepseek:
  base-url: http://localhost:9000
  api-key: dummy
```

后端会请求：`http://localhost:9000/chat/completions`

