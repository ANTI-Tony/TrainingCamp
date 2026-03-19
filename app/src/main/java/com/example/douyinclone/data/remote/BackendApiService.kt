package com.example.douyinclone.data.remote

import com.example.douyinclone.data.model.Comment
import com.example.douyinclone.data.model.VideoItem
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

interface BackendApiService {

    @GET("videos")
    suspend fun getVideos(
        @Query("page") page: Int,
        @Query("page_size") pageSize: Int
    ): VideosResponse

    @GET("videos/{id}/comments")
    suspend fun getComments(
        @Path("id") videoId: String
    ): CommentsResponse

    @POST("videos/{id}/comments")
    suspend fun addComment(
        @Path("id") videoId: String,
        @Body body: AddCommentRequest
    ): Comment

    @POST("videos/{id}/like")
    suspend fun likeVideo(
        @Path("id") videoId: String,
        @Body body: LikeVideoRequest = LikeVideoRequest()
    ): LikeVideoResponse

    @POST("events/exposure")
    suspend fun trackExposure(
        @Body body: ExposureEventRequest
    ): TrackEventResponse

    @POST("events/click")
    suspend fun trackClick(
        @Body body: ClickEventRequest
    ): TrackEventResponse

    @POST("events/play")
    suspend fun trackPlay(
        @Body body: PlayEventRequest
    ): TrackEventResponse
}

data class VideosResponse(
    val items: List<VideoItem>,
    val page: Int,
    val pageSize: Int,
    val total: Int
)

data class CommentsResponse(
    val items: List<Comment>
)

data class AddCommentRequest(
    val content: String,
    val userId: String = "current_user",
    val userName: String = "我"
)

data class LikeVideoRequest(
    val userId: String = "current_user"
)

data class LikeVideoResponse(
    val success: Boolean,
    val isLiked: Boolean,
    val likeCount: Int
)

data class ExposureEventRequest(
    val userId: String = "current_user",
    val videoId: String,
    val scene: String? = null,
    val position: Int? = null,
    val requestId: String? = null
)

data class ClickEventRequest(
    val userId: String = "current_user",
    val videoId: String,
    val scene: String? = null,
    val position: Int? = null,
    val requestId: String? = null
)

data class PlayEventRequest(
    val userId: String = "current_user",
    val videoId: String,
    val scene: String? = null,
    val playMs: Long? = null,
    val isComplete: Boolean? = null,
    val requestId: String? = null
)

data class TrackEventResponse(
    val success: Boolean,
    val id: String,
    val createTime: Long
)

object BackendApiClient {

    // Emulator: use 10.0.2.2 to access host machine
    private const val BASE_URL = "http://10.0.2.2:3000/"

    val api: BackendApiService by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build()

        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(BackendApiService::class.java)
    }
}

