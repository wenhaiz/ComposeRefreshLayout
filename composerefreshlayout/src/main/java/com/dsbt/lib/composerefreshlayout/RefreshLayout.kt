package com.dsbt.lib.composerefreshlayout

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
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.collectLatest
import kotlin.math.roundToInt

private const val TAG = "RefreshLayout"

/**
 * A refresh layout that can be used to refresh and load more data.
 *
 * The refresh layout is composed of a header and a footer, and the content is placed in the middle.
 * When the content is pulled down, the header will be displayed, and when the content is pulled up,
 * the footer will be displayed.
 *
 * Thanks Github Copilot for helping me write this comment :)
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
    //For some special cases, you may need to set the padding of the footer to make it float on the content(there may be something cover the footer).
    footerPaddingBottom: Dp = Dp.Unspecified,
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
            enableLoadMore = enableLoadMore,
        )
    }.apply {
        this.enableRefresh = enableRefresh
        this.enableLoadMore = enableLoadMore
    }
    LaunchedEffect(state.refreshingState) {
        snapshotFlow {
            state.refreshingState.componentStatus
        }.collectLatest {
            if (it == ActionComponentStatus.ActionInProgress) {
                onRefresh()
                state.animateOffsetTo(headerHeight.toFloat())
            } else if (it.isResetting) {
                val stateOffsetY = state.offsetY
                val d1 = async {
                    if (stateOffsetY != 0f) {
                        contentScrollState?.animateScrollBy(-stateOffsetY)
                    }
                }
                val d2 = async {
                    state.animateOffsetTo(0f)
                }
                d1.await()
                d2.await()
                state.idle()
            } else if (it == ActionComponentStatus.IDLE) {
                state.animateOffsetTo(0f)
            }
        }
    }
    LaunchedEffect(state.loadingMoreState) {
        snapshotFlow {
            state.loadingMoreState.componentStatus
        }.collectLatest {
            if (it == ActionComponentStatus.ActionInProgress) {
                onLoadMore()
                state.animateOffsetTo(-footerHeight.toFloat())
            } else if (it.isResetting) {
                val stateOffsetY = state.offsetY
                val d1 = async {
                    if (stateOffsetY != 0f) {
                        contentScrollState?.animateScrollBy(-stateOffsetY)
                    }
                }
                val d2 = async {
                    state.animateOffsetTo(0f)
                }
                d1.await()
                d2.await()
                state.idle()
            } else if (it == ActionComponentStatus.IDLE) {
                state.animateOffsetTo(0f)
            }
        }
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clipToBounds()
            .nestedScroll(conn)
    ) {
        //header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .layout { measurable, constraints ->
                    val placeable = measurable.measure(constraints)
                    layout(placeable.width, placeable.height) {
                        placeable.placeRelative(0, -placeable.height)
                    }
                }
                .graphicsLayer {
                    //translate the header by the offset if we are refreshing (scrolling down)
                    translationY = if (state.offsetY > 0) state.offsetY else 0f
                }
                .onSizeChanged {
                    state.refreshingState.triggerDistancePx = it.height.toFloat()
                    headerHeight = it.height
                }
        ) {
            header(state.refreshingState)
        }
        //content
        Box(modifier = Modifier
            .fillMaxSize()
            .offset {
                //Move the content by the offset
                IntOffset(0, state.offsetY.roundToInt())
            }) {
            content(state)
        }
        //footer
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .layout { measurable, constraints ->
                    val placeable = measurable.measure(constraints)
                    layout(placeable.width, placeable.height) {
                        placeable.placeRelative(0, placeable.height)
                    }
                }
                .then(if (footerPaddingBottom != Dp.Unspecified) Modifier.offset(y = -footerPaddingBottom) else Modifier)
                .graphicsLayer {
                    //translate the footer by the offset if we are loading more (scrolling up)
                    translationY = if (state.offsetY < 0) state.offsetY else 0f
                }
                .onSizeChanged {
                    state.loadingMoreState.triggerDistancePx = it.height.toFloat()
                    footerHeight = it.height
                }
                .align(Alignment.BottomCenter)
        ) {
            footer(state.loadingMoreState)
        }
    }
}