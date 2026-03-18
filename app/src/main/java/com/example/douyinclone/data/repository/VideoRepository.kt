package com.example.douyinclone.data.repository

import com.example.douyinclone.data.model.Comment
import com.example.douyinclone.data.model.VideoItem
import com.example.douyinclone.data.remote.BackendApiClient
import com.example.douyinclone.data.remote.AddCommentRequest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VideoRepository @Inject constructor() {

    private val api = BackendApiClient.api

    suspend fun getVideos(page: Int, pageSize: Int = 10): List<VideoItem> {
        return api.getVideos(page = page, pageSize = pageSize).items
    }

    suspend fun refreshVideos(): List<VideoItem> {
        // 简单实现：重新拉取第 0 页，后续可以根据需求扩展
        return api.getVideos(page = 0, pageSize = 20).items
    }

    suspend fun getComments(videoId: String): List<Comment> {
        return api.getComments(videoId).items
    }

    suspend fun addComment(videoId: String, content: String): Comment {
        return api.addComment(
            videoId = videoId,
            body = AddCommentRequest(content = content)
        )
    }

    suspend fun likeVideo(videoId: String): Boolean {
        val response = api.likeVideo(videoId)
        return response.success
    }
}
