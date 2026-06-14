package com.abis.direnc.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.abis.direnc.ui.theme.Spacing

@Composable
fun SettingsScreen(viewModel: SettingsViewModel, onBack: () -> Unit) {
    val texts by viewModel.texts.collectAsStateWithLifecycle()
    var draft by remember { mutableStateOf("") }
    var editingId by remember { mutableStateOf<Long?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(Spacing.xl),
    ) {
        Text("Ayarlar — Metinlerin", style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(Spacing.lg))

        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it },
                modifier = Modifier.weight(1f),
                label = { Text("Yeni motive/caydırıcı metin") },
            )
            Spacer(Modifier.height(Spacing.sm))
            Button(
                onClick = { viewModel.add(draft); draft = "" },
                enabled = draft.isNotBlank(),
            ) { Text("Ekle") }
        }

        Spacer(Modifier.height(Spacing.lg))
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            items(texts, key = { it.id }) { item ->
                if (editingId == item.id) {
                    var edited by remember(item.id) { mutableStateOf(item.text) }
                    Column(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = edited,
                            onValueChange = { edited = it },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                            TextButton(onClick = { editingId = null }) { Text("İptal") }
                            TextButton(
                                onClick = { viewModel.edit(item, edited); editingId = null },
                                enabled = edited.isNotBlank(),
                            ) { Text("Kaydet") }
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(item.text, modifier = Modifier.weight(1f))
                        TextButton(onClick = { editingId = item.id }) { Text("Düzenle") }
                        TextButton(onClick = { viewModel.delete(item) }) { Text("Sil") }
                    }
                }
            }
        }

        TextButton(onClick = onBack) { Text("Geri") }
    }
}
