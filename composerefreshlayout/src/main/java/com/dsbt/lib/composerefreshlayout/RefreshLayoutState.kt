package com.dsbt.lib.composerefreshlayout

import android.util.Log
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.MutatorMutex
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import kotlin.math.absoluteValue

private const val TAG = "RefreshLayoutState"

enum class GestureState {
    IDLE,
    Dragging,
    ReadyForAction,
    InProgress,
    Success,
    Failed,
    Resetting;

    val isResetting: Boolean
        get() = this == Resetting

    val isFinishing: Boolean
        get() = this == Success || this == Failed || this == Resetting

    val isDragging: Boolean
        get() = this == Dragging
}

/**
 * Helper function to create a [RefreshLayoutState] that is remembered across compositions.
 */
@Composable
fun rememberRefreshLayoutState(): RefreshLayoutState {
    return remember { RefreshLayoutState() }
}

sealed class DragState constructor(
    private val _offsetY: State<Float>,
) {
    internal var _gestureState: GestureState by mutableStateOf(GestureState.IDLE)
    internal var _hasMoreData: Boolean by mutableStateOf(true)
    internal var triggerDistancePx: Float = 0f

    abstract val dragProgress: Float

    val gestureState: GestureState
        get() = _gestureState
    val hasMoreData: Boolean
        get() = _hasMoreData

    val offsetY: Float
        get() = _offsetY.value

    class RefreshState internal constructor(offsetY: State<Float>) : DragState(offsetY) {

        override val dragProgress: Float
            get() = if (offsetY <= 0) 0f else (offsetY / triggerDistancePx).absoluteValue

    }

    class LoadMoreState internal constructor(offsetY: State<Float>) : DragState(offsetY) {
        override val dragProgress: Float
            get() = if (offsetY >= 0) 0f else (offsetY / triggerDistancePx).absoluteValue

    }

}

/**
 * Internal state of the [RefreshLayout], used to track the current state of the refresh and load and control
 * the scroll offset.
 */
class RefreshLayoutState {

    internal var maxScrollUpPx = 0
        private set
    internal var maxScrollDownPx = 0
        private set

    internal var refreshTriggerPx: Int
        set(value) {
            dragState.triggerDistancePx = value.toFloat()
            maxScrollDownPx = value + 100
        }
        get() = dragState.triggerDistancePx.toInt()


    internal var loadMoreTriggerPx: Int
        set(value) {
            loadMoreState.triggerDistancePx = value.toFloat()
            maxScrollUpPx = value.absoluteValue + 100
        }
        get() = loadMoreState.triggerDistancePx.toInt()


    private val _offsetY = Animatable(0f)

    val dragState = DragState.RefreshState(_offsetY.asState())
    val loadMoreState = DragState.LoadMoreState(_offsetY.asState())

    private val mutatorMutex = MutatorMutex()
    val offsetY: Float
        get() = _offsetY.value

    internal suspend fun animateOffsetTo(v: Float) {
        return mutatorMutex.mutate {
            if (offsetY != v) {
                _offsetY.animateTo(v)
            }
        }
    }

    internal suspend fun dispatchScrollDelta(delta: Float) {
        if (delta == 0f) {
            return
        }
        mutatorMutex.mutate {
            Log.d(TAG, "dispatchScrollDelta: delta=$delta")
            val newValue = _offsetY.value + delta
            if (newValue > 0) {
                //scroll down
                if (newValue >= refreshTriggerPx && dragState._hasMoreData) {
                    dragState._gestureState = GestureState.ReadyForAction
                } else {
                    dragState._gestureState = GestureState.Dragging
                }
            } else if (newValue < 0) {
                //scroll up
                if (newValue.absoluteValue >= loadMoreTriggerPx && loadMoreState._hasMoreData) {
                    loadMoreState._gestureState = GestureState.ReadyForAction
                } else {
                    loadMoreState._gestureState = GestureState.Dragging
                }
            }
            _offsetY.snapTo(newValue)
        }
    }

    internal fun startRefresh() {
        dragState._gestureState = GestureState.InProgress
    }


    internal fun startLoadMore() {
        loadMoreState._gestureState = GestureState.InProgress
    }

    internal fun idle() {
        if (dragState._gestureState != GestureState.IDLE) {
            dragState._gestureState = GestureState.IDLE
        }
        if (loadMoreState._gestureState != GestureState.IDLE) {
            loadMoreState._gestureState = GestureState.IDLE
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
        loadMoreState._gestureState =
            if (success) GestureState.Success else GestureState.Failed
        delay(delay)
        loadMoreState._gestureState = GestureState.Resetting
        loadMoreState._hasMoreData = hasMoreData
    }

    /**
     * Finish refresh
     *
     * @param success refresh success or not
     * @param hasMoreData has more data or not
     * @param delay Duration of the result message display
     */
    suspend fun finishRefresh(success: Boolean, hasMoreData: Boolean = true, delay: Long = 1000) {
        dragState._gestureState =
            if (success) GestureState.Success else GestureState.Failed
        delay(delay)
        dragState._gestureState = GestureState.Resetting
        dragState._hasMoreData = hasMoreData
        //FIXME:This may be tricky
        loadMoreState._hasMoreData = hasMoreData
    }

}
