package com.n3network.falloutterminalhacker.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.n3network.falloutterminalhacker.solver.SolverState
import com.n3network.falloutterminalhacker.solver.SolverStatus
import com.n3network.falloutterminalhacker.ui.theme.PipBoyDarkGreen
import com.n3network.falloutterminalhacker.ui.theme.PipBoyDialogGreen
import com.n3network.falloutterminalhacker.ui.theme.PipBoyHighlightGreen

@Composable
fun SolverScreen(
    initialCandidates: List<String>,
    onRestart: () -> Unit
) {
    var state by rememberSaveable(stateSaver = SolverStateSaver) {
        mutableStateOf(SolverState.start(initialCandidates))
    }
    // selected/likenessInput intentionally use remember (not rememberSaveable):
    // the LaunchedEffect below resets them whenever state changes, so they're
    // effectively derived from state.
    var selected by remember { mutableStateOf(state.recommendation) }
    var likenessInput by remember { mutableStateOf("") }
    var editingWord by remember { mutableStateOf<String?>(null) }
    var addingWord by remember { mutableStateOf(false) }

    // Whenever state changes, default selection to the new recommendation.
    LaunchedEffect(state) {
        selected = state.recommendation
        likenessInput = ""
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding()
            .padding(16.dp)
    ) {
        Header(state)
        Spacer(Modifier.height(12.dp))

        if (state.status == SolverStatus.IN_PROGRESS) {
            RecommendationCard(state.recommendation, state.wordLength, state.remaining.size)
            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "CANDIDATES (tap to pick)",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                TextButton(onClick = { addingWord = true }) {
                    Text(
                        "+ ADD",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 80.dp, max = 280.dp)
            ) {
                items(state.remaining) { word ->
                    WordRow(
                        word = word,
                        selected = word == selected,
                        onClick = { selected = word },
                        onEdit = { editingWord = word },
                        onRemove = { state = state.removeCandidate(word) }
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            LikenessInputRow(
                value = likenessInput,
                wordLength = state.wordLength,
                onChange = { likenessInput = it }
            )
            Spacer(Modifier.height(12.dp))

            val parsedLikeness = likenessInput.toIntOrNull()
            val canSubmit = selected != null &&
                parsedLikeness != null && parsedLikeness in 0..state.wordLength

            TerminalButton(
                text = "SUBMIT GUESS",
                onClick = {
                    val word = selected ?: return@TerminalButton
                    val k = parsedLikeness ?: return@TerminalButton
                    state = state.submitGuess(word, k)
                },
                enabled = canSubmit,
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            ResultBlock(state)
            Spacer(Modifier.height(12.dp))
            TerminalButton(
                text = "NEW TERMINAL",
                onClick = onRestart,
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (state.history.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            Text(
                "HISTORY",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            state.history.forEach { g ->
                Text(
                    "> ${g.word}  [${g.likeness}/${state.wordLength}]",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }

    // Edit existing word
    editingWord?.let { old ->
        WordEntryDialog(
            title = "EDIT WORD",
            initial = old,
            wordLength = state.wordLength,
            onDismiss = { editingWord = null },
            onConfirm = { new ->
                state = state.editCandidate(old, new)
                editingWord = null
            }
        )
    }

    // Add new word OCR missed
    if (addingWord) {
        WordEntryDialog(
            title = "ADD WORD",
            initial = "",
            wordLength = state.wordLength,
            onDismiss = { addingWord = false },
            onConfirm = { new ->
                state = state.addCandidate(new)
                addingWord = false
            }
        )
    }
}

@Composable
private fun Header(state: SolverState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            "ROBCO TERMLINK",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            "ATTEMPTS: ${state.attemptsLeft}/${SolverState.MAX_ATTEMPTS}",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun RecommendationCard(word: String?, wordLength: Int, remainingCount: Int) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = PipBoyDarkGreen,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
        shape = RoundedCornerShape(4.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                "RECOMMENDED ($remainingCount left, length $wordLength)",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelMedium
            )
            Text(
                word ?: "—",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.headlineMedium
            )
        }
    }
}

@Composable
private fun WordRow(
    word: String,
    selected: Boolean,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onRemove: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        color = if (selected) PipBoyHighlightGreen else Color.Transparent,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "${if (selected) ">" else " "} $word",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onEdit) {
                Text(
                    "EDIT",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelMedium
                )
            }
            TextButton(onClick = onRemove) {
                Text(
                    "DEL",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}

@Composable
private fun LikenessInputRow(
    value: String,
    wordLength: Int,
    onChange: (String) -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            "LIKENESS:",
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.labelLarge
        )
        Spacer(Modifier.width(8.dp))
        OutlinedTextField(
            value = value,
            onValueChange = { new ->
                if (new.length <= 2 && new.all { it.isDigit() }) onChange(new)
            },
            modifier = Modifier.width(80.dp),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            colors = terminalFieldColors()
        )
        Spacer(Modifier.width(8.dp))
        Text(
            "/ $wordLength",
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
private fun ResultBlock(state: SolverState) {
    val green = MaterialTheme.colorScheme.primary
    Column {
        when (state.status) {
            SolverStatus.SOLVED -> {
                Text(
                    "ACCESS GRANTED",
                    color = green,
                    style = MaterialTheme.typography.headlineMedium
                )
                Text(
                    "Password: ${state.history.last().word}",
                    color = green,
                    style = MaterialTheme.typography.titleMedium
                )
            }
            SolverStatus.FAILED -> {
                Text(
                    "LOCKOUT",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.headlineMedium
                )
                if (state.remaining.isNotEmpty()) {
                    Text(
                        "Possible: ${state.remaining.joinToString(", ")}",
                        color = green,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            else -> Unit
        }
    }
}

@Composable
private fun WordEntryDialog(
    title: String,
    initial: String,
    wordLength: Int,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var text by remember { mutableStateOf(initial) }
    val isValid = text.length == wordLength && text.all { it.isLetter() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(title, color = MaterialTheme.colorScheme.primary)
        },
        text = {
            Column {
                OutlinedTextField(
                    value = text,
                    onValueChange = { new ->
                        val filtered = new.uppercase().filter { it.isLetter() }
                        if (filtered.length <= wordLength) text = filtered
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        capitalization = KeyboardCapitalization.Characters
                    ),
                    label = {
                        Text(
                            "$wordLength letters",
                            color = MaterialTheme.colorScheme.primary
                        )
                    },
                    colors = terminalFieldColors()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (isValid) onConfirm(text) },
                enabled = isValid
            ) {
                Text(
                    "OK",
                    color = if (isValid) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outline
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", color = MaterialTheme.colorScheme.primary)
            }
        },
        containerColor = PipBoyDialogGreen,
        textContentColor = MaterialTheme.colorScheme.primary,
        titleContentColor = MaterialTheme.colorScheme.primary
    )
}
