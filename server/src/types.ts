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

