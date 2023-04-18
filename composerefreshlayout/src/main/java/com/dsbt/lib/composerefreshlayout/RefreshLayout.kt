package com.dsbt.lib.composerefreshlayout

import android.util.Log
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationResult
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.foundation.MutatorMutex
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import com.google.android.material.progressindicator.CircularProgressIndicator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

private const val TAG = "RefreshLayout"

class RefreshNestedScrollConnection(
    val state: RefreshLayoutState,
    val coroutineScope: CoroutineScope,
    var enableRefresh: Boolean,
    var enableLoadMore: Boolean,
) : NestedScrollConnection {

    private val defaultMultiplier = 1f
    private var multiplier = defaultMultiplier

    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        return when {
            source != NestedScrollSource.Drag -> Offset.Zero
//            state.isRefreshing.isRefreshing || state.isLoadingMore.isLoadingMore -> Offset.Zero
            else -> onPreScroll(available)
        }
    }

    private fun onPreScroll(available: Offset): Offset {
        return if (available.y < 0) {
            if (state.offsetY > 0 && enableRefresh) {
                val y = available.y
                val newOffsetY = (state.offsetY + y).coerceAtLeast(0f)
                val delta = newOffsetY - state.offsetY
                onScroll(delta)
                Offset(0f, delta)
            } else {
                Offset.Zero
            }
        } else {
            if (state.offsetY < 0) {
                val y = available.y
                val newOffsetY = (state.offsetY + y).coerceAtMost(0f)
                val delta = newOffsetY - state.offsetY
                onScroll(delta)
                Offset(0f, delta)
            } else {
                Offset.Zero
            }
        }

    }


    override fun onPostScroll(
        consumed: Offset,
        available: Offset,
        source: NestedScrollSource
    ): Offset {
        return when {
            source != NestedScrollSource.Drag -> Offset.Zero
            state.isRefreshing.state == State.InProgress || state.isLoadingMore.state == State.InProgress -> Offset.Zero
            else -> onPostScroll(available)
        }

    }

    private fun onPostScroll(available: Offset): Offset {
        if (available.y > 0 && !enableRefresh) {
            return Offset.Zero
        }
        if (available.y < 0 && !enableLoadMore) {
            return Offset.Zero
        }
        val y = available.y * multiplier
        Log.d(TAG, "onPostScroll: $y, ${available.y},multiplier:$multiplier")
        val newOffsetY = state.offsetY + y
        val delta = newOffsetY - state.offsetY
        onScroll(delta)
        updateMultiplier(if (available.y > 0) 1 else -1)
        return Offset(0f, available.y)
    }

    private fun updateMultiplier(dir: Int) {
        // 这里根据滑动距离衰减,可以优化一下这里的算法
        val max = if (dir > 0) state.maxScrollDownPx else state.maxScrollUpPx
        multiplier = (0.9f - state.offsetY.absoluteValue / max).coerceAtLeast(0f)
    }

    override suspend fun onPreFling(available: Velocity): Velocity {
        if (state.isRefreshing.state == State.ReadyForAction && state.isRefreshing.hasMoreData) {
            state.startRefresh()
        } else if (state.isLoadingMore.state == State.ReadyForAction && state.isLoadingMore.hasMoreData) {
            state.startLoadMore()
        } else {
            state.idle()
        }
        multiplier = defaultMultiplier
        return Velocity.Zero
    }

    private fun onScroll(delta: Float) {
        coroutineScope.launch {
            state.dispatchScrollDelta(delta)
        }
    }
}

enum class State {
    IDLE,
    Dragging,
    ReadyForAction,
    InProgress,
    Success,
    Failed,
    Resetting;

    val isResetting: Boolean
        get() = this == Resetting || this == IDLE
}

@Stable
class RefreshLayoutState {

    data class RefreshState(
        val state: State = State.IDLE,
        val hasMoreData: Boolean = true,
    )

    var maxScrollUpPx = 0
        private set
    var maxScrollDownPx = 0
        private set

    internal var refreshTriggerPx = 300
        set(value) {
            field = value
            maxScrollDownPx = value + 100
        }
    internal var loadMoreTriggerPx = -300
        set(value) {
            field = value
            maxScrollUpPx = value.absoluteValue + 100
        }

    var isRefreshing by mutableStateOf(RefreshState())
        private set
    var isLoadingMore by mutableStateOf(RefreshState())
        private set


//    val isDragging: Boolean
//        get() = isRefreshing.state == State.Dragging || isLoadingMore.state == State.Dragging

    private val _offsetY = Animatable(0f)
    private val mutatorMutex = MutatorMutex()
    val offsetY: Float
        get() = _offsetY.value

    suspend fun animateOffsetTo(v: Float): AnimationResult<Float, AnimationVector1D> {
        return mutatorMutex.mutate {
            _offsetY.animateTo(v)
        }
    }

    suspend fun dispatchScrollDelta(delta: Float) {
        mutatorMutex.mutate {
            val newValue = _offsetY.value + delta
            if (newValue > 0) {
                //scroll down
                isRefreshing = if (newValue >= refreshTriggerPx) {
                    isRefreshing.copy(state = State.ReadyForAction)
                } else {
                    isRefreshing.copy(state = State.Dragging)
                }
            } else if (newValue < 0) {
                //scroll up
                isLoadingMore = if (newValue <= loadMoreTriggerPx) {
                    isLoadingMore.copy(state = State.ReadyForAction)
                } else {
                    isLoadingMore.copy(state = State.Dragging)
                }
            }
            _offsetY.snapTo(newValue)
        }
    }

    suspend fun finishLoadMore(success: Boolean, hasMoreData: Boolean, delay: Long = 1000) {
        isLoadingMore = isLoadingMore.copy(state = if (success) State.Success else State.Failed)
        delay(delay)
        isLoadingMore = RefreshState(state = State.Resetting, hasMoreData = hasMoreData)
    }

    suspend fun finishRefresh(success: Boolean, hasMoreData: Boolean = true, delay: Long = 1000) {
        isRefreshing = isRefreshing.copy(state = if (success) State.Success else State.Failed)
        delay(delay)
        isRefreshing = RefreshState(state = State.Resetting, hasMoreData = hasMoreData)
        isLoadingMore = isLoadingMore.copy(hasMoreData = hasMoreData)
    }

    fun startRefresh() {
        isRefreshing = isRefreshing.copy(state = State.InProgress)
    }


    fun startLoadMore() {
        isLoadingMore = isLoadingMore.copy(state = State.InProgress)
    }

    fun idle() {
        if (isRefreshing.state != State.IDLE) {
            isRefreshing = isRefreshing.copy(state = State.IDLE)
        }
        if (isLoadingMore.state != State.IDLE) {
            isLoadingMore = isLoadingMore.copy(state = State.IDLE)
        }
    }

}


@Composable
fun RefreshLayout(
    state: RefreshLayoutState,
    modifier: Modifier = Modifier,
    header: @Composable BoxScope.(RefreshLayoutState) -> Unit = {
        DefaultRefreshHeader(state = it)
    },
    footer: @Composable BoxScope.(RefreshLayoutState) -> Unit = {
        DefaultRefreshFooter(state = it)
    },
    enableRefresh: Boolean = true,
    enableLoadMore: Boolean = true,
    onRefresh: () -> Unit = {},
    onLoadMore: () -> Unit = {},
    contentScrollState: ScrollableState,
    content: @Composable BoxScope.(RefreshLayoutState) -> Unit,
) {
    var headerHeight by remember { mutableStateOf(0) }
    var footerHeight by remember { mutableStateOf(0) }
    val coroutineScope = rememberCoroutineScope()
    LaunchedEffect(state.isRefreshing) {
        coroutineScope.launch {
            if (state.isRefreshing.state == State.InProgress) {
                onRefresh()
                state.animateOffsetTo(headerHeight.toFloat())
            } else if (state.isRefreshing.state.isResetting) {
                state.animateOffsetTo(0f)
                state.idle()
            }
        }
    }
    LaunchedEffect(state.isLoadingMore) {
        coroutineScope.launch {
            if (state.isLoadingMore.state == State.InProgress) {
                onLoadMore()
                state.animateOffsetTo(-footerHeight.toFloat())
            } else if (state.isLoadingMore.state.isResetting) {
                state.animateOffsetTo(0f)
                state.idle()
            }
        }
    }
    val conn = remember {
        RefreshNestedScrollConnection(
            state = state,
            coroutineScope = coroutineScope,
            enableRefresh = enableRefresh,
            enableLoadMore = enableLoadMore
        )
    }.apply {
        this.enableRefresh = enableRefresh
        this.enableLoadMore = enableLoadMore
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clipToBounds()
            .nestedScroll(conn)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset {
                    //根据offsetY来设置偏移
                    IntOffset(0, -headerHeight)
                }
                .graphicsLayer {
                    //根据offsetY来设置偏移
                    val y = if (state.offsetY > 0) state.offsetY else 0f
                    translationY = y
                }
                .onSizeChanged {
                    state.refreshTriggerPx = it.height
                    headerHeight = it.height
                }
        ) {
            header(state)
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset {
                    //根据offsetY来设置偏移
                    IntOffset(0, footerHeight)
                }
                .graphicsLayer {
                    //根据offsetY来设置偏移
                    val y = if (state.offsetY < 0) state.offsetY else 0f
                    translationY = y
                }
                .onSizeChanged {
                    state.loadMoreTriggerPx = -it.height
                    footerHeight = it.height
                }
                .align(Alignment.BottomCenter)
        ) {
            footer(state)
        }
        var stateOffsetY = remember {
            0f
        }
        LaunchedEffect(state.isLoadingMore) {
            if (state.isLoadingMore.state == State.InProgress) {
                stateOffsetY = state.offsetY
            } else if (state.isLoadingMore.state.isResetting) {
                val delta = stateOffsetY - state.offsetY
                stateOffsetY = state.offsetY
                if (delta != 0f) {
                    contentScrollState.animateScrollBy(delta)
                }
            }
        }
        LaunchedEffect(state.isRefreshing) {
            if (state.isRefreshing.state == State.InProgress) {
                stateOffsetY = state.offsetY
            } else if (state.isRefreshing.state.isResetting) {
                val delta = stateOffsetY - state.offsetY
                stateOffsetY = state.offsetY
                if (delta != 0f) {
                    contentScrollState.animateScrollBy(delta)
                }
            }
        }
        Box(modifier = Modifier
            .fillMaxSize()
            .offset {
                //根据offsetY来设置偏移
                IntOffset(0, state.offsetY.toInt())
            }) {
            content(state)
        }
    }
}


@Composable
fun DefaultRefreshHeader(state: RefreshLayoutState) {
    val heightDp = 80.dp
    var text by remember {
        mutableStateOf("")
    }
    val newText = when {
        state.isRefreshing.state == State.Resetting -> ""
        (state.isRefreshing.state == State.IDLE || state.isRefreshing.state == State.Dragging) && !state.isRefreshing.hasMoreData -> "没有更多数据了"
        state.isRefreshing.state == State.InProgress -> "正在刷新"
        state.isRefreshing.state == State.Success -> "刷新成功"
        state.isRefreshing.state == State.Failed -> "刷新失败"
        state.isRefreshing.state == State.ReadyForAction -> "松开刷新"
        else -> "下拉可以刷新"
    }
    if (newText.isNotEmpty()) {
        text = newText
    }
    Box(
        modifier = Modifier
            .height(heightDp)
            .fillMaxWidth()
    ) {
        Text(text = text, modifier = Modifier.align(Alignment.Center))
    }
}

@Composable
fun DefaultRefreshFooter(state: RefreshLayoutState) {
    val heightDp = 80.dp
    var text by remember {
        mutableStateOf("")
    }
    val newText = when {
        state.isLoadingMore.state == State.Resetting -> ""
        (state.isLoadingMore.state == State.IDLE || state.isLoadingMore.state == State.Dragging) && !state.isLoadingMore.hasMoreData -> "没有更多数据了"
        state.isLoadingMore.state == State.InProgress -> "正在加载..."
        state.isLoadingMore.state == State.Success -> "加载成功"
        state.isLoadingMore.state == State.Failed -> "加载失败"
        state.isLoadingMore.state == State.ReadyForAction -> "松开加载更多"
        else -> "上滑加载更多"
    }
    if (newText.isNotEmpty()) {
        text = newText
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(heightDp)
    ) {
        Row(
            modifier = Modifier.align(Alignment.Center),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (state.isLoadingMore.state == State.InProgress) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .padding(end = 12.dp)
                        .size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colors.primary
                )
            }
            Text(text = text, modifier = Modifier)
        }
    }

}