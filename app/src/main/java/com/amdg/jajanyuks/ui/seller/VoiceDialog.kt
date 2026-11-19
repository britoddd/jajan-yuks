package com.amdg.jajanyuks.ui.seller

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.amdg.jajanyuks.R

// Step 1: the seller speaks (or types) what changed. Step 2: they see what Gemini understood and confirm.
@Composable
fun VoiceDialog(
    state: VoiceUiState,
    onParse: (String) -> Unit,
    onVoiceUnavailable: () -> Unit,
    onSave: () -> Unit,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
) {
    var text by rememberSaveable { mutableStateOf("") }
    // Opens the phone's own speech screen (Google's recognizer) in Indonesian and returns the text.
    // ponytail: system recognizer UI instead of a custom SpeechRecognizer screen; no RECORD_AUDIO permission needed.
    val speech = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val heard = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
        if (result.resultCode == Activity.RESULT_OK && heard != null) {
            text = heard
            onParse(heard)
        }
    }
    val speechIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        .putExtra(RecognizerIntent.EXTRA_LANGUAGE, "id-ID")
        .putExtra(RecognizerIntent.EXTRA_PROMPT, stringResource(R.string.voice_hint))
    val proposal = state.proposal

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.voice_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                when {
                    state.thinking -> {
                        CircularProgressIndicator()
                        Text(stringResource(R.string.voice_thinking))
                    }
                    proposal == null -> {
                        OutlinedButton(
                            onClick = {
                                try { speech.launch(speechIntent) } catch (e: ActivityNotFoundException) { onVoiceUnavailable() }
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text(stringResource(R.string.voice_speak)) }
                        OutlinedTextField(
                            value = text,
                            onValueChange = { text = it },
                            label = { Text(stringResource(R.string.voice_input_label)) },
                            placeholder = { Text(stringResource(R.string.voice_hint)) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        state.error?.let { Text(stringResource(it), color = MaterialTheme.colorScheme.error) }
                    }
                    proposal.isEmpty -> Text(stringResource(R.string.voice_nothing))
                    else -> {
                        Text(stringResource(R.string.voice_confirm), style = MaterialTheme.typography.titleSmall)
                        proposal.stock.forEach {
                            Text(stringResource(R.string.voice_stock_change, it.item.name, it.item.stock, it.newStock))
                        }
                        proposal.statusText?.let { Text(stringResource(R.string.voice_status_change, it)) }
                    }
                }
            }
        },
        confirmButton = {
            when {
                state.thinking -> Unit
                proposal == null -> TextButton(onClick = { onParse(text) }, enabled = text.isNotBlank()) {
                    Text(stringResource(R.string.voice_process))
                }
                proposal.isEmpty -> TextButton(onClick = onRetry) { Text(stringResource(R.string.voice_retry)) }
                else -> TextButton(onClick = onSave) { Text(stringResource(R.string.save)) }
            }
        },
        dismissButton = {
            TextButton(onClick = if (proposal != null && !proposal.isEmpty) onRetry else onDismiss) {
                Text(stringResource(if (proposal != null && !proposal.isEmpty) R.string.voice_retry else R.string.cancel))
            }
        },
    )
}
