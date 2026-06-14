package com.abis.direnc.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.abis.direnc.ui.theme.Spacing

/**
 * İzinleri tek tek açıklayıp ilgili ayar ekranını açan onboarding. Durum (granted)
 * dışarıdan verilir; kullanıcı ayara gidip dönünce MainActivity yeniden hesaplar.
 */
@Composable
fun OnboardingScreen(
    overlayGranted: Boolean,
    accessibilityGranted: Boolean,
    batteryGranted: Boolean,
    onOpenOverlay: () -> Unit,
    onOpenAccessibility: () -> Unit,
    onOpenBattery: () -> Unit,
    onAllReady: () -> Unit,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(Spacing.xl),
    ) {
        Text("Gözcü kurulumu", style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(Spacing.md))
        Text(
            "Gözcünün çalışması için üç izin gerekiyor. Hepsi cihazında kalır; " +
                "hiçbir veri toplanmaz veya internete gönderilmez.",
            style = MaterialTheme.typography.bodyLarge,
        )
        Spacer(Modifier.height(Spacing.xl))

        PermissionRow(
            title = "Diğer uygulamaların üstünde göster",
            desc = "Ritüel ekranını her şeyin önüne getirebilmek için.",
            granted = overlayGranted,
            onOpen = onOpenOverlay,
        )
        PermissionRow(
            title = "Erişilebilirlik servisi",
            desc = "Kaçmayı engellemek ve 'Vazgeçtim'de ekranı kilitlemek için.",
            granted = accessibilityGranted,
            onOpen = onOpenAccessibility,
        )
        PermissionRow(
            title = "Pil kısıtlamasından muafiyet",
            desc = "Telefonun gözcüyü arka planda öldürmemesi için.",
            granted = batteryGranted,
            onOpen = onOpenBattery,
        )

        Spacer(Modifier.height(Spacing.xl))
        Button(
            onClick = onAllReady,
            enabled = overlayGranted && accessibilityGranted && batteryGranted,
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Hazır — devam et") }
        TextButton(onClick = onBack) { Text("Geri") }
    }
}

@Composable
private fun PermissionRow(
    title: String,
    desc: String,
    granted: Boolean,
    onOpen: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.sm)) {
        Text(
            (if (granted) "✓ " else "• ") + title,
            style = MaterialTheme.typography.bodyLarge,
        )
        Text(desc, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(Spacing.sm))
        OutlinedButton(onClick = onOpen, enabled = !granted) {
            Text(if (granted) "Verildi" else "Ayarı aç")
        }
        Spacer(Modifier.height(Spacing.sm))
    }
}
