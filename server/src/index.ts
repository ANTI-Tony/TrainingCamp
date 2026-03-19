import express from "express";
import cors from "cors";
import morgan from "morgan";
import { addComment, getComments, getVideos, insertEvent, toggleLike, videoExists } from "./db";
import { parsePositiveInt, sendError } from "./http";

const app = express();

const PORT = parsePositiveInt(process.env.PORT, 3000, { min: 1, max: 65535 });
const CORS_ORIGIN = process.env.CORS_ORIGIN || "*";

app.use(cors({ origin: CORS_ORIGIN === "*" ? true : CORS_ORIGIN.split(",").map((s) => s.trim()) }));
app.use(express.json());
app.use(morgan("dev"));

app.get("/", (_req, res) => {
  res.json({
    message: "Douyin backend is running",
    health: "/health",
    videos: "/videos"
  });
});

app.get("/health", (_req, res) => {
  res.json({ status: "ok" });
});

// POST /events/exposure
app.post("/events/exposure", (req, res) => {
  const body = req.body || {};
  const userId = typeof body.userId === "string" && body.userId.trim() ? body.userId.trim() : "current_user";
  const videoId = typeof body.videoId === "string" ? body.videoId.trim() : "";
  const scene = typeof body.scene === "string" ? body.scene.trim() : undefined;
  const requestId = typeof body.requestId === "string" ? body.requestId.trim() : undefined;
  const position = typeof body.position === "number" && Number.isFinite(body.position) ? body.position : undefined;

  if (!videoId) return sendError(res, 400, "INVALID_ARGUMENT", "videoId is required");
  if (!videoExists(videoId)) return sendError(res, 404, "VIDEO_NOT_FOUND", "video not found", { videoId });

  const { id, createTime } = insertEvent({ type: "exposure", userId, videoId, scene, requestId, position });
  res.status(201).json({ success: true, id, createTime });
});

// POST /events/click
app.post("/events/click", (req, res) => {
  const body = req.body || {};
  const userId = typeof body.userId === "string" && body.userId.trim() ? body.userId.trim() : "current_user";
  const videoId = typeof body.videoId === "string" ? body.videoId.trim() : "";
  const scene = typeof body.scene === "string" ? body.scene.trim() : undefined;
  const requestId = typeof body.requestId === "string" ? body.requestId.trim() : undefined;
  const position = typeof body.position === "number" && Number.isFinite(body.position) ? body.position : undefined;

  if (!videoId) return sendError(res, 400, "INVALID_ARGUMENT", "videoId is required");
  if (!videoExists(videoId)) return sendError(res, 404, "VIDEO_NOT_FOUND", "video not found", { videoId });

  const { id, createTime } = insertEvent({ type: "click", userId, videoId, scene, requestId, position });
  res.status(201).json({ success: true, id, createTime });
});

// POST /events/play
app.post("/events/play", (req, res) => {
  const body = req.body || {};
  const userId = typeof body.userId === "string" && body.userId.trim() ? body.userId.trim() : "current_user";
  const videoId = typeof body.videoId === "string" ? body.videoId.trim() : "";
  const scene = typeof body.scene === "string" ? body.scene.trim() : undefined;
  const requestId = typeof body.requestId === "string" ? body.requestId.trim() : undefined;
  const playMs = typeof body.playMs === "number" && Number.isFinite(body.playMs) ? Math.max(0, body.playMs) : undefined;
  const isComplete = typeof body.isComplete === "boolean" ? body.isComplete : undefined;

  if (!videoId) return sendError(res, 400, "INVALID_ARGUMENT", "videoId is required");
  if (!videoExists(videoId)) return sendError(res, 404, "VIDEO_NOT_FOUND", "video not found", { videoId });

  const { id, createTime } = insertEvent({ type: "play", userId, videoId, scene, requestId, playMs, isComplete });
  res.status(201).json({ success: true, id, createTime });
});

// GET /videos?user_id=&scene=&page=&page_size=
app.get("/videos", (req, res) => {
  const page = parsePositiveInt(req.query.page, 0, { min: 0, max: 100000 });
  const pageSize = parsePositiveInt(req.query.page_size, 10, { min: 1, max: 50 });
  const userId = typeof req.query.user_id === "string" ? req.query.user_id : undefined;

  const { items, total } = getVideos({ page, pageSize, userId });
  res.json({ items, page, pageSize, total });
});

// GET /videos/:id/comments?page=&page_size=
app.get("/videos/:id/comments", (req, res) => {
  const videoId = req.params.id;
  if (!videoExists(videoId)) {
    return sendError(res, 404, "VIDEO_NOT_FOUND", "video not found", { videoId });
  }

  const page = parsePositiveInt(req.query.page, 0, { min: 0, max: 100000 });
  const pageSize = parsePositiveInt(req.query.page_size, 20, { min: 1, max: 50 });

  const { items, total } = getComments({ videoId, page, pageSize });
  res.json({ items, page, pageSize, total });
});

// POST /videos/:id/comments
app.post("/videos/:id/comments", (req, res) => {
  const videoId = req.params.id;
  if (!videoExists(videoId)) {
    return sendError(res, 404, "VIDEO_NOT_FOUND", "video not found", { videoId });
  }

  const body = req.body || {};
  const content = typeof body.content === "string" ? body.content.trim() : "";
  const userId = typeof body.userId === "string" && body.userId.trim() ? body.userId.trim() : "current_user";
  const userName = typeof body.userName === "string" && body.userName.trim() ? body.userName.trim() : "我";

  if (!content) {
    return sendError(res, 400, "INVALID_ARGUMENT", "content is required");
  }
  if (content.length > 500) {
    return sendError(res, 400, "INVALID_ARGUMENT", "content too long (max 500)");
  }

  const comment = addComment({ videoId, userId, userName, content });
  res.status(201).json(comment);
});

// POST /videos/:id/like  { userId }
app.post("/videos/:id/like", (req, res) => {
  const videoId = req.params.id;
  if (!videoExists(videoId)) {
    return sendError(res, 404, "VIDEO_NOT_FOUND", "video not found", { videoId });
  }

  const body = req.body || {};
  const userId = typeof body.userId === "string" && body.userId.trim() ? body.userId.trim() : "current_user";

  const { isLiked, likeCount } = toggleLike({ videoId, userId });
  res.json({ success: true, isLiked, likeCount });
});

app.use((req, res) => {
  sendError(res, 404, "NOT_FOUND", "route not found", { method: req.method, path: req.path });
});

app.listen(PORT, () => {
  console.log(`Douyin backend server running on http://localhost:${PORT}`);
  console.log(`DB_PATH=${process.env.DB_PATH || "dev.db (in server dir)"}; CORS_ORIGIN=${CORS_ORIGIN}`);
});

