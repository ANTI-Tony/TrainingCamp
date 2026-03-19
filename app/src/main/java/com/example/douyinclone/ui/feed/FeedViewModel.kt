package com.example.douyinclone.ui.feed

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.douyinclone.data.model.TabItem
import com.example.douyinclone.data.model.VideoItem
import com.example.douyinclone.data.repository.VideoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FeedViewModel @Inject constructor(
    private val repository: VideoRepository
) : ViewModel() {
    
    private val _videos = MutableLiveData<List<VideoItem>>()
    val videos: LiveData<List<VideoItem>> = _videos
    
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    private val _isRefreshing = MutableLiveData<Boolean>()
    val isRefreshing: LiveData<Boolean> = _isRefreshing
    
    private val _selectedTopTab = MutableLiveData<Int>(5) // 默认选中"推荐"
    val selectedTopTab: LiveData<Int> = _selectedTopTab
    
    private val _selectedBottomTab = MutableLiveData<Int>(0) // 默认选中"首页"
    val selectedBottomTab: LiveData<Int> = _selectedBottomTab
    
    private val _topTabs = MutableLiveData<List<TabItem>>()
    val topTabs: LiveData<List<TabItem>> = _topTabs
    
    private var currentPage = 0
    private var isLoadingMore = false
    
    init {
        initTabs()
        loadVideos()
    }
    
    private fun initTabs() {
        _topTabs.value = listOf(
            TabItem(0, "购"),
            TabItem(1, "经验"),
            TabItem(2, "同城"),
            TabItem(3, "关注"),
            TabItem(4, "商城"),
            TabItem(5, "推荐", true)
        )
    }
    
    fun loadVideos() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                currentPage = 0
                val result = repository.getVideos(currentPage)
                _videos.value = result
                trackExposureForPage(result, currentPage)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun refreshVideos() {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                currentPage = 0
                val result = repository.refreshVideos()
                _videos.value = result
                trackExposureForPage(result, currentPage)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isRefreshing.value = false
            }
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
                val startIndex = currentList.size
                currentList.addAll(moreVideos)
                _videos.value = currentList
                trackExposureForPage(moreVideos, currentPage, startIndex)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoadingMore = false
            }
        }
    }
    
    fun selectTopTab(position: Int) {
        _selectedTopTab.value = position
        val currentTabs = _topTabs.value.orEmpty().map { tab ->
            tab.copy(isSelected = tab.id == position)
        }
        _topTabs.value = currentTabs
        // 切换tab时刷新数据
        loadVideos()
    }
    
    fun selectBottomTab(position: Int) {
        _selectedBottomTab.value = position
    }
    
    fun likeVideo(videoId: String) {
        viewModelScope.launch {
            repository.likeVideo(videoId)
            // 更新本地列表
            val currentList = _videos.value.orEmpty().map { video ->
                if (video.id == videoId) {
                    video.copy(
                        isLiked = !video.isLiked,
                        likeCount = if (video.isLiked) video.likeCount - 1 else video.likeCount + 1
                    )
                } else {
                    video
                }
            }
            _videos.value = currentList
        }
    }

    /**
     * 为当前页的视频打曝光埋点
     */
    private fun trackExposureForPage(videos: List<VideoItem>, page: Int, startIndex: Int = 0) {
        viewModelScope.launch {
            videos.forEachIndexed { index, video ->
                val position = startIndex + index
                repository.trackExposure(
                    videoId = video.id,
                    position = position,
                    scene = "feed_page$page"
                )
            }
        }
    }
}
