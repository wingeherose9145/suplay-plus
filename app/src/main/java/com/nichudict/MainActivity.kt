package com.nichudict

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            NichuDictApp()
        }
    }
}

@Composable
fun NichuDictApp() {
    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            DictScreen()
        }
    }
}

@Composable
fun DictScreen() {
    var searchText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Text(
            text = "日中离线词典",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        OutlinedTextField(
            value = searchText,
            onValueChange = { searchText = it },
            label = { Text("输入日语单词") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            singleLine = true
        )

        Button(
            onClick = { /* 后续实现搜索 */ },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("搜索")
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "目前还是基础框架\n词典搜索功能正在开发中...",
            style = MaterialTheme.typography.bodyLarge
        )
    }
}
