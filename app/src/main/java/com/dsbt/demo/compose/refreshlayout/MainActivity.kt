package com.dsbt.demo.compose.refreshlayout

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.dsbt.demo.compose.refreshlayout.ui.theme.RefreshLayoutComposeTheme
import com.dsbt.lib.composerefreshlayout.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private var cursor = 10
    private var data by mutableStateOf((0 until cursor).map { it })

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            RefreshPagingSample()
//            PullRefreshSample()
        }
    }

    @Composable
    private fun PullRefreshSample() {
        RefreshLayoutComposeTheme {
            // A surface container using the 'background' color from the theme
            val state = rememberRefreshLayoutState()
            val lazyListState = rememberLazyListState()
            val scope = rememberCoroutineScope()
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                Box(
                    modifier = Modifier
                        .navigationBarsPadding()
                        .statusBarsPadding()
                ) {
                    PullRefreshLayout(
                        state = state,
                        onRefresh = {
                            scope.launch {
                                refresh(state)
                            }
                        },
                        enableRefresh = true,
                        header = {
                            DefaultRefreshHeader(state = it)
                        },
                    ) {
                        LazyColumn(state = lazyListState, modifier = Modifier.fillMaxSize()) {
                            items(data, key = { it }) {
                                Text(
                                    text = "text $it",
                                    modifier = Modifier.padding(vertical = 12.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun RefreshPagingSample() {
        RefreshLayoutComposeTheme {
            // A surface container using the 'background' color from the theme
            val state = rememberRefreshLayoutState()
            val scope = rememberCoroutineScope()
            val lazyListState = rememberLazyListState()
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                Box(
                    modifier = Modifier
                        .navigationBarsPadding()
                        .statusBarsPadding()
                ) {
                    RefreshLayout(
                        state = state,
                        contentScrollState = lazyListState,
                        onRefresh = {
                            scope.launch {
                                refresh(state)
                            }
                        },
                        onLoadMore = {
                            scope.launch {
                                loadMore(state)
                            }
                        },
                        enableLoadMore = true,
                        enableRefresh = true,
                        header = {
                            DefaultRefreshHeader(state = it)
                        },
                        footer = {
                            DefaultRefreshFooter(state = it)
                        },
                    ) {
                        LazyColumn(state = lazyListState, modifier = Modifier.fillMaxSize()) {
                            items(data, key = { it }) {
                                Text(
                                    text = "text $it",
                                    modifier = Modifier.padding(vertical = 12.dp)
                                )
                            }
                        }
                    }

                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Button(onClick = {
                            scope.launch {
                                state.refresh()
                            }
                        }, modifier = Modifier) {
                            Text(text = "Refresh")
                        }
                        Button(onClick = {
                            scope.launch {
                                state.loadMore()
                            }
                        }, modifier = Modifier) {
                            Text(text = "LoadMore")
                        }
                    }

                }
            }
        }
    }

    private suspend fun loadMore(
        state: RefreshLayoutState
    ) {
        delay(200)
        val hasMore = cursor < 100
        if (hasMore) {
            val newCur = cursor + 10
            data = data + (cursor until newCur).map { it }
            cursor = newCur
        }
        state.finishLoadMore(success = true, hasMoreData = cursor < 100)
    }

    private suspend fun refresh(
        state: RefreshLayoutState
    ) {
        delay(2000)
        cursor = 10
        data = (0 until cursor).map { it }
        state.finishRefresh(success = true, hasMoreData = true)
    }
}