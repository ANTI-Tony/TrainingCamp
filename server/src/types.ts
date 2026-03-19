export interface VideoItem {
  id: string;
  coverUrl: string;
  videoUrl: string;
  title: string;
  authorName: string;
  authorAvatar: string;
  likeCount: number;
  commentCount: number;
  shareCount: number;
  isLiked: boolean;
  description: string;
  musicName: string;
  musicAuthor: string;
}

export interface Comment {
  id: string;
  videoId: string;
  userId: string;
  userName: string;
  userAvatar: string;
  content: string;
  likeCount: number;
  createTime: number;
  isLiked: boolean;
}

export interface ApiError {
  errorCode: string;
  message: string;
  details?: unknown;
}

export type EventType = "exposure" | "click" | "play";

export interface EventBase {
  id: string;
  type: EventType;
  userId: string;
  videoId: string;
  createTime: number;
}

export interface ExposureEvent extends EventBase {
  type: "exposure";
  scene?: string;
  position?: number;
  requestId?: string;
}

export interface ClickEvent extends EventBase {
  type: "click";
  scene?: string;
  position?: number;
  requestId?: string;
}

export interface PlayEvent extends EventBase {
  type: "play";
  scene?: string;
  requestId?: string;
  playMs?: number;
  isComplete?: boolean;
}

export type Event = ExposureEvent | ClickEvent | PlayEvent;

