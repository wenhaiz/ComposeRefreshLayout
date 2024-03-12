package com.dsbt.lib.composerefreshlayout

import android.util.Log
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.spring
import androidx.compose.foundation.MutatorMutex
import androidx.compose.runtime.*
import kotlinx.coroutines.delay
import kotlin.math.absoluteValue

private const val TAG = "RefreshLayoutState"

/**
 * A normal process is: IDLE -> Dragging -> ReadyForAction -> ActionInProgress -> ActionSuccess/ActionFailed -> Resetting -> IDLE
 */
enum class ActionComponentStatus {
    IDLE,
    Dragging,
    ReadyForAction,
    ActionInProgress,
    ActionSuccess,
    ActionFailed,
    Resetting;

    val isResetting: Boolean
        get() = this == Resetting

    val isFinishing: Boolean
        get() = this == ActionSuccess || this == ActionFailed || this == Resetting

    val canAcceptScroll: Boolean
        get() = this == IDLE || this == Dragging || this == ReadyForAction

    val shouldRejectScroll: Boolean
        get() = this == ActionInProgress || this == ActionSuccess || this == ActionFailed || this == Resetting
}

/**
 * Helper function to create a [RefreshLayoutState] that is remembered across compositions.
 */
@Composable
fun rememberRefreshLayoutState(): RefreshLayoutState {
    return remember { RefreshLayoutState() }
}

sealed class ActionState(
    private val _offsetY: State<Float>,
) {
    var componentStatus: ActionComponentStatus by mutableStateOf(ActionComponentStatus.IDLE)
        private set
    var hasMoreData: Boolean by mutableStateOf(true)
        internal set
    internal var triggerDistancePx: Float = 0f

    abstract val dragProgress: Float

    val offsetY: Float
        get() = _offsetY.value

    override fun toString(): String {
        return "ActionState(offsetY=$offsetY, componentStatus=$componentStatus, hasMoreData=$hasMoreData, triggerDistancePx=$triggerDistancePx)"
    }

    class RefreshingState internal constructor(offsetY: State<Float>) : ActionState(offsetY) {

        override val dragProgress: Float
            get() = if (offsetY <= 0) 0f else (offsetY / triggerDistancePx).absoluteValue

    }

    class LoadingMoreState internal constructor(offsetY: State<Float>) : ActionState(offsetY) {
        override val dragProgress: Float
            get() = if (offsetY >= 0) 0f else (offsetY / triggerDistancePx).absoluteValue

    }

    internal fun updateComponentStatus(status: ActionComponentStatus) {
        if (status == componentStatus) {
            return
        }
        Log.d(
            TAG,
            "${this.javaClass.simpleName} updateComponentStatus:$componentStatus -> $status "
        )
        componentStatus = status
    }

}

/**
 * Internal state of the [RefreshLayout], used to track the current state of the refresh and load and control
 * the scroll offset.
 */
class RefreshLayoutState {

    internal val maxScrollUpPx: Float
        get() = loadingMoreState.triggerDistancePx.absoluteValue + 100

    internal val maxScrollDownPx: Float
        get() = refreshingState.triggerDistancePx + 100

    /**
     * The current offset of the refresh indicator.
     */
    private val _offsetY = Animatable(0f)

    val refreshingState = ActionState.RefreshingState(_offsetY.asState())
    val loadingMoreState = ActionState.LoadingMoreState(_offsetY.asState())

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
        if (delta.absoluteValue < 0.001f) {
            return
        }
        mutatorMutex.mutate {
            var newOffsetY = _offsetY.value + delta
            Log.d(TAG, "dispatchScrollDelta: delta=$delta,newOffsetY=$newOffsetY")
            if (newOffsetY > 0) {
                //scroll down
                if (newOffsetY >= refreshingState.triggerDistancePx && refreshingState.hasMoreData) {
                    refreshingState.updateComponentStatus(ActionComponentStatus.ReadyForAction)
                } else {
                    refreshingState.updateComponentStatus(ActionComponentStatus.Dragging)
                }
                newOffsetY = newOffsetY.coerceAtMost(refreshingState.triggerDistancePx + 100f)
            } else if (newOffsetY < 0) {
                //scroll up
                if (newOffsetY.absoluteValue >= loadingMoreState.triggerDistancePx && loadingMoreState.hasMoreData) {
                    loadingMoreState.updateComponentStatus(ActionComponentStatus.ReadyForAction)
                } else {
                    loadingMoreState.updateComponentStatus(ActionComponentStatus.Dragging)
                }
                newOffsetY = newOffsetY.coerceAtLeast(-loadingMoreState.triggerDistancePx - 100f)
            }
            _offsetY.snapTo(newOffsetY)
        }
    }

    internal fun startRefresh() {
        refreshingState.updateComponentStatus(ActionComponentStatus.ActionInProgress)
    }


    internal fun startLoadMore() {
        loadingMoreState.updateComponentStatus(ActionComponentStatus.ActionInProgress)
    }

    internal fun idle() {
        if (refreshingState.componentStatus != ActionComponentStatus.IDLE) {
            refreshingState.updateComponentStatus(ActionComponentStatus.IDLE)
        }
        if (loadingMoreState.componentStatus != ActionComponentStatus.IDLE) {
            loadingMoreState.updateComponentStatus(ActionComponentStatus.IDLE)
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
        if (loadingMoreState.componentStatus == ActionComponentStatus.ActionInProgress) {
            loadingMoreState.updateComponentStatus(
                if (success) ActionComponentStatus.ActionSuccess else ActionComponentStatus.ActionFailed
            )
            delay(delay)
            loadingMoreState.updateComponentStatus(ActionComponentStatus.Resetting)
        }
        loadingMoreState.hasMoreData = hasMoreData
    }

    /**
     * Finish refresh
     *
     * @param success refresh success or not
     * @param hasMoreData has more data or not
     * @param delay Duration of the result message display
     */
    suspend fun finishRefresh(
        success: Boolean,
        hasMoreData: Boolean = true,
        delay: Long = 1000
    ) {
        if (refreshingState.componentStatus == ActionComponentStatus.ActionInProgress) {
            refreshingState.updateComponentStatus(
                if (success) ActionComponentStatus.ActionSuccess else ActionComponentStatus.ActionFailed
            )
            delay(delay)
            refreshingState.updateComponentStatus(ActionComponentStatus.Resetting)
        }
//        refreshingState.hasMoreData = hasMoreData
        loadingMoreState.hasMoreData = hasMoreData
    }


    suspend fun refresh(animationSpec: AnimationSpec<Float> = spring()) {
        _offsetY.animateTo(refreshingState.triggerDistancePx, animationSpec = animationSpec)
        startRefresh()
    }

    suspend fun loadMore(animationSpec: AnimationSpec<Float> = spring()) {
        _offsetY.animateTo(-loadingMoreState.triggerDistancePx, animationSpec = animationSpec)
        startLoadMore()
    }

}
