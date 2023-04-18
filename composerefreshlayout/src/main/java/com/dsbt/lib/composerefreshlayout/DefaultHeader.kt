package com.dsbt.lib.composerefreshlayout

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp


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