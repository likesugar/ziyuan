@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
package com.ziyuan.immersive

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val lp = window.attributes
        lp.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        window.attributes = lp
        setContent { App() }
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

// ---------------- 主题 ----------------
data class ThemeColors(
    val bg: Brush,
    val card: Color,
    val text: Color,
    val sub: Color,
    val isDark: Boolean
)

@Composable
fun appTheme(prefs: android.content.SharedPreferences): ThemeColors {
    val light = prefs.getBoolean("theme_light", false)
    return if (light) ThemeColors(
        bg = Brush.verticalGradient(listOf(Color(0xFFCFE4FF), Color(0xFFBFD9FF))),
        card = Color.White.copy(alpha = 0.95f),
        text = Color(0xFF16202B),
        sub = Color(0xFF5B6B7C),
        isDark = false
    ) else ThemeColors(
        bg = Brush.verticalGradient(listOf(Color(0xFF000000), Color(0xFF0A0E14), Color(0xFF000000))),
        card = Color.White.copy(alpha = 0.08f),
        text = Color.White,
        sub = Color.White.copy(alpha = 0.55f),
        isDark = true
    )
}

// ---------------- 入口 ----------------
@Composable
fun App() {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("ziyuan", android.content.Context.MODE_PRIVATE)
    val tc = appTheme(prefs)
    val pagerState = rememberPagerState(pageCount = { 2 })
    val scope = rememberCoroutineScope()
    var showPlayerDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(tc.bg)
    ) {
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
            if (page == 0) HomePage(tc) else SettingsPage(tc)
        }
        CapsuleBar(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 22.dp),
            tc = tc, pagerState = pagerState, scope = scope,
            prefs = prefs,
            onPlay = { showPlayerDialog = true }
        )
    }
    if (showPlayerDialog) {
        PlayerDialog(onDismiss = { showPlayerDialog = false })
    }
}

// ---------------- 播放入口弹窗：本地文件 / 网络流 ----------------
@Composable
fun PlayerDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    var url by remember { mutableStateOf("") }

    fun play(u: String, title: String) {
        if (u.isBlank()) return
        onDismiss()
        context.startActivity(
            Intent(context, NativePlayerActivity::class.java)
                .putExtra("url", u)
                .putExtra("title", title)
        )
    }

    // 本地文件选择（SAF），拷到缓存后播放
    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            android.widget.Toast.makeText(context, "载入本地文件…", android.widget.Toast.LENGTH_SHORT).show()
            Thread {
                try {
                    val dst = java.io.File(context.cacheDir, "local_" + (uri.lastPathSegment?.takeLast(40) ?: "video.mp4"))
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        dst.outputStream().use { input.copyTo(it) }
                    }
                    play(dst.absolutePath, dst.name)
                } catch (e: Exception) {
                    android.widget.Toast.makeText(context, "读取失败：${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                }
            }.start()
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(24.dp), color = Color(0xFF1A2230)) {
            Column(Modifier.padding(20.dp)) {
                Text("播放器", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(14.dp))
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    placeholder = { Text("输入网络流地址 http(s)://… 或 m3u8", color = Color.White.copy(alpha = 0.4f), fontSize = 13.sp) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF60A5FA),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = Color(0xFF60A5FA)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFF34A853),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { filePicker.launch("*/*") }
                    ) {
                        Text("本地文件", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center, modifier = Modifier.padding(vertical = 12.dp))
                    }
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFF60A5FA),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { play(url.trim(), "网络流") }
                    ) {
                        Text("播放网络流", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center, modifier = Modifier.padding(vertical = 12.dp))
                    }
                }
            }
        }
    }
}

// ---------------- 首页 ----------------
@Composable
fun HomePage(tc: ThemeColors) {
    Column(modifier = Modifier.fillMaxSize()) {
        Spacer(modifier = Modifier.height(34.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            InfoCard("实时状态", "系统运行中", "全部组件正常加载", Color(0xFF4ADE80), tc, Modifier.weight(1f))
            InfoCard("媒体中心", "双内核就绪", "MediaPlayer / IjkPlayer", Color(0xFF60A5FA), tc, Modifier.weight(1f))
        }
        Spacer(modifier = Modifier.height(14.dp))
        InfoCard(
            "沉浸模式", "全屏无边框", "状态栏与导航栏已隐藏，挖孔区完全覆盖",
            Color(0xFFF9A8D4), tc,
            Modifier.fillMaxWidth().padding(horizontal = 16.dp)
        )
    }
}

@Composable
fun InfoCard(title: String, headline: String, sub: String, accent: Color, tc: ThemeColors, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, shape = RoundedCornerShape(24.dp), color = tc.card) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(10.dp).background(accent, CircleShape))
                Spacer(Modifier.width(8.dp))
                Text(title, color = tc.sub, fontSize = 13.sp)
            }
            Spacer(Modifier.height(14.dp))
            Text(headline, color = tc.text, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text(sub, color = tc.sub, fontSize = 13.sp)
        }
    }
}

// ---------------- 设置页 ----------------
@Composable
fun SettingsPage(tc: ThemeColors) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("ziyuan", android.content.Context.MODE_PRIVATE)
    val autoPlay = remember { mutableStateOf(prefs.getBoolean("auto_play", true)) }
    val clipDetect = remember { mutableStateOf(prefs.getBoolean("clip_detect", true)) }
    val hideHomeCap = remember { mutableStateOf(prefs.getBoolean("capsule_hide_home", false)) }
    val hidePlayCap = remember { mutableStateOf(prefs.getBoolean("capsule_hide_play", false)) }
    val hideSetCap = remember { mutableStateOf(prefs.getBoolean("capsule_hide_settings", false)) }
    val themeLight = remember { mutableStateOf(prefs.getBoolean("theme_light", false)) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(44.dp))
        Text("设置", color = tc.text, fontWeight = FontWeight.Bold, fontSize = 26.sp,
            modifier = Modifier.padding(horizontal = 24.dp))
        Spacer(modifier = Modifier.height(12.dp))

        SectionTitle("主题与播放", tc)
        CardBlock(tc) {
            SettingSwitch("纯浅蓝主题", "纯黑 ↔ 纯浅蓝 一键切换", themeLight.value, Color(0xFF60A5FA), tc) {
                themeLight.value = it
                prefs.edit().putBoolean("theme_light", it).apply()
            }
            SettingSwitch("自动播放", "进入播放页立即开始", autoPlay.value, Color(0xFFF9A8D4), tc) {
                autoPlay.value = it
                prefs.edit().putBoolean("auto_play", it).apply()
            }
        }

        SectionTitle("平台账号", tc)
        CardBlock(tc) {
            PlatformRow("哔哩哔哩", "已登录", "管理", Color(0xFFF8A8C0), tc) { toast(context, "哔哩哔哩：管理（未接入）") }
            Divider(tc)
            PlatformRow("抖音", "未登录", "登录", Color(0xFF1D1D24), tc) { toast(context, "抖音：登录（未接入）") }
            Divider(tc)
            PlatformRow("YouTube", "未登录", "登录", Color(0xFFE53935), tc) { toast(context, "YouTube：登录（未接入）") }
            Divider(tc)
            PlatformRow("小红书", "未登录", "登录", Color(0xFFE64A3B), tc) { toast(context, "小红书：登录（未接入）") }
            Divider(tc)
            PlatformRow("添加平台", "支持任意网站的网页登录", "添加", Color(0xFF90A4AE), tc) { toast(context, "添加平台（未接入）") }
            Spacer(Modifier.height(8.dp))
            Text(
                "登录后使用账号可访问的画质与资源。会员、地区和平台验证限制仍以网站为准。\n登录信息只保存在本机，不会读取或保存密码；登录过期请回到这里重新登录。",
                color = tc.sub, fontSize = 12.sp, lineHeight = 18.sp,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
            Spacer(Modifier.height(8.dp))
            Text("清除全部网页登录", color = Color(0xFF2563EB), fontSize = 14.sp, fontWeight = FontWeight.Medium,
                modifier = Modifier.clickable { toast(context, "已清除（演示）") }.padding(vertical = 6.dp))
        }

        SectionTitle("配置文件上传与下载", tc)
        CardBlock(tc) {
            ActionRow("上传配置到 GitHub", "AI配置+收藏+下载历史 备份到私密 gist", "上传", Color(0xFF34A853), tc) { toast(context, "上传（未接入）") }
            Divider(tc)
            ActionRow("从 GitHub 下载配置", "恢复 AI配置+收藏+下载历史", "下载", Color(0xFF1E88E5), tc) { toast(context, "下载（未接入）") }
        }

        SectionTitle("胶囊", tc)
        CardBlock(tc) {
            SettingSwitch("隐藏首页胶囊", "隐藏后底部导航不显示首页入口", hideHomeCap.value, Color(0xFF60A5FA), tc) { hideHomeCap.value = it }
            SettingSwitch("隐藏播放器胶囊", "隐藏后底部导航不显示播放器入口", hidePlayCap.value, Color(0xFFF9A8D4), tc) { hidePlayCap.value = it }
            SettingSwitch("隐藏设置胶囊", "隐藏后底部导航不显示设置入口", hideSetCap.value, Color(0xFFFDE68A), tc) { hideSetCap.value = it }
            Spacer(Modifier.height(10.dp))
            Surface(
                modifier = Modifier.fillMaxWidth().clickable {
                    prefs.edit()
                        .putBoolean("capsule_hide_home", hideHomeCap.value)
                        .putBoolean("capsule_hide_play", hidePlayCap.value)
                        .putBoolean("capsule_hide_settings", hideSetCap.value)
                        .apply()
                    toast(context, "已生效，切到首页查看")
                },
                shape = RoundedCornerShape(20.dp),
                color = if (tc.isDark) Color.White.copy(alpha = 0.10f) else Color(0xFFCFE4FF)
            ) {
                Text("确认生效", color = tc.text, fontSize = 15.sp, fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp))
            }
        }

        SectionTitle("链接与提醒", tc)
        CardBlock(tc) {
            SettingSwitch("自动检测剪贴板", "打开应用或回到前台时，识别视频并提示播放。", clipDetect.value, Color(0xFF1E88E5), tc) {
                clipDetect.value = it
                prefs.edit().putBoolean("clip_detect", it).apply()
            }
        }

        SectionTitle("关于", tc)
        CardBlock(tc) {
            Text("ziyuan Immersive · vc13", color = tc.sub, fontSize = 13.sp)
        }
        Spacer(modifier = Modifier.height(140.dp))
    }
}

fun toast(context: android.content.Context, msg: String) {
    android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
}

@Composable
fun SectionTitle(title: String, tc: ThemeColors) {
    Text(title, color = tc.sub, fontSize = 13.sp,
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp))
}

@Composable
fun CardBlock(tc: ThemeColors, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(24.dp),
        color = tc.card
    ) {
        Column(Modifier.padding(16.dp), content = content)
    }
}

@Composable
fun Divider(tc: ThemeColors) {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .height(1.dp)
            .background(if (tc.isDark) Color.White.copy(alpha = 0.06f) else Color(0xFFE3EEFF))
    )
}

@Composable
fun PlatformRow(name: String, status: String, action: String, avatarBg: Color, tc: ThemeColors, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(46.dp).background(avatarBg, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(name.take(1), color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(name, color = tc.text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Text(status, color = tc.sub, fontSize = 13.sp)
        }
        Text(action, color = Color(0xFF2563EB), fontSize = 15.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun ActionRow(title: String, sub: String, action: String, iconBg: Color, tc: ThemeColors, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(46.dp).background(iconBg, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(if (action == "上传") "↑" else "↓", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = tc.text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Text(sub, color = tc.sub, fontSize = 13.sp)
        }
        Text(action, color = Color(0xFF2563EB), fontSize = 15.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun SettingSwitch(title: String, sub: String, checked: Boolean, accent: Color, tc: ThemeColors, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(10.dp).background(accent, CircleShape))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = tc.text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Text(sub, color = tc.sub, fontSize = 13.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = accent,
                uncheckedThumbColor = Color(0xFFB0BEC5),
                uncheckedTrackColor = Color(0xFF78909C).copy(alpha = 0.5f)
            )
        )
    }
}

// ---------------- 胶囊 ----------------
@Composable
fun CapsuleBar(
    modifier: Modifier = Modifier,
    tc: ThemeColors,
    pagerState: androidx.compose.foundation.pager.PagerState,
    scope: kotlinx.coroutines.CoroutineScope,
    prefs: android.content.SharedPreferences,
    onPlay: () -> Unit
) {
    val hideHome = prefs.getBoolean("capsule_hide_home", false)
    val hidePlay = prefs.getBoolean("capsule_hide_play", false)
    val hideSet = prefs.getBoolean("capsule_hide_settings", false)
    val current = pagerState.currentPage
    val bubbleBg = if (tc.isDark) Color.Transparent else Color(0xFFEFF6FF)

    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = if (tc.isDark) Color.White.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.95f),
        contentColor = tc.text
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!hideHome) CapsuleBubble(
                icon = Icons.Filled.Home,
                selected = current == 0,
                tint = Color(0xFF6EE7B7),
                bubble = bubbleBg
            ) { scope.launch { pagerState.animateScrollToPage(0) } }
            if (!hideSet) CapsuleBubble(
                icon = Icons.Filled.Settings,
                selected = current == 1,
                tint = Color(0xFF60A5FA),
                bubble = bubbleBg
            ) { scope.launch { pagerState.animateScrollToPage(1) } }
            if (!hidePlay) CapsuleBubble(
                icon = Icons.Filled.PlayArrow,
                selected = false,
                tint = Color(0xFFF9A8D4),
                bubble = bubbleBg,
                onClick = onPlay
            )
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
        Icon(icon, contentDescription = null, tint = if (selected) tint else if (bubble == Color(0xFFEFF6FF)) Color(0xFF37474F) else Color.White.copy(alpha = 0.75f))
    }
}
