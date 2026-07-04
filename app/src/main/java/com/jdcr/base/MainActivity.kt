package com.jdcr.base

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.jdcr.base.ui.theme.JdcrDevelopBaseTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.lifecycle.lifecycleScope
import com.jdcr.jdcrbase.device.JdcrDeviceInfo
import com.jdcr.jdcrbase.device.JdcrDeviceUtils
import com.jdcr.jdcrbase.device.JdcrShakeDetector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


class MainActivity : ComponentActivity() {

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycleScope.launch(Dispatchers.Default) {
            delay(5000)
        }
        val context = this
        Log.d("jdcr_", JdcrDeviceInfo().toString())
        Log.d("jdcr_", JdcrDeviceUtils.getSystemName() ?: "")
        JdcrDeviceUtils.getRAMInfo()
        JdcrDeviceUtils.getStorageInfo()
        JdcrShakeDetector {

        }.start()

        setContent {
            JdcrDevelopBaseTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Greeting(
                        name = "Android",
                        modifier = Modifier.padding(innerPadding)
                    )
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
    JdcrDevelopBaseTheme {
        Greeting("Android")
    }
}