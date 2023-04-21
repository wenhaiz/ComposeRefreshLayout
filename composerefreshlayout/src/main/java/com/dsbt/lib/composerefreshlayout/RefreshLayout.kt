package com.dsbt.lib.composerefreshlayout

import android.util.Log
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntOffset

private const val TAG = "RefreshLayout"

/**
 * A refresh layout that can be used to refresh and load more data.
 *
 * The refresh layout is composed of a header and a footer, and the content is placed in the middle.
 * When the content is pulled down, the header will be displayed, and when the content is pulled up,
 * the footer will be displayed.
 *
 * Thanks to Github Copilot to generate this comment :)
 *
 *
 * @param state The state of the refresh layout. You can use [rememberRefreshLayoutState] to create one.
 * @param modifier The modifier to be applied to the layout.
 * @param header The header of the refresh layout. Default is [DefaultRefreshHeader].
 * @param footer The footer of the refresh layout. Default is [DefaultRefreshFooter].
 * @param enableRefresh Whether to enable refresh.
 * @param enableLoadMore Whether to enable load more.
 * @param onRefresh The callback to be invoked when the refresh action is triggered.
 * @param onLoadMore The callback to be invoked when the load more action is triggered.
 * @param contentScrollState The scroll state of the content, used to scroll the content when loading more.
 * @param content The content of the refresh layout.
 *
 * @see RefreshLayoutState for more information about the state of the refresh layout.
 * @see RefreshNestedScrollConnection for more information about the nested scroll connection.
 */
@Composable
fun RefreshLayout(
    modifier: Modifier = Modifier,
    state: RefreshLayoutState = rememberRefreshLayoutState(),
    header: @Composable BoxScope.(ActionState.RefreshingState) -> Unit = {
        DefaultRefreshHeader(state = it)
    },
    footer: @Composable BoxScope.(ActionState.LoadingMoreState) -> Unit = {
        DefaultRefreshFooter(state = it)
    },
    enableRefresh: Boolean = true,
    enableLoadMore: Boolean = true,
    onRefresh: () -> Unit = {},
    onLoadMore: () -> Unit = {},
    contentScrollState: ScrollableState? = null,
    content: @Composable BoxScope.(RefreshLayoutState) -> Unit,
) {
    var headerHeight by remember { mutableStateOf(0) }
    var footerHeight by remember { mutableStateOf(0) }
    val coroutineScope = rememberCoroutineScope()
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
    LaunchedEffect(state.refreshingState) {
        snapshotFlow {
            state.refreshingState.componentStatus
        }.collect {
            Log.d(TAG, "RefreshLayout: refreshState $it ")
            if (it == ActionComponentStatus.ActionInProgress) {
                onRefresh()
                state.animateOffsetTo(headerHeight.toFloat())
            } else if (it.isResetting) {
                state.animateOffsetTo(0f)
                state.idle()
            } else if (it == ActionComponentStatus.IDLE) {
                state.animateOffsetTo(0f)
            }
        }
    }
    LaunchedEffect(state.loadingMoreState) {
        snapshotFlow {
            state.loadingMoreState.componentStatus
        }.collect {
            Log.d(TAG, "RefreshLayout: loadMoreState $it")
            if (it == ActionComponentStatus.ActionInProgress) {
                onLoadMore()
                state.animateOffsetTo(-footerHeight.toFloat())
            } else if (it.isResetting) {
                state.animateOffsetTo(0f)
                state.idle()
            } else if (it == ActionComponentStatus.IDLE) {
                state.animateOffsetTo(0f)
            }
        }
    }
    var stateOffsetY = remember {
        0f
    }
    LaunchedEffect(state) {
        snapshotFlow {
            Pair(state.offsetY, state.loadingMoreState.componentStatus)
        }.collect {
            if (state.loadingMoreState.componentStatus == ActionComponentStatus.ActionSuccess || state.loadingMoreState.componentStatus == ActionComponentStatus.ActionFailed) {
                stateOffsetY = it.first
                Log.d(TAG, "RefreshLayout: offsetY ${it.first}")
            } else if (it.second == ActionComponentStatus.Resetting) {
                if (stateOffsetY != 0f) {
                    contentScrollState?.animateScrollBy(-stateOffsetY)
                }
            }
        }
    }
    LaunchedEffect(state) {
        snapshotFlow {
            Pair(state.offsetY, state.refreshingState.componentStatus)
        }.collect {
            if (state.refreshingState.componentStatus == ActionComponentStatus.ActionSuccess || state.refreshingState.componentStatus == ActionComponentStatus.ActionFailed) {
                stateOffsetY = it.first
                Log.d(TAG, "RefreshLayout: offsetY ${it.first}")
            } else if (it.second == ActionComponentStatus.Resetting) {
                if (stateOffsetY != 0f) {
                    contentScrollState?.animateScrollBy(-stateOffsetY)
                }
            }
        }
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
                    //Hide the header by moving it off the screen
                    IntOffset(0, -headerHeight)
                }
                .graphicsLayer {
                    //translate the header by the offset if we are refreshing (scrolling down)
                    val y = if (state.offsetY > 0) state.offsetY else 0f
                    translationY = y
                }
                .onSizeChanged {
                    state.refreshingState.triggerDistancePx = it.height.toFloat()
                    headerHeight = it.height
                }
        ) {
            header(state.refreshingState)
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset {
                    //Hide the footer by moving it off the screen
                    IntOffset(0, footerHeight)
                }
                .graphicsLayer {
                    //translate the footer by the offset if we are loading more (scrolling up)
                    val y = if (state.offsetY < 0) state.offsetY else 0f
                    translationY = y
                }
                .onSizeChanged {
                    state.loadingMoreState.triggerDistancePx = it.height.toFloat()
                    footerHeight = it.height
                }
                .align(Alignment.BottomCenter)
        ) {
            footer(state.loadingMoreState)
        }
        Box(modifier = Modifier
            .fillMaxSize()
            .offset {
                //Move the content by the offset
                IntOffset(0, state.offsetY.toInt())
            }) {
            content(state)
        }
    }
}