package ir.taghvimkari.app.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ir.taghvimkari.app.BuildConfig
import ir.taghvimkari.app.domain.toPersianDigits

@Composable
fun UpdateDialog(state: UpdateUi, onDismiss: () -> Unit) {
    val context = LocalContext.current
    when (state) {
        is UpdateUi.Available -> AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("نسخهٔ جدید ${state.info.version.toPersianDigits()}", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("نسخهٔ فعلی شما: ${BuildConfig.VERSION_NAME.toPersianDigits()}")
                    if (state.info.notes.isNotBlank()) Text(state.info.notes.take(400))
                    Text("پس از دانلود، فایل APK را باز کنید تا روی همین نسخه نصب شود. اطلاعات ثبت‌شده حفظ می‌شود.")
                }
            },
            confirmButton = {
                Button(onClick = {
                    openUrl(context, state.info.apkUrl?.takeIf { it.isNotBlank() } ?: state.info.pageUrl)
                    onDismiss()
                }) { Text("دانلود") }
            },
            dismissButton = { TextButton(onClick = onDismiss) { Text("بعداً") } }
        )
        UpdateUi.UpToDate -> MessageDialog("بروزرسانی", "برنامه به‌روز است.", onDismiss)
        UpdateUi.Failed -> MessageDialog("بروزرسانی", "بررسی انجام نشد. اتصال اینترنت را بررسی کنید و دوباره تلاش کنید.", onDismiss)
        UpdateUi.Checking, UpdateUi.Idle -> Unit
    }
}

@Composable
private fun MessageDialog(title: String, message: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = { Text(message) },
        confirmButton = { TextButton(onClick = onDismiss) { Text("باشه") } }
    )
}

private fun openUrl(context: Context, url: String) {
    if (url.isBlank()) return
    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
}
