package com.dsbt.lib.composerefreshlayout

import android.util.Log
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.unit.Velocity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

private const val TAG = "RefreshNestedScrollConn"

internal class RefreshNestedScrollConnection(
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
//            state.refreshingState.componentStatus.shouldRejectScroll|| state.loadingMoreState.componentStatus.shouldRejectScroll -> Offset.Zero
            else -> handlePreScroll(available)
        }
    }

    /**
     * cases to handle:
     * scroll up when refreshing
     * scroll down when loading more
     */
    private fun handlePreScroll(available: Offset): Offset {
        val scrollUpWhenRefreshing = available.y < 0 && state.offsetY > 0 && enableRefresh
        val scrollDownWhenLoading = available.y > 0 && state.offsetY < 0 && enableLoadMore
        val needHandle = scrollUpWhenRefreshing || scrollDownWhenLoading
        Log.d(TAG, "handleScroll[Pre]: available = $available,needHandle=$needHandle")
        if (!needHandle) {
            return Offset.Zero
        }
        val y = available.y
        var newOffsetY = state.offsetY + y
        newOffsetY = if (state.offsetY > 0) {
            //in refreshing process
            newOffsetY.coerceAtLeast(0f)
        } else {
            //in loading process
            newOffsetY.coerceAtMost(0f)
        }
        val consumedY = newOffsetY - state.offsetY
        dispatchScroll(consumedY)
        return Offset(0f, consumedY)
    }


    override fun onPostScroll(
        consumed: Offset,
        available: Offset,
        source: NestedScrollSource
    ): Offset {
        Log.d(
            TAG,
            "onPostScroll:available = $available,refreshingState=${state.refreshingState.componentStatus},loadingMoreState=${state.loadingMoreState.componentStatus} "
        )
        return when {
            source != NestedScrollSource.Drag -> Offset.Zero
            available.y > 0 && state.refreshingState.componentStatus.shouldRejectScroll -> Offset.Zero
            available.y < 0 && state.loadingMoreState.componentStatus.shouldRejectScroll -> Offset.Zero
            else -> handlePostScroll(available)
        }

    }

    /**
     * cases to handle:
     * scroll down when refreshing
     * scroll up when loading more
     */
    private fun handlePostScroll(available: Offset): Offset {
        val canRefresh = available.y > 0 && enableRefresh
        val canLoadMore = available.y < 0 && enableLoadMore
        val needHandle = canRefresh || canLoadMore
        Log.d(TAG, "handleScroll[Post]: available = $available,needHandle=$needHandle")
        if (!needHandle) {
            return Offset.Zero
        }
        val y = available.y * multiplier
        val newOffsetY = state.offsetY + y
        val delta = newOffsetY - state.offsetY
        dispatchScroll(delta)
        updateMultiplier(if (available.y > 0) 1 else -1)
        return Offset(0f, available.y)
    }

    private fun updateMultiplier(dir: Int) {
        // 这里根据滑动距离衰减,可以优化一下这里的算法
        val max = if (dir > 0) state.maxScrollDownPx else state.maxScrollUpPx
        multiplier = (1f - state.offsetY.absoluteValue / max).coerceAtLeast(0f)
    }

    override suspend fun onPreFling(available: Velocity): Velocity {
        Log.d(TAG, "onPreFling: available = $available")
        multiplier = defaultMultiplier
        return when {
            state.refreshingState.componentStatus == ActionComponentStatus.ReadyForAction && state.refreshingState.hasMoreData -> {
                state.startRefresh()
                Velocity.Zero
            }

            state.loadingMoreState.componentStatus == ActionComponentStatus.ReadyForAction && state.loadingMoreState.hasMoreData -> {
                state.startLoadMore()
                Velocity.Zero
            }

            state.refreshingState.componentStatus.shouldRejectScroll || state.loadingMoreState.componentStatus.shouldRejectScroll -> {
                available
            }

            else -> {
                state.idle()
                Velocity.Zero
            }
        }
    }

    private fun dispatchScroll(delta: Float) {
        coroutineScope.launch {
            state.dispatchScrollDelta(delta)
        }
    }
}