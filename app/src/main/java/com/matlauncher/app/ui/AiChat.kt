package com.matlauncher.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.matlauncher.app.ai.AiHost
import com.matlauncher.app.ai.MatAi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private class Msg(val fromUser: Boolean, val text: String)

@Composable
fun AiChat(host: AiHost, initialQuery: String?, onClose: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val messages = remember {
        mutableStateListOf(
            Msg(false, "Hi, I'm Mat AI. I can chain commands, like \"flashlight on, silent mode, open YouTube\". Say \"help\" to see everything.")
        )
    }
    var input by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    fun send(text: String) {
        val t = text.trim()
        if (t.isEmpty()) return
        messages.add(Msg(true, t))
        input = ""
        scope.launch {
            delay(350)
            val steps = MatAi.plan(t, host)
            for (step in steps) {
                val run = step.run
                if (run == null) {
                    if (step.text.isNotEmpty()) messages.add(Msg(false, step.text))
                } else if (step.launch) {
                    // say it first: opening an app leaves the launcher
                    messages.add(Msg(false, step.text))
                    listState.animateScrollToItem(messages.lastIndex)
                    delay(250)
                    run()
                } else {
                    val result = try { run() } catch (e: Exception) { "That didn't work." }
                    val shown = result.ifEmpty { step.text }
                    if (shown.isNotEmpty()) messages.add(Msg(false, shown))
                }
                listState.animateScrollToItem(messages.lastIndex)
                if (steps.size > 1) delay(250)
            }
        }
    }

    LaunchedEffect(initialQuery) {
        if (!initialQuery.isNullOrBlank()) send(initialQuery)
    }

    Surface(Modifier.fillMaxSize(), color = scheme.surface.copy(alpha = 0.97f)) {
        Column(Modifier.fillMaxSize().statusBarsPadding().imePadding().navigationBarsPadding()) {
            Row(
                Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 8.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.AutoAwesome, null, tint = scheme.primary, modifier = Modifier.size(26.dp))
                Spacer(Modifier.size(10.dp))
                Text("Mat AI", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                IconButton(onClick = onClose) { Icon(Icons.Rounded.Close, "Close") }
            }

            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(messages) { m ->
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = if (m.fromUser) Arrangement.End else Arrangement.Start,
                    ) {
                        Surface(
                            shape = RoundedCornerShape(
                                topStart = 22.dp, topEnd = 22.dp,
                                bottomStart = if (m.fromUser) 22.dp else 6.dp,
                                bottomEnd = if (m.fromUser) 6.dp else 22.dp,
                            ),
                            color = if (m.fromUser) scheme.primary else scheme.surfaceContainerHigh,
                            contentColor = if (m.fromUser) scheme.onPrimary else scheme.onSurface,
                            modifier = Modifier.widthIn(max = 300.dp),
                        ) {
                            Text(m.text, modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp))
                        }
                    }
                }
            }

            LazyRow(
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(MatAi.suggestions) { s ->
                    SuggestionChip(onClick = { send(s) }, label = { Text(s) })
                }
            }

            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Ask Mat AI…") },
                    singleLine = true,
                    shape = RoundedCornerShape(28.dp),
                )
                FilledIconButton(onClick = { send(input) }) {
                    Icon(Icons.AutoMirrored.Rounded.Send, "Send")
                }
            }
        }
    }
}
