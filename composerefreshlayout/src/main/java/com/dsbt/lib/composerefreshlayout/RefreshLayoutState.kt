package com.dsbt.lib.composerefreshlayout

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationResult
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.foundation.MutatorMutex
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import kotlin.math.absoluteValue


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

/**
 * Helper function to create a [RefreshLayoutState] that is remembered across compositions.
 */
@Composable
fun rememberRefreshLayoutState(): RefreshLayoutState {
    return remember { RefreshLayoutState() }
}

/**
 * Internal state of the [RefreshLayout], used to track the current state of the refresh and load and control
 * the scroll offset.
 */
@Stable
class RefreshLayoutState {

    data class RefreshState(
        val state: State = State.IDLE,
        val hasMoreData: Boolean = true,
    )

    internal var maxScrollUpPx = 0
        private set
    internal var maxScrollDownPx = 0
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


    val isDragging: Boolean
        get() = isRefreshing.state == State.Dragging || isLoadingMore.state == State.Dragging

    private val _offsetY = Animatable(0f)
    private val mutatorMutex = MutatorMutex()
    val offsetY: Float
        get() = _offsetY.value

    internal suspend fun animateOffsetTo(v: Float): AnimationResult<Float, AnimationVector1D> {
        return mutatorMutex.mutate {
            _offsetY.animateTo(v)
        }
    }

    internal suspend fun dispatchScrollDelta(delta: Float) {
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

    internal fun startRefresh() {
        isRefreshing = isRefreshing.copy(state = State.InProgress)
    }


    internal fun startLoadMore() {
        isLoadingMore = isLoadingMore.copy(state = State.InProgress)
    }

    internal fun idle() {
        if (isRefreshing.state != State.IDLE) {
            isRefreshing = isRefreshing.copy(state = State.IDLE)
        }
        if (isLoadingMore.state != State.IDLE) {
            isLoadingMore = isLoadingMore.copy(state = State.IDLE)
        }
    }


    /**
     * Finish load more
     *
     * @param success load more success or not
     * @param hasMoreData has more data or not
     * @param delay Duration of the result message display
     */
    suspend fun finishLoadMore(success: Boolean, hasMoreData: Boolean, delay: Long = 1000) {
        isLoadingMore = isLoadingMore.copy(state = if (success) State.Success else State.Failed)
        delay(delay)
        isLoadingMore = RefreshState(state = State.Resetting, hasMoreData = hasMoreData)
    }

    /**
     * Finish refresh
     *
     * @param success refresh success or not
     * @param hasMoreData has more data or not
     * @param delay Duration of the result message display
     */
    suspend fun finishRefresh(success: Boolean, hasMoreData: Boolean = true, delay: Long = 1000) {
        isRefreshing = isRefreshing.copy(state = if (success) State.Success else State.Failed)
        delay(delay)
        isRefreshing = RefreshState(state = State.Resetting, hasMoreData = hasMoreData)
        isLoadingMore = isLoadingMore.copy(hasMoreData = hasMoreData)
    }

}
