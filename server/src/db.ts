import Database from "better-sqlite3";
import path from "path";
import { Comment, VideoItem } from "./types";

const DB_PATH = process.env.DB_PATH || path.join(process.cwd(), "dev.db");

export const db = new Database(DB_PATH);
db.pragma("journal_mode = WAL");

db.exec(`
CREATE TABLE IF NOT EXISTS videos (
  id TEXT PRIMARY KEY,
  coverUrl TEXT NOT NULL,
  videoUrl TEXT NOT NULL,
  title TEXT NOT NULL,
  authorName TEXT NOT NULL,
  authorAvatar TEXT NOT NULL,
  likeCount INTEGER NOT NULL DEFAULT 0,
  commentCount INTEGER NOT NULL DEFAULT 0,
  shareCount INTEGER NOT NULL DEFAULT 0,
  description TEXT NOT NULL,
  musicName TEXT NOT NULL,
  musicAuthor TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS comments (
  id TEXT PRIMARY KEY,
  videoId TEXT NOT NULL,
  userId TEXT NOT NULL,
  userName TEXT NOT NULL,
  userAvatar TEXT NOT NULL,
  content TEXT NOT NULL,
  likeCount INTEGER NOT NULL DEFAULT 0,
  createTime INTEGER NOT NULL,
  isLiked INTEGER NOT NULL DEFAULT 0,
  FOREIGN KEY(videoId) REFERENCES videos(id)
);
CREATE INDEX IF NOT EXISTS idx_comments_video_time ON comments(videoId, createTime DESC);

CREATE TABLE IF NOT EXISTS likes (
  userId TEXT NOT NULL,
  videoId TEXT NOT NULL,
  createTime INTEGER NOT NULL,
  PRIMARY KEY(userId, videoId),
  FOREIGN KEY(videoId) REFERENCES videos(id)
);
CREATE INDEX IF NOT EXISTS idx_likes_video ON likes(videoId);
`);

function seedIfEmpty() {
  const count = db.prepare("SELECT COUNT(1) as c FROM videos").get() as { c: number };
  if (count.c > 0) return;

  const covers = Array.from({ length: 10 }, (_, i) => `https://picsum.photos/400/600?random=${i + 1}`);
  const titles = [
    "38集｜喜欢海边五彩斑斓的彩色石头吗？观看建...",
    "人生只要两次幸运就好，一次遇见你，一次走到底...",
    "程摇跨简单易学 饭后十分钟 助消化瘦肚子",
    "苏泊尔折叠式烧水壶，折叠设计，小巧轻便，出差旅...",
    "#适合所有人的健身动 每天坚持锻炼会有不一...",
    "大话西游:归来",
    "《基础摆胯舞》",
    "今日份的美食分享",
    "旅行vlog｜云南之行",
    "教你三分钟学会这道菜"
  ];
  const authors = [
    "治愈宝藏美景",
    "向日葵",
    "HuDev",
    "苏泊尔折叠电水壶",
    "健身达人",
    "游戏官方",
    "舞蹈教学",
    "美食家小明",
    "旅行者小红",
    "厨房小白"
  ];
  const videoUrls = [
    "https://www.w3schools.com/html/mov_bbb.mp4",
    "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
    "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4",
    "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerFun.mp4",
    "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerJoyrides.mp4",
    "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerMeltdowns.mp4",
    "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4",
    "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/SubaruOutbackOnStreetAndDirt.mp4",
    "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4",
    "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/VolkswagenGTIReview.mp4"
  ];

  const insert = db.prepare(`
    INSERT INTO videos (
      id, coverUrl, videoUrl, title, authorName, authorAvatar,
      likeCount, commentCount, shareCount,
      description, musicName, musicAuthor
    ) VALUES (
      @id, @coverUrl, @videoUrl, @title, @authorName, @authorAvatar,
      @likeCount, @commentCount, @shareCount,
      @description, @musicName, @musicAuthor
    )
  `);

  const tx = db.transaction(() => {
    for (let i = 0; i < 20; i++) {
      const index = i % 10;
      insert.run({
        id: `video_${i}`,
        coverUrl: covers[index],
        videoUrl: videoUrls[index],
        title: titles[index],
        authorName: authors[index],
        authorAvatar: `https://picsum.photos/100/100?random=${i + 100}`,
        likeCount: Math.floor(Math.random() * (9999 - 100 + 1)) + 100,
        commentCount: 0,
        shareCount: Math.floor(Math.random() * (500 - 5 + 1)) + 5,
        description: titles[index],
        musicName: "原声音乐",
        musicAuthor: authors[index]
      });
    }
  });
  tx();
}

seedIfEmpty();

export function getVideos(params: { page: number; pageSize: number; userId?: string }) {
  const { page, pageSize, userId } = params;
  const offset = page * pageSize;

  const rows = db
    .prepare(
      `SELECT id, coverUrl, videoUrl, title, authorName, authorAvatar,
              likeCount, commentCount, shareCount,
              description, musicName, musicAuthor
       FROM videos
       ORDER BY id
       LIMIT ? OFFSET ?`
    )
    .all(pageSize, offset) as Omit<VideoItem, "isLiked">[];

  const ids = rows.map((r) => r.id);
  let likedSet = new Set<string>();
  if (userId && ids.length > 0) {
    const liked = db
      .prepare(`SELECT videoId FROM likes WHERE userId = ? AND videoId IN (${ids.map(() => "?").join(",")})`)
      .all(userId, ...ids) as { videoId: string }[];
    likedSet = new Set(liked.map((x) => x.videoId));
  }

  const items: VideoItem[] = rows.map((r) => ({
    ...r,
    isLiked: userId ? likedSet.has(r.id) : false
  }));

  const total = (db.prepare("SELECT COUNT(1) as c FROM videos").get() as { c: number }).c;
  return { items, total };
}

export function getComments(params: { videoId: string; page: number; pageSize: number }) {
  const { videoId, page, pageSize } = params;
  const offset = page * pageSize;
  const items = db
    .prepare(
      `SELECT id, videoId, userId, userName, userAvatar, content,
              likeCount, createTime, isLiked
       FROM comments
       WHERE videoId = ?
       ORDER BY createTime DESC
       LIMIT ? OFFSET ?`
    )
    .all(videoId, pageSize, offset)
    .map((r: any) => ({
      ...r,
      isLiked: Boolean(r.isLiked)
    })) as Comment[];

  const total = (db
    .prepare("SELECT COUNT(1) as c FROM comments WHERE videoId = ?")
    .get(videoId) as { c: number }).c;

  return { items, total };
}

export function addComment(params: {
  videoId: string;
  userId: string;
  userName: string;
  content: string;
}) {
  const { videoId, userId, userName, content } = params;
  const now = Date.now();
  const id = `comment_${now}_${Math.floor(Math.random() * 10000)}`;

  const tx = db.transaction(() => {
    db.prepare(
      `INSERT INTO comments (id, videoId, userId, userName, userAvatar, content, likeCount, createTime, isLiked)
       VALUES (?, ?, ?, ?, ?, ?, 0, ?, 0)`
    ).run(id, videoId, userId, userName, "https://picsum.photos/50/50?random=999", content, now);

    db.prepare(`UPDATE videos SET commentCount = commentCount + 1 WHERE id = ?`).run(videoId);
  });
  tx();

  const row = db
    .prepare(
      `SELECT id, videoId, userId, userName, userAvatar, content, likeCount, createTime, isLiked
       FROM comments WHERE id = ?`
    )
    .get(id) as any;

  return {
    ...row,
    isLiked: Boolean(row.isLiked)
  } as Comment;
}

export function toggleLike(params: { videoId: string; userId: string }) {
  const { videoId, userId } = params;
  const now = Date.now();

  const tx = db.transaction(() => {
    const existing = db
      .prepare(`SELECT 1 as ok FROM likes WHERE userId = ? AND videoId = ?`)
      .get(userId, videoId) as { ok: 1 } | undefined;

    if (existing) {
      db.prepare(`DELETE FROM likes WHERE userId = ? AND videoId = ?`).run(userId, videoId);
      db.prepare(`UPDATE videos SET likeCount = MAX(0, likeCount - 1) WHERE id = ?`).run(videoId);
      return { isLiked: false };
    }

    db.prepare(`INSERT INTO likes (userId, videoId, createTime) VALUES (?, ?, ?)`).run(userId, videoId, now);
    db.prepare(`UPDATE videos SET likeCount = likeCount + 1 WHERE id = ?`).run(videoId);
    return { isLiked: true };
  });

  const { isLiked } = tx();
  const row = db.prepare(`SELECT likeCount FROM videos WHERE id = ?`).get(videoId) as { likeCount: number } | undefined;
  return { isLiked, likeCount: row?.likeCount ?? 0 };
}

export function videoExists(videoId: string) {
  const row = db.prepare(`SELECT 1 as ok FROM videos WHERE id = ?`).get(videoId) as { ok: 1 } | undefined;
  return Boolean(row);
}

