package com.example.douyinclone.ui.video

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.douyinclone.data.model.Comment
import com.example.douyinclone.data.model.VideoItem
import com.example.douyinclone.data.repository.VideoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VideoDetailViewModel @Inject constructor(
    private val repository: VideoRepository
) : ViewModel() {
    
    private val _videos = MutableLiveData<List<VideoItem>>()
    val videos: LiveData<List<VideoItem>> = _videos
    
    private val _currentVideo = MutableLiveData<VideoItem>()
    val currentVideo: LiveData<VideoItem> = _currentVideo
    
    private val _currentPosition = MutableLiveData<Int>()
    val currentPosition: LiveData<Int> = _currentPosition
    
    private val _isPlaying = MutableLiveData<Boolean>(true)
    val isPlaying: LiveData<Boolean> = _isPlaying
    
    private val _comments = MutableLiveData<List<Comment>>()
    val comments: LiveData<List<Comment>> = _comments
    
    private val _showComments = MutableLiveData<Boolean>(false)
    val showComments: LiveData<Boolean> = _showComments
    
    private var isLoadingMore = false
    private var currentPage = 0
    
    fun setInitialVideos(videoList: List<VideoItem>, startPosition: Int) {
        _videos.value = videoList
        _currentPosition.value = startPosition
        if (videoList.isNotEmpty() && startPosition < videoList.size) {
            _currentVideo.value = videoList[startPosition]
        }
    }
    
    fun setCurrentPosition(position: Int) {
        _currentPosition.value = position
        val videoList = _videos.value.orEmpty()
        if (position < videoList.size) {
            _currentVideo.value = videoList[position]
        }
    }
    
    fun togglePlayPause() {
        _isPlaying.value = !(_isPlaying.value ?: true)
    }
    
    fun play() {
        _isPlaying.value = true
    }
    
    fun pause() {
        _isPlaying.value = false
    }
    
    fun likeCurrentVideo() {
        val video = _currentVideo.value ?: return
        viewModelScope.launch {
            repository.likeVideo(video.id)
            val updatedVideo = video.copy(
                isLiked = !video.isLiked,
                likeCount = if (video.isLiked) video.likeCount - 1 else video.likeCount + 1
            )
            _currentVideo.value = updatedVideo
            
            // 同时更新列表中的数据
            val currentList = _videos.value.orEmpty().map { v ->
                if (v.id == video.id) updatedVideo else v
            }
            _videos.value = currentList
        }
    }
    
    fun doubleTapLike() {
        val video = _currentVideo.value ?: return
        if (!video.isLiked) {
            likeCurrentVideo()
        }
    }

    fun trackPlayFinished(playMs: Long?, isComplete: Boolean?) {
        val video = _currentVideo.value ?: return
        viewModelScope.launch {
            repository.trackPlay(
                videoId = video.id,
                playMs = playMs,
                isComplete = isComplete,
                scene = "detail"
            )
        }
    }
    
    fun loadComments() {
        val video = _currentVideo.value ?: return
        viewModelScope.launch {
            val commentList = repository.getComments(video.id)
            _comments.value = commentList
        }
    }
    
    fun showCommentPanel() {
        _showComments.value = true
        loadComments()
    }
    
    fun hideCommentPanel() {
        _showComments.value = false
    }
    
    fun addComment(content: String) {
        val video = _currentVideo.value ?: return
        if (content.isBlank()) return
        
        viewModelScope.launch {
            val newComment = repository.addComment(video.id, content)
            val currentComments = _comments.value.orEmpty().toMutableList()
            currentComments.add(0, newComment)
            _comments.value = currentComments
        }
    }
    
    fun loadMoreVideos() {
        if (isLoadingMore) return
        
        viewModelScope.launch {
            isLoadingMore = true
            try {
                currentPage++
                val moreVideos = repository.getVideos(currentPage)
                val currentList = _videos.value.orEmpty().toMutableList()
                currentList.addAll(moreVideos)
                _videos.value = currentList
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoadingMore = false
            }
        }
    }
    
    /**
     * 更新视频头像
     */
    fun updateVideoAvatar(videoId: String, newAvatarUrl: String) {
        val currentList = _videos.value.orEmpty().map { video ->
            if (video.id == videoId) {
                video.copy(authorAvatar = newAvatarUrl)
            } else {
                video
            }
        }
        _videos.value = currentList
        
        // 如果当前视频就是要更新的视频，也更新当前视频
        _currentVideo.value?.let { current ->
            if (current.id == videoId) {
                _currentVideo.value = current.copy(authorAvatar = newAvatarUrl)
            }
        }
    }
}
