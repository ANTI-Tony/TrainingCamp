## 后端服务（TrainingCamp/server）

这是给 Android Demo 配套的最小后端，提供视频流、评论、点赞接口。

### 运行

在 `TrainingCamp/server` 目录执行：

```bash
npm install
npm run dev
```

默认监听 `http://localhost:3000`。

### 环境变量

- `PORT`: 端口（默认 3000）
- `CORS_ORIGIN`: 允许的 Origin。默认 `*`。如果要限制可以用逗号分隔：`http://localhost:5173,http://127.0.0.1:5173`
- `DB_PATH`: SQLite 文件路径（默认 `dev.db`，在 `TrainingCamp/server` 目录下生成）

### API

- `GET /health`
- `GET /videos?page=0&page_size=10&user_id=current_user`
- `GET /videos/{id}/comments?page=0&page_size=20`
- `POST /videos/{id}/comments`
  - body: `{ "content": "...", "userId": "current_user", "userName": "我" }`
- `POST /videos/{id}/like`
  - body: `{ "userId": "current_user" }`
- `POST /events/exposure`
  - body: `{ "userId": "u1", "videoId": "video_0", "scene": "feed", "position": 3, "requestId": "req_xxx" }`
- `POST /events/click`
  - body: `{ "userId": "u1", "videoId": "video_0", "scene": "feed", "position": 3, "requestId": "req_xxx" }`
- `POST /events/play`
  - body: `{ "userId": "u1", "videoId": "video_0", "scene": "detail", "playMs": 8321, "isComplete": false, "requestId": "req_xxx" }`

### 说明

- 数据已接入 SQLite（`better-sqlite3`），服务重启后数据不会丢。
- 点赞是 **userId + videoId** 维度，不再是全局开关。
- 评论接口支持分页并返回 `total`。
- 已增加事件埋点表 `events`，用于采集曝光/点击/播放行为（后续召回/排序会用到）。

