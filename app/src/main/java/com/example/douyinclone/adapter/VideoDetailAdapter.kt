package com.example.douyinclone.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.douyinclone.R
import com.example.douyinclone.data.model.VideoItem
import com.example.douyinclone.utils.FormatUtils

class VideoDetailAdapter(
    private val onVideoClick: () -> Unit,
    private val onLikeClick: (VideoItem) -> Unit,
    private val onCommentClick: (VideoItem) -> Unit,
    private val onShareClick: (VideoItem) -> Unit,
    private val onDoubleTap: (VideoItem) -> Unit,
    private val onAvatarClick: (VideoItem) -> Unit,
    private val onAvatarLongClick: (VideoItem) -> Unit
) : ListAdapter<VideoItem, VideoDetailAdapter.VideoViewHolder>(VideoDiffCallback()) {
    
    private var currentPlayingHolder: VideoViewHolder? = null
    private val preloadedHolders = mutableMapOf<Int, VideoViewHolder>()
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_video_detail, parent, false)
        return VideoViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: VideoViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item, onVideoClick, onLikeClick, onCommentClick, onShareClick, onDoubleTap, onAvatarClick, onAvatarLongClick)
    }
    
    override fun onViewAttachedToWindow(holder: VideoViewHolder) {
        super.onViewAttachedToWindow(holder)
        val position = holder.bindingAdapterPosition
        if (position != RecyclerView.NO_POSITION) {
            preloadedHolders[position] = holder
        }
        holder.startPlay()
        currentPlayingHolder = holder
        
        // 预加载下一个视频
        preloadNextVideo(position)
    }
    
    override fun onViewDetachedFromWindow(holder: VideoViewHolder) {
        super.onViewDetachedFromWindow(holder)
        val position = holder.bindingAdapterPosition
        if (position != RecyclerView.NO_POSITION) {
            preloadedHolders.remove(position)
        }
        holder.stopPlay()
        if (currentPlayingHolder == holder) {
            currentPlayingHolder = null
        }
    }
    
    /**
     * 预加载下一个视频
     */
    private fun preloadNextVideo(currentPosition: Int) {
        val nextPosition = currentPosition + 1
        if (nextPosition < itemCount) {
            // 延迟预加载，避免影响当前视频播放
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                preloadedHolders[nextPosition]?.preloadVideo()
            }, 500) // 延迟 500ms
        }
    }
    
    override fun onViewRecycled(holder: VideoViewHolder) {
        super.onViewRecycled(holder)
        holder.releasePlayer()
    }
    
    fun pauseCurrentVideo() {
        currentPlayingHolder?.pausePlay()
    }
    
    fun resumeCurrentVideo() {
        currentPlayingHolder?.resumePlay()
    }
    
    class VideoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val playerView: PlayerView = itemView.findViewById(R.id.player_view)
        private val ivPause: ImageView = itemView.findViewById(R.id.iv_pause)
        private val ivCover: ImageView = itemView.findViewById(R.id.iv_cover)
        private val ivAvatar: ImageView = itemView.findViewById(R.id.iv_avatar)
        private val tvAuthor: TextView = itemView.findViewById(R.id.tv_author)
        private val tvDescription: TextView = itemView.findViewById(R.id.tv_description)
        private val tvMusic: TextView = itemView.findViewById(R.id.tv_music)
        private val ivLike: ImageView = itemView.findViewById(R.id.iv_like)
        private val tvLikeCount: TextView = itemView.findViewById(R.id.tv_like_count)
        private val ivComment: ImageView = itemView.findViewById(R.id.iv_comment)
        private val tvCommentCount: TextView = itemView.findViewById(R.id.tv_comment_count)
        private val ivShare: ImageView = itemView.findViewById(R.id.iv_share)
        private val tvShareCount: TextView = itemView.findViewById(R.id.tv_share_count)
        private val ivMusicDisc: ImageView = itemView.findViewById(R.id.iv_music_disc)
        
        private var player: ExoPlayer? = null
        private var currentItem: VideoItem? = null
        private var isPaused = false
        private var lastClickTime = 0L
        private var discAnimation: Animation? = null
        
        fun bind(
            item: VideoItem,
            onVideoClick: () -> Unit,
            onLikeClick: (VideoItem) -> Unit,
            onCommentClick: (VideoItem) -> Unit,
            onShareClick: (VideoItem) -> Unit,
            onDoubleTap: (VideoItem) -> Unit,
            onAvatarClick: (VideoItem) -> Unit,
            onAvatarLongClick: (VideoItem) -> Unit
        ) {
            currentItem = item
            
            // 加载封面
            Glide.with(itemView.context)
                .load(item.coverUrl)
                .into(ivCover)
            
            // 加载头像
            val prefs = itemView.context.getSharedPreferences("avatar_prefs", android.content.Context.MODE_PRIVATE)
            val customAvatarUri = prefs.getString("avatar_${item.id}", null)
            val avatarToLoad = customAvatarUri ?: item.authorAvatar
            
            Glide.with(itemView.context)
                .load(avatarToLoad)
                .circleCrop()
                .into(ivAvatar)
            
            // 设置文本
            tvAuthor.text = "@${item.authorName}"
            tvDescription.text = item.description
            tvMusic.text = "${item.musicName} - ${item.musicAuthor}"
            tvLikeCount.text = FormatUtils.formatCount(item.likeCount)
            tvCommentCount.text = FormatUtils.formatCount(item.commentCount)
            tvShareCount.text = FormatUtils.formatCount(item.shareCount)
            
            // 设置点赞状态
            ivLike.setImageResource(
                if (item.isLiked) R.drawable.ic_like_filled else R.drawable.ic_like_white
            )
            
            // 加载音乐碟片
            Glide.with(itemView.context)
                .load(item.authorAvatar)
                .circleCrop()
                .into(ivMusicDisc)
            
            // 点击事件 - 单击暂停/播放，双击点赞
            playerView.setOnClickListener {
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastClickTime < 300) {
                    // 双击点赞
                    onDoubleTap(item)
                    lastClickTime = 0
                } else {
                    // 单击暂停/播放
                    lastClickTime = currentTime
                    playerView.postDelayed({
                        if (System.currentTimeMillis() - lastClickTime >= 300) {
                            togglePlayPause()
                            onVideoClick()
                        }
                    }, 300)
                }
            }
            
            // 右侧按钮点击事件
            ivLike.setOnClickListener { onLikeClick(item) }
            ivComment.setOnClickListener { onCommentClick(item) }
            ivShare.setOnClickListener { onShareClick(item) }
            
            // 头像点击事件
            ivAvatar.setOnClickListener { onAvatarClick(item) }
            
            // 头像长按事件
            ivAvatar.setOnLongClickListener {
                onAvatarLongClick(item)
                true
            }
        }
        
        private fun togglePlayPause() {
            player?.let {
                if (it.isPlaying) {
                    it.pause()
                    isPaused = true
                    ivPause.visibility = View.VISIBLE
                    stopDiscAnimation()
                } else {
                    it.play()
                    isPaused = false
                    ivPause.visibility = View.GONE
                    startDiscAnimation()
                }
            }
        }
        
        fun startPlay() {
            currentItem?.let { item ->
                if (player == null) {
                    player = ExoPlayer.Builder(itemView.context).build().apply {
                        playerView.player = this
                        repeatMode = Player.REPEAT_MODE_ONE
                        
                        addListener(object : Player.Listener {
                            override fun onPlaybackStateChanged(playbackState: Int) {
                                when (playbackState) {
                                    Player.STATE_READY -> {
                                        ivCover.visibility = View.GONE
                                    }
                                    Player.STATE_BUFFERING -> {
                                        ivCover.visibility = View.VISIBLE
                                    }
                                }
                            }
                        })
                    }
                }
                
                val mediaItem = MediaItem.fromUri(item.videoUrl)
                player?.setMediaItem(mediaItem)
                player?.prepare()
                player?.playWhenReady = true
                ivPause.visibility = View.GONE
                isPaused = false
                
                // 启动音乐转盘旋转动画
                startDiscAnimation()
            }
        }
        
        /**
         * 预加载视频（准备但不播放）
         */
        fun preloadVideo() {
            currentItem?.let { item ->
                if (player == null) {
                    player = ExoPlayer.Builder(itemView.context).build().apply {
                        playerView.player = this
                        repeatMode = Player.REPEAT_MODE_ONE
                        playWhenReady = false // 不自动播放
                    }
                    
                    val mediaItem = MediaItem.fromUri(item.videoUrl)
                    player?.setMediaItem(mediaItem)
                    player?.prepare() // 只准备，不播放
                }
            }
        }
        
        private fun startDiscAnimation() {
            if (discAnimation == null) {
                discAnimation = AnimationUtils.loadAnimation(itemView.context, R.anim.rotate_disc)
            }
            ivMusicDisc.startAnimation(discAnimation)
        }
        
        private fun stopDiscAnimation() {
            ivMusicDisc.clearAnimation()
        }
        
        fun stopPlay() {
            player?.stop()
            ivCover.visibility = View.VISIBLE
            stopDiscAnimation()
        }
        
        fun pausePlay() {
            player?.pause()
            ivPause.visibility = View.VISIBLE
            stopDiscAnimation()
        }
        
        fun resumePlay() {
            if (!isPaused) {
                player?.play()
                ivPause.visibility = View.GONE
                startDiscAnimation()
            }
        }
        
        fun releasePlayer() {
            player?.release()
            player = null
        }
    }
    
    class VideoDiffCallback : DiffUtil.ItemCallback<VideoItem>() {
        override fun areItemsTheSame(oldItem: VideoItem, newItem: VideoItem): Boolean {
            return oldItem.id == newItem.id
        }
        
        override fun areContentsTheSame(oldItem: VideoItem, newItem: VideoItem): Boolean {
            return oldItem == newItem
        }
    }
}
