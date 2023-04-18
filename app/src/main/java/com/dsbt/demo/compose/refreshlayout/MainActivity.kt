package com.dsbt.demo.compose.refreshlayout

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dsbt.demo.compose.refreshlayout.ui.theme.RefreshLayoutComposeTheme
import com.dsbt.lib.composerefreshlayout.RefreshLayout
import com.dsbt.lib.composerefreshlayout.RefreshLayoutState

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RefreshLayoutComposeTheme {
                // A surface container using the 'background' color from the theme
                val state = remember {
                    RefreshLayoutState()
                }
                var cur = remember {
                    10
                }
                var data by remember {
                    mutableStateOf(0)
                }
                val lazyListState = rememberLazyListState()
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    RefreshLayout(state = state, contentScrollState = lazyListState) {
                        LazyColumn(state = lazyListState, modifier = Modifier.fillMaxSize()) {
                            items(data) {
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