package com.dsbt.lib.composerefreshlayout

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.unit.Velocity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue


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