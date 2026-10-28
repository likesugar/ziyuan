@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
package com.ziyuan.immersive

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import kotlinx.coroutines.launch

// ---------------- 实时全局状态 ----------------
object AppState {
    var lightTheme by mutableStateOf(false)          // false=纯黑 true=纯浅蓝
    var hidePaipan by mutableStateOf(false)
    var hideHome by mutableStateOf(false)
    var hideMedia by mutableStateOf(false)
    var hidePlayer by mutableStateOf(false)
    var autoPlay by mutableStateOf(true)
    var clipboardDetect by mutableStateOf(false)
    fun load(prefs: android.content.SharedPreferences) {
        lightTheme = prefs.getBoolean("light_theme", false)
        hidePaipan = prefs.getBoolean("hide_paipan", false)
        hideHome = prefs.getBoolean("hide_home", false)
        hideMedia = prefs.getBoolean("hide_media", false)
        hidePlayer = prefs.getBoolean("hide_player", false)
        autoPlay = prefs.getBoolean("auto_play", true)
        clipboardDetect = prefs.getBoolean("clipboard_detect", false)
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val lp = window.attributes
        lp.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        window.attributes = lp
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        AppState.load(getSharedPreferences("ziyuan", MODE_PRIVATE))
        setContent { App() }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            WindowCompat.setDecorFitsSystemWindows(window, false)
            val c = WindowInsetsControllerCompat(window, window.decorView)
            c.hide(WindowInsetsCompat.Type.systemBars())
            c.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }
}

// ---------------- 主题色 ----------------
data class Tc(val isDark: Boolean, val bg: Brush, val card: Color, val text: Color, val sub: Color, val capsule: Color, val bubble: Color)

@Composable
fun themeColors(): Tc = if (!AppState.lightTheme) Tc(
    true,
    Brush.verticalGradient(listOf(Color(0xFF0B0F14), Color(0xFF141B26), Color(0xFF0B0F14))),
    Color.White.copy(alpha = 0.07f), Color.White, Color.White.copy(alpha = 0.6f),
    Color.White.copy(alpha = 0.10f), Color.Transparent
) else Tc(
    false,
    Brush.verticalGradient(listOf(Color(0xFFCFE4FF), Color(0xFFBBD8FF), Color(0xFFCFE4FF))),
    Color.White, Color(0xFF17233A), Color(0xFF5A6B85),
    Color.White, Color(0xFFEAF3FF)
)

// ---------------- 主框架：Pager + 胶囊 ----------------
@Composable
fun App() {
    val tc = themeColors()
    val prefs = androidx.compose.ui.platform.LocalContext.current
        .getSharedPreferences("ziyuan", android.content.Context.MODE_PRIVATE)
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current
    val pagerState = rememberPagerState(pageCount = { 2 })
    var showPlayerDialog by remember { mutableStateOf(false) }

    // 本地文件选择
    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) context.startActivity(
            Intent(context, NativePlayerActivity::class.java).putExtra("url", uri.toString()).putExtra("title", "本地播放")
        )
    }

    Box(modifier = Modifier.fillMaxSize().background(tc.bg)) {
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
            if (page == 0) HomePage(tc) else SettingsPage(tc, prefs)
        }
        CapsuleBar(
            tc = tc,
            current = pagerState.currentPage,
            onHome = { scope.launch { pagerState.animateScrollToPage(0) } },
            onSettings = { scope.launch { pagerState.animateScrollToPage(1) } },
            onPlay = { showPlayerDialog = true },
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 18.dp)
        )
        if (showPlayerDialog) PlayerDialog(
            tc = tc,
            onDismiss = { showPlayerDialog = false },
            onPickFile = { showPlayerDialog = false; filePicker.launch("video/*") }
        )
    }
}

// ---------------- 首页：功能卡网格 ----------------
data class FeatCard(val emoji: String, val color: Color, val title: String, val sub: String)

@Composable
fun HomePage(tc: Tc) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val cards = listOf(
        FeatCard("➤", Color(0xFF2E9BF0), "资源下载", "下载管理器"),
        FeatCard("🕷", Color(0xFF12A5A0), "资源嗅探", "Spider 引擎"),
        FeatCard("🌐", Color(0xFF4F5BD5), "资源嗅探", "嗅探网页媒体资源"),
        FeatCard("🎬", Color(0xFFE8336E), "视频下载", "在线视频解析"),
        FeatCard("📕", Color(0xFFE63946), "小红书", "图文视频提取"),
        FeatCard("⚡", Color(0xFF4353C4), "创建快捷键", "任意快捷方式"),
        FeatCard("💬", Color(0xFFB04FD8), "聊天室", "WebChat 聊天"),
        FeatCard("🥁", Color(0xFF14A08A), "节拍器&天气", "节拍+天气卡片")
    )
    Column(modifier = Modifier.fillMaxSize()) {
        Spacer(modifier = Modifier.height(40.dp))
        Text(
            "助手工具箱",
            color = tc.text, fontWeight = FontWeight.Bold, fontSize = 26.sp,
            modifier = Modifier.padding(horizontal = 20.dp)
        )
        Spacer(modifier = Modifier.height(6.dp))
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 8.dp)
        ) {
            items(cards) { c ->
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = tc.card,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (c.title == "节拍器&天气" || c.title.startsWith("资源嗅探"))
                                android.widget.Toast.makeText(context, "待接入", android.widget.Toast.LENGTH_SHORT).show()
                            else
                                android.widget.Toast.makeText(context, "待接入", android.widget.Toast.LENGTH_SHORT).show()
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.size(46.dp).background(c.color, CircleShape),
                            contentAlignment = Alignment.Center
                        ) { Text(c.emoji, fontSize = 22.sp) }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(c.title, color = tc.text, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text(c.sub, color = tc.sub, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(90.dp))
    }
}

// ---------------- 设置页 ----------------
@Composable
fun SettingsPage(tc: Tc, prefs: android.content.SharedPreferences) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(44.dp))
        Text("设置", color = tc.text, fontWeight = FontWeight.Bold, fontSize = 26.sp, modifier = Modifier.padding(horizontal = 24.dp))
        Spacer(modifier = Modifier.height(10.dp))
        // 主题切换（实时）
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ThemeCard("纯黑", !AppState.lightTheme, tc, Modifier.weight(1f)) {
                AppState.lightTheme = false; prefs.edit().putBoolean("light_theme", false).apply()
            }
            ThemeCard("纯浅蓝", AppState.lightTheme, tc, Modifier.weight(1f)) {
                AppState.lightTheme = true; prefs.edit().putBoolean("light_theme", true).apply()
            }
        }
        SectionTitle("自动播放", tc)
        SwitchRow("自动播放", "进入播放页立即开始", AppState.autoPlay, Color(0xFFF9A8D4), tc) {
            AppState.autoPlay = it; prefs.edit().putBoolean("auto_play", it).apply()
        }
        SectionTitle("胶囊", tc)
        SwitchRow("隐藏排盘胶囊", "", AppState.hidePaipan, Color(0xFFFDE68A), tc) {
            AppState.hidePaipan = it; prefs.edit().putBoolean("hide_paipan", it).apply()
        }
        SwitchRow("隐藏首页胶囊", "", AppState.hideHome, Color(0xFF86EFAC), tc) {
            AppState.hideHome = it; prefs.edit().putBoolean("hide_home", it).apply()
        }
        SwitchRow("隐藏媒体胶囊", "", AppState.hideMedia, Color(0xFF93C5FD), tc) {
            AppState.hideMedia = it; prefs.edit().putBoolean("hide_media", it).apply()
        }
        SwitchRow("隐藏播放器胶囊", "", AppState.hidePlayer, Color(0xFFF9A8D4), tc) {
            AppState.hidePlayer = it; prefs.edit().putBoolean("hide_player", it).apply()
        }
        SectionTitle("链接与提醒", tc)
        SwitchRow("自动检测剪贴板", "自动识别链接并嗅探", AppState.clipboardDetect, Color(0xFF60A5FA), tc) {
            AppState.clipboardDetect = it; prefs.edit().putBoolean("clipboard_detect", it).apply()
        }
        SectionTitle("关于", tc)
        InfoItem("版本", "ziyuan Immersive · vc15", tc)
        InfoItem("播放器内核", "MediaPlayer / IjkPlayer 双内核", tc)
        Spacer(modifier = Modifier.height(120.dp))
    }
}

@Composable
fun SectionTitle(t: String, tc: Tc) {
    Text(t, color = tc.sub, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp))
}

@Composable
fun ThemeCard(name: String, selected: Boolean, tc: Tc, modifier: Modifier, onClick: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    Surface(
        shape = RoundedCornerShape(20.dp), color = tc.card,
        modifier = modifier.padding(horizontal = 0.dp).clickable { onClick() }
    ) {
        Column(Modifier.padding(14.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth().height(44.dp)
                    .background(
                        if (name == "纯黑") Brush.verticalGradient(listOf(Color(0xFF0B0F14), Color(0xFF141B26)))
                        else Brush.verticalGradient(listOf(Color(0xFFCFE4FF), Color(0xFFBBD8FF))),
                        RoundedCornerShape(12.dp)
                    )
            )
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(name, color = tc.text, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Spacer(Modifier.width(8.dp))
                Text(if (selected) "使用中" else "点击切换", color = tc.sub, fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun SwitchRow(title: String, sub: String, checked: Boolean, accent: Color, tc: Tc, onChange: (Boolean) -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 5.dp),
        shape = RoundedCornerShape(22.dp), color = tc.card
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, color = tc.text, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                if (sub.isNotEmpty()) Text(sub, color = tc.sub, fontSize = 12.sp)
            }
            Switch(checked = checked, onCheckedChange = onChange,
                colors = SwitchDefaults.colors(checkedTrackColor = accent))
        }
    }
}

@Composable
fun InfoItem(title: String, sub: String, tc: Tc) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 5.dp),
        shape = RoundedCornerShape(22.dp), color = tc.card
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text(title, color = tc.text, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(sub, color = tc.sub, fontSize = 12.sp)
            }
        }
    }
}

// ---------------- 播放选择弹窗 ----------------
@Composable
fun PlayerDialog(tc: Tc, onDismiss: () -> Unit, onPickFile: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var url by remember { mutableStateOf("") }
    Box(
        Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.55f)).clickable { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(26.dp), color = tc.card,
            modifier = Modifier.padding(28.dp).fillMaxWidth().clickable(enabled = false) { }
        ) {
            Column(Modifier.padding(20.dp)) {
                Text("选择播放源", color = tc.text, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(Modifier.height(12.dp))
                androidx.compose.material3.OutlinedTextField(
                    value = url, onValueChange = { url = it },
                    placeholder = { Text("输入网络流地址 m3u8 / mp4 / flv", color = tc.sub, fontSize = 13.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = androidx.compose.ui.text.TextStyle(color = tc.text, fontSize = 14.sp)
                )
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(
                        shape = RoundedCornerShape(16.dp), color = Color(0xFF2E9BF0),
                        modifier = Modifier.weight(1f).clickable {
                            if (url.isNotBlank()) {
                                onDismiss()
                                context.startActivity(
                                    Intent(context, NativePlayerActivity::class.java)
                                        .putExtra("url", url.trim()).putExtra("title", "网络流")
                                )
                            }
                        }
                    ) {
                        Text("播放网络流", color = Color.White, fontWeight = FontWeight.Bold,
                            fontSize = 14.sp, modifier = Modifier.padding(vertical = 12.dp).fillMaxWidth(),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    }
                    Surface(
                        shape = RoundedCornerShape(16.dp), color = Color(0xFF14A08A),
                        modifier = Modifier.weight(1f).clickable { onPickFile() }
                    ) {
                        Text("本地文件", color = Color.White, fontWeight = FontWeight.Bold,
                            fontSize = 14.sp, modifier = Modifier.padding(vertical = 12.dp).fillMaxWidth(),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    }
                }
            }
        }
    }
}

// ---------------- 胶囊栏（实时隐藏） ----------------
@Composable
fun CapsuleBar(tc: Tc, current: Int, onHome: () -> Unit, onSettings: () -> Unit, onPlay: () -> Unit, modifier: Modifier = Modifier) {
    if (AppState.hideHome && AppState.hideMedia && AppState.hidePaipan && AppState.hidePlayer) return
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = tc.capsule,
        contentColor = tc.text
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!AppState.hideHome) CapsuleBubble(
                icon = Icons.Filled.Home, selected = current == 0,
                tint = Color(0xFF4ADE80), bubble = tc.bubble
            ) { onHome() }
            if (!AppState.hideMedia) CapsuleBubble(
                icon = Icons.Filled.Star, selected = false,
                tint = Color(0xFF93C5FD), bubble = tc.bubble
            ) { onPlay() }
            if (!AppState.hidePaipan) CapsuleBubble(
                icon = Icons.Filled.Settings, selected = current == 1,
                tint = Color(0xFF60A5FA), bubble = tc.bubble
            ) { onSettings() }
            if (!AppState.hidePlayer) CapsuleBubble(
                icon = Icons.Filled.PlayArrow, selected = false,
                tint = Color(0xFFF9A8D4), bubble = tc.bubble
            ) { onPlay() }
        }
    }
}

@Composable
fun CapsuleBubble(icon: androidx.compose.ui.graphics.vector.ImageVector, selected: Boolean, tint: Color, bubble: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(46.dp)
            .background(if (selected) tint.copy(alpha = 0.25f) else bubble, CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null,
            tint = if (selected) tint else if (tc_light()) Color(0xFF37474F) else Color.White.copy(alpha = 0.75f))
    }
}

@Composable
fun tc_light(): Boolean = AppState.lightTheme
