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


enum class GestureState {
    IDLE,
    Dragging,
    ReadyForAction,
    InProgress,
    Success,
    Failed,
    Resetting;

    val isResetting: Boolean
        get() = this == Resetting || this == IDLE

    val isFinishing: Boolean
        get() = this == Success || this == Failed || this == Resetting
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
        val gestureState: GestureState = GestureState.IDLE,
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
    internal var loadMoreTriggerPx = 300
        set(value) {
            field = value
            maxScrollUpPx = value.absoluteValue + 100
        }

    var refreshState by mutableStateOf(RefreshState())
        private set
    var loadMoreState by mutableStateOf(RefreshState())
        private set

    val refreshDragProgress: Float
        get() = (offsetY / refreshTriggerPx).absoluteValue

    val loadMoreDragProgress: Float
        get() = (offsetY / loadMoreTriggerPx).absoluteValue


    val isDragging: Boolean
        get() = refreshState.gestureState == GestureState.Dragging || loadMoreState.gestureState == GestureState.Dragging

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
                refreshState = if (newValue >= refreshTriggerPx) {
                    refreshState.copy(gestureState = GestureState.ReadyForAction)
                } else {
                    refreshState.copy(gestureState = GestureState.Dragging)
                }
            } else if (newValue < 0) {
                //scroll up
                loadMoreState = if (newValue.absoluteValue >= loadMoreTriggerPx) {
                    loadMoreState.copy(gestureState = GestureState.ReadyForAction)
                } else {
                    loadMoreState.copy(gestureState = GestureState.Dragging)
                }
            }
            _offsetY.snapTo(newValue)
        }
    }

    internal fun startRefresh() {
        refreshState = refreshState.copy(gestureState = GestureState.InProgress)
    }


    internal fun startLoadMore() {
        loadMoreState = loadMoreState.copy(gestureState = GestureState.InProgress)
    }

    internal fun idle() {
        if (refreshState.gestureState != GestureState.IDLE) {
            refreshState = refreshState.copy(gestureState = GestureState.IDLE)
        }
        if (loadMoreState.gestureState != GestureState.IDLE) {
            loadMoreState = loadMoreState.copy(gestureState = GestureState.IDLE)
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
        loadMoreState = loadMoreState.copy(gestureState = if (success) GestureState.Success else GestureState.Failed)
        delay(delay)
        loadMoreState = RefreshState(gestureState = GestureState.Resetting, hasMoreData = hasMoreData)
    }

    /**
     * Finish refresh
     *
     * @param success refresh success or not
     * @param hasMoreData has more data or not
     * @param delay Duration of the result message display
     */
    suspend fun finishRefresh(success: Boolean, hasMoreData: Boolean = true, delay: Long = 1000) {
        refreshState = refreshState.copy(gestureState = if (success) GestureState.Success else GestureState.Failed)
        delay(delay)
        refreshState = RefreshState(gestureState = GestureState.Resetting, hasMoreData = hasMoreData)
        loadMoreState = loadMoreState.copy(hasMoreData = hasMoreData)
    }

}
