package com.wardclick.app.ui.screens.profile

import com.wardclick.app.ui.theme.LocalBottomBarPadding
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.wardclick.app.BuildConfig
import com.wardclick.app.ui.components.LoadingBox
import com.wardclick.app.ui.viewmodels.ProfileViewModel

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    onLogout: () -> Unit,
    onOpenAdmin: () -> Unit
) {
    val profile by viewModel.userProfile.collectAsState()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.loadProfile() }

    val p = profile
    if (p == null) {
        LoadingBox()
        return
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(bottom = LocalBottomBarPadding.current).padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Spacer(Modifier.height(24.dp))
        Box(
            modifier = Modifier.size(88.dp).background(MaterialTheme.colorScheme.primary, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                (p.fullName.ifBlank { p.email }.firstOrNull()?.uppercase() ?: "?"),
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onPrimary
            )
        }
        Text(
            p.fullName.ifBlank { "Без имени" },
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(p.email, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Роль", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    if (p.isAdmin) "Администратор" else "Гость (пользователь)",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }

        // Скрытый вход в панель администратора (как в Telegram): 10 нажатий на версию приложения.
        // Обычному пользователю нажатия ничего не дают, поэтому раздел ему не виден.
        val context = LocalContext.current
        var taps by remember { mutableIntStateOf(0) }
        // одна подсказка за раз: новая заменяет предыдущую, иначе они встают в очередь
        val hint = remember { arrayOfNulls<Toast>(1) }
        Card(
            modifier = Modifier.fillMaxWidth().clickable {
                if (!p.isAdmin) return@clickable
                taps++
                val left = ADMIN_TAPS - taps
                when {
                    left <= 0 -> {
                        taps = 0
                        hint[0]?.cancel()
                        onOpenAdmin()
                    }
                    left <= 4 -> {
                        hint[0]?.cancel()
                        hint[0] = Toast.makeText(context, "Ещё $left — и откроется панель администратора", Toast.LENGTH_SHORT)
                            .also { it.show() }
                    }
                }
            }
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("О приложении", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Версия приложения ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.titleMedium)
            }
        }

        Spacer(Modifier.weight(1f))
        Button(
            onClick = {
                viewModel.logout()
                onLogout()
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        ) {
            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Выйти из аккаунта")
        }
    }
}

/** Сколько нажатий на версию приложения открывают панель администратора. */
private const val ADMIN_TAPS = 10
