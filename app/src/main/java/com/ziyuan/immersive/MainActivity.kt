package com.ziyuan.immersive

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 沉浸式：内容延伸到系统栏后面，并隐藏状态栏 + 导航栏
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        // 挖孔屏：内容延伸到挖孔区
        val lp = window.attributes
        lp.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        window.attributes = lp
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        setContent { ImmersiveScreen() }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemBars()
    }

    override fun onResume() {
        super.onResume()
        hideSystemBars()
    }

    private fun hideSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }
}

@Composable
fun ImmersiveScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF0B0F14), Color(0xFF141B26), Color(0xFF0B0F14))
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {
            Spacer(modifier = Modifier.height(34.dp)) // 顶到挖孔区，手动留出顶部安全距离
            // 左右卡片
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                InfoCard(
                    title = "左侧卡片",
                    body = "这是左边的卡片内容，展示沉浸式布局。",
                    accent = Color(0xFF4ADE80),
                    modifier = Modifier.weight(1f)
                )
                InfoCard(
                    title = "右侧卡片",
                    body = "这是右边的卡片内容，与左侧等宽对称。",
                    accent = Color(0xFF60A5FA),
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            // 胶囊低栏
            CapsuleBar(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(bottom = 20.dp)
            )
        }
    }
}

@Composable
fun InfoCard(title: String, body: String, accent: Color, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = Color.White.copy(alpha = 0.06f),
        contentColor = Color.White
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(accent, CircleShape)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(title, fontWeight = FontWeight.Bold, fontSize = 17.sp)
            Spacer(modifier = Modifier.height(6.dp))
            Text(body, fontSize = 13.sp, color = Color.White.copy(alpha = 0.7f), lineHeight = 19.sp)
        }
    }
}

@Composable
fun CapsuleBar(modifier: Modifier = Modifier) {
    val context = androidx.compose.ui.platform.LocalContext.current
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(percent = 50),
        color = Color.White.copy(alpha = 0.08f),
        contentColor = Color.White
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 22.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(26.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CapsuleIcon(Icons.Filled.Home)
            CapsuleIcon(Icons.Filled.Settings)
            CapsuleIcon(Icons.Filled.PlayArrow, highlight = true) {
                context.startActivity(
                    android.content.Intent(context, NativePlayerActivity::class.java)
                        .putExtra("url", "https://demo.unified-streaming.com/k8s/features/stable/video/tears-of-steel/tears-of-steel.ism/.m3u8")
                        .putExtra("title", "胶囊播放")
                )
            }
        }
    }
}

@Composable
fun CapsuleIcon(icon: ImageVector, highlight: Boolean = false, onClick: () -> Unit = {}) {
    val tint = if (highlight) Color(0xFF4ADE80) else Color.White.copy(alpha = 0.85f)
    val context = androidx.compose.ui.platform.LocalContext.current
    Box(
        modifier = Modifier
            .size(40.dp)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = tint)
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewImmersive() {
    ImmersiveScreen()
}
