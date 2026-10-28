@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import kotlinx.coroutines.launch

val MintGreen = Color(0xFF5EEAD4)
val SoftBlue = Color(0xFF93C5FD)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val lp = window.attributes
        lp.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        window.attributes = lp
        hideSystemBars()
        setContent { ImmersiveApp() }
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
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }
}

@Composable
fun ImmersiveApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(pageCount = { 2 })

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF0B0F14), Color(0xFF141B26), Color(0xFF0B0F14))
                )
            )
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            when (page) {
                0 -> HomePage(
                    onPlay = {
                        context.startActivity(
                            android.content.Intent(context, NativePlayerActivity::class.java)
                                .putExtra("url", "https://demo.unified-streaming.com/k8s/features/stable/video/tears-of-steel/tears-of-steel.ism/.m3u8")
                                .putExtra("title", "胶囊播放")
                        )
                    }
                )
                1 -> SettingsPage()
            }
        }
        // 悬浮胶囊栏（两页共用，圆润可爱版）
        CapsuleBar(
            current = pagerState.currentPage,
            onHome = { scope.launch { pagerState.animateScrollToPage(0) } },
            onSettings = { scope.launch { pagerState.animateScrollToPage(1) } },
            onPlay = {
                context.startActivity(
                    android.content.Intent(context, NativePlayerActivity::class.java)
                        .putExtra("url", "https://demo.unified-streaming.com/k8s/features/stable/video/tears-of-steel/tears-of-steel.ism/.m3u8")
                        .putExtra("title", "胶囊播放")
                )
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 26.dp)
        )
    }
}

// ---------------- 首页 ----------------
@Composable
fun HomePage(onPlay: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Spacer(modifier = Modifier.height(34.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            InfoCard(
                title = "左侧卡片",
                body = "这是左边的卡片内容，展示沉浸式布局。",
                accent = MintGreen,
                modifier = Modifier.weight(1f)
            )
            InfoCard(
                title = "右侧卡片",
                body = "这是右边的卡片内容，与左侧等宽对称。",
                accent = SoftBlue,
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(modifier = Modifier.height(90.dp))
        Text(
            "左右滑动切换 设置页 ⇄ 首页",
            color = Color.White.copy(alpha = 0.35f),
            fontSize = 12.sp,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
fun InfoCard(title: String, body: String, accent: Color, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        color = Color.White.copy(alpha = 0.06f),
        contentColor = Color.White
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(accent, CircleShape)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(title, fontWeight = FontWeight.Bold, fontSize = 17.sp)
            Spacer(modifier = Modifier.height(6.dp))
            Text(body, fontSize = 13.sp, color = Color.White.copy(alpha = 0.7f), lineHeight = 19.sp)
        }
    }
}

// ---------------- 设置页 ----------------
@Composable
fun SettingsPage() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = context.getSharedPreferences("ziyuan", android.content.Context.MODE_PRIVATE)
    var autoPlay by remember { mutableStateOf(prefs.getBoolean("auto_play", true)) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(44.dp))
        Text(
            "设置",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 26.sp,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        SettingSwitch("自动播放", "进入播放页立即开始", autoPlay, Color(0xFFF9A8D4)) {
            autoPlay = it
            prefs.edit().putBoolean("auto_play", it).apply()
        }
        SettingItem("关于", "ziyuan Immersive · vc11", MintGreen)
        Spacer(modifier = Modifier.height(120.dp))
    }
}

@Composable
fun SettingSwitch(title: String, sub: String, checked: Boolean, accent: Color, onChange: (Boolean) -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(24.dp),
        color = Color.White.copy(alpha = 0.06f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(accent, CircleShape)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                Text(sub, color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
            }
            Switch(
                checked = checked,
                onCheckedChange = onChange,
                colors = SwitchDefaults.colors(
                    checkedTrackColor = accent,
                    uncheckedTrackColor = Color.White.copy(alpha = 0.15f)
                )
            )
        }
    }
}

@Composable
fun SettingItem(title: String, sub: String, accent: Color) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(24.dp),
        color = Color.White.copy(alpha = 0.06f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(accent, CircleShape)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                Text(sub, color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
            }
        }
    }
}

// ---------------- 可爱圆润胶囊栏 ----------------
@Composable
fun CapsuleBar(
    current: Int,
    onHome: () -> Unit,
    onSettings: () -> Unit,
    onPlay: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = Color.White.copy(alpha = 0.10f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CapsuleBubble(
                icon = Icons.Filled.Home,
                selected = current == 0,
                tint = MintGreen,
                onClick = onHome
            )
            CapsuleBubble(
                icon = Icons.Filled.PlayArrow,
                selected = false,
                tint = Color(0xFFF9A8D4),
                onClick = onPlay
            )
            CapsuleBubble(
                icon = Icons.Filled.Settings,
                selected = current == 1,
                tint = SoftBlue,
                onClick = onSettings
            )
        }
    }
}

@Composable
fun CapsuleBubble(icon: ImageVector, selected: Boolean, tint: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(46.dp)
            .background(
                if (selected) tint.copy(alpha = 0.25f) else Color.Transparent,
                CircleShape
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = if (selected) tint else Color.White.copy(alpha = 0.75f))
    }
}
