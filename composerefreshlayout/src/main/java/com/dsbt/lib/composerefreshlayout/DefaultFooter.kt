package com.dsbt.lib.composerefreshlayout

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp


@Composable
fun DefaultRefreshFooter(state: RefreshLayoutState) {
    val heightDp = 80.dp
    var text by remember {
        mutableStateOf("")
    }
    val newText = when {
        state.isLoadingMore.state == State.Resetting -> ""
        (state.isLoadingMore.state == State.IDLE || state.isLoadingMore.state == State.Dragging) && !state.isLoadingMore.hasMoreData -> stringResource(
            id = R.string.footer_no_more
        )
        state.isLoadingMore.state == State.InProgress -> stringResource(id = R.string.footer_refreshing)
        state.isLoadingMore.state == State.Success -> stringResource(id = R.string.footer_complete)
        state.isLoadingMore.state == State.Failed -> stringResource(id = R.string.footer_failed)
        state.isLoadingMore.state == State.ReadyForAction -> stringResource(id = R.string.footer_pulling)
        else -> stringResource(id = R.string.footer_idle)
    }
    if (newText.isNotEmpty()) {
        text = newText
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(heightDp)
    ) {
        Row(
            modifier = Modifier.align(Alignment.Center),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (state.isLoadingMore.state == State.InProgress) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .padding(end = 12.dp)
                        .size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colors.primary
                )
            }
            Text(text = text, modifier = Modifier)
        }
    }

}