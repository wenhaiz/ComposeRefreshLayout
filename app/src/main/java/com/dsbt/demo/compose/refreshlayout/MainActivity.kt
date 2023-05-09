package com.dsbt.demo.compose.refreshlayout

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dsbt.demo.compose.refreshlayout.ui.theme.RefreshLayoutComposeTheme
import com.dsbt.lib.composerefreshlayout.DefaultRefreshFooter
import com.dsbt.lib.composerefreshlayout.DefaultRefreshHeader
import com.dsbt.lib.composerefreshlayout.RefreshLayout
import com.dsbt.lib.composerefreshlayout.RefreshLayoutState
import com.dsbt.lib.composerefreshlayout.rememberRefreshLayoutState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private var cursor = 10
    private var data by mutableStateOf((0 until cursor).map { it })

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RefreshLayoutComposeTheme {
                // A surface container using the 'background' color from the theme
                val state = rememberRefreshLayoutState()
                val scope = rememberCoroutineScope()
                val lazyListState = rememberLazyListState()
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Box {
                        RefreshLayout(
                            state = state,
                            contentScrollState = lazyListState,
                            onRefresh = {
                                refresh(scope, state)
                            },
                            onLoadMore = {
                                loadMore(scope, state)
                            },
                            enableLoadMore = true,
                            enableRefresh = true,
                            header = {
                                DefaultRefreshHeader(state = it)
                            },
                            footer = {
                                DefaultRefreshFooter(state = it)
                            }
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
    }

    private fun loadMore(
        scope: CoroutineScope,
        state: RefreshLayoutState
    ) {
        scope.launch {
            delay(1000)
            if (cursor < 100) {
                val newCur = cursor + 10
                data = data + (cursor until newCur).map { it }
                cursor = newCur
            }
            state.finishLoadMore(success = true, hasMoreData = cursor < 100)
        }
    }

    private fun refresh(
        scope: CoroutineScope,
        state: RefreshLayoutState
    ) {
        scope.launch {
            delay(1000)
            cursor = 10
            data = (0 until cursor).map { it }
            state.finishRefresh(success = true)
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    RefreshLayoutComposeTheme {
        Greeting("Android")
    }
}