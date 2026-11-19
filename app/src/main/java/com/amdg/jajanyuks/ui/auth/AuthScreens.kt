package com.amdg.jajanyuks.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.amdg.jajanyuks.R
import com.amdg.jajanyuks.domain.Role

@Composable
fun LoginScreen(
    onLoggedIn: (Role) -> Unit,
    onGoToRegister: () -> Unit,
    viewModel: AuthViewModel = viewModel(factory = AuthViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    // `rememberSaveable` also survives screen rotation, unlike plain `remember`.
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }

    AuthForm(title = stringResource(R.string.login_title), state = state) {
        EmailField(email) { email = it }
        PasswordField(password) { password = it }
        Button(
            onClick = { viewModel.signIn(email, password, onLoggedIn) },
            enabled = !state.loading,
            modifier = Modifier.fillMaxWidth(),
        ) { Text(stringResource(R.string.login_button)) }
        TextButton(onClick = onGoToRegister) { Text(stringResource(R.string.go_to_register)) }
    }
}

@Composable
fun RegisterScreen(
    onRegistered: (Role) -> Unit,
    onGoToLogin: () -> Unit,
    viewModel: AuthViewModel = viewModel(factory = AuthViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var role by rememberSaveable { mutableStateOf(Role.BUYER) }

    AuthForm(title = stringResource(R.string.register_title), state = state) {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text(stringResource(R.string.name_label)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        EmailField(email) { email = it }
        PasswordField(password) { password = it }
        Text(stringResource(R.string.register_as))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = role == Role.SELLER,
                onClick = { role = Role.SELLER },
                label = { Text(stringResource(R.string.role_seller)) },
            )
            FilterChip(
                selected = role == Role.BUYER,
                onClick = { role = Role.BUYER },
                label = { Text(stringResource(R.string.role_buyer)) },
            )
        }
        Button(
            onClick = { viewModel.register(name, email, password, role, onRegistered) },
            enabled = !state.loading,
            modifier = Modifier.fillMaxWidth(),
        ) { Text(stringResource(R.string.register_button)) }
        TextButton(onClick = onGoToLogin) { Text(stringResource(R.string.go_to_login)) }
    }
}

// Shared layout: title, the form fields passed in as `content`, then loading/error.
// `content: @Composable () -> Unit` is a "slot": the caller decides what goes inside.
@Composable
private fun AuthForm(title: String, state: AuthUiState, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineMedium)
        Text(title, style = MaterialTheme.typography.titleMedium)
        content()
        if (state.loading) CircularProgressIndicator()
        state.error?.let { Text(stringResource(it), color = MaterialTheme.colorScheme.error) }
    }
}

@Composable
private fun EmailField(value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(stringResource(R.string.email_label)) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun PasswordField(value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(stringResource(R.string.password_label)) },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        modifier = Modifier.fillMaxWidth(),
    )
}
