package com.dsbt.lib.composerefreshlayout

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.unit.Velocity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

private const val TAG = "RefreshNestedScrollConn"

class RefreshNestedScrollConnection(
    val state: RefreshLayoutState,
    val coroutineScope: CoroutineScope,
    var enableRefresh: Boolean,
    var enableLoadMore: Boolean
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
        val scrollUp = available.y < 0 && state.offsetY > 0 && enableRefresh
        val scrollDown = available.y > 0 && state.offsetY < 0 && enableLoadMore
        if (!scrollUp && !scrollDown) {
            return Offset.Zero
        }
        val y = available.y
        var newOffsetY = (state.offsetY + y)
        newOffsetY = if (state.offsetY > 0) {
            newOffsetY.coerceAtLeast(0f)
        } else {
            newOffsetY.coerceAtMost(0f)
        }
        val delta = newOffsetY - state.offsetY
        onScroll(delta)
        return Offset(0f, delta)
    }


    override fun onPostScroll(
        consumed: Offset,
        available: Offset,
        source: NestedScrollSource
    ): Offset {
        return when {
            source != NestedScrollSource.Drag -> Offset.Zero
            state.refreshingState.componentStatus == ActionComponentStatus.ActionInProgress || state.loadingMoreState.componentStatus == ActionComponentStatus.ActionInProgress -> Offset.Zero
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
        if (state.refreshingState.componentStatus == ActionComponentStatus.ReadyForAction && state.refreshingState.hasMoreData) {
            state.startRefresh()
        } else if (state.loadingMoreState.componentStatus == ActionComponentStatus.ReadyForAction && state.loadingMoreState.hasMoreData) {
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