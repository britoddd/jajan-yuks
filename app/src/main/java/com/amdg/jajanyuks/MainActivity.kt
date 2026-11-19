package com.amdg.jajanyuks

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.layout.padding
import com.amdg.jajanyuks.ui.theme.JajanYuksTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // setContent replaces XML layouts: the UI is built from @Composable functions.
        setContent {
            JajanYuksTheme {
                Scaffold { padding ->
                    Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                        Text(stringResource(R.string.welcome))
                    }
                }
            }
        }
    }
}
