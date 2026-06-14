package com.abis.direnc.ui.ritual

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.text.KeyboardOptions
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.abis.direnc.ui.theme.Spacing

/**
 * Ritüel akışı. result PASSED/GAVE_UP olunca ilgili callback tetiklenir.
 *
 * onFinishStudy null değilse, sol üst köşede sade bir "Çalışmam bitti" butonu gösterilir
 * (yalnızca gözcü overlay'inde kullanılır). Sol üst, tek elle kaçamak bakarken zor
 * ulaşılan bir nokta olduğundan bilinçli sürtünmedir: yanlışlıkla değil, gerçekten
 * bitirince basılır → oturumu kapatır.
 */
@Composable
fun RitualScreen(
    viewModel: RitualViewModel,
    onPassed: () -> Unit,
    onGaveUp: () -> Unit,
    onFinishStudy: (() -> Unit)? = null,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.result) {
        when (state.result) {
            RitualResult.PASSED -> onPassed()
            RitualResult.GAVE_UP -> onGaveUp()
            null -> {}
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(Spacing.xl),
        contentAlignment = Alignment.Center,
    ) {
        if (onFinishStudy != null) {
            TextButton(
                onClick = onFinishStudy,
                modifier = Modifier.align(Alignment.TopStart),
            ) {
                Text(
                    "Çalışmam bitti",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            when (state.stepIndex) {
                0 -> MotivationStep(state.motivationText, viewModel::onMotivationContinue)
                1 -> ConfrontStep(viewModel::onConfrontSubmit)
                else -> MathStep(
                    question = state.problem.question,
                    error = state.mathError,
                    onSubmit = viewModel::onMathSubmit,
                )
            }
            Spacer(Modifier.height(Spacing.xxl))
            TextButton(onClick = viewModel::onGiveUp) {
                Text("Vazgeçtim, çalışmaya dönüyorum")
            }
        }
    }
}

@Composable
private fun MotivationStep(text: String, onContinue: () -> Unit) {
    Text(
        text = text,
        style = MaterialTheme.typography.headlineSmall,
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(Spacing.xl))
    Button(onClick = onContinue) { Text("Devam") }
}

@Composable
private fun ConfrontStep(onSubmit: (String) -> Unit) {
    var answer by remember { mutableStateOf("") }
    Text(
        text = "Telefonu şu an niçin açmak istiyorsun?",
        style = MaterialTheme.typography.headlineSmall,
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(Spacing.lg))
    OutlinedTextField(
        value = answer,
        onValueChange = { answer = it },
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(Spacing.lg))
    Button(onClick = { onSubmit(answer) }, enabled = answer.isNotBlank()) {
        Text("Devam")
    }
}

@Composable
private fun MathStep(question: String, error: Boolean, onSubmit: (String) -> Unit) {
    var answer by remember { mutableStateOf("") }
    Text(text = question, style = MaterialTheme.typography.headlineLarge)
    Spacer(Modifier.height(Spacing.lg))
    OutlinedTextField(
        value = answer,
        onValueChange = { answer = it },
        isError = error,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
    )
    if (error) {
        Spacer(Modifier.height(Spacing.sm))
        Text("Yanlış. Tekrar dene.", color = MaterialTheme.colorScheme.error)
    }
    Spacer(Modifier.height(Spacing.lg))
    Button(onClick = { onSubmit(answer) }, enabled = answer.isNotBlank()) {
        Text("Onayla")
    }
}
