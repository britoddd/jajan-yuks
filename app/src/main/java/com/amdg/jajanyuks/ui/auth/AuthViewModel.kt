package com.amdg.jajanyuks.ui.auth

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.amdg.jajanyuks.JajanYuksApp
import com.amdg.jajanyuks.R
import com.amdg.jajanyuks.data.UserRepository
import com.amdg.jajanyuks.domain.Role
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthUiState(val loading: Boolean = false, @StringRes val error: Int? = null)

class AuthViewModel(private val users: UserRepository) : ViewModel() {
    private val _state = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = _state

    fun signIn(email: String, password: String, onSuccess: (Role) -> Unit) {
        if (email.isBlank() || password.isBlank()) return fail(R.string.error_fill_all)
        run(onSuccess) {
            users.signIn(email.trim(), password)
            users.currentRole()
        }
    }

    fun register(name: String, email: String, password: String, role: Role, onSuccess: (Role) -> Unit) {
        if (name.isBlank() || email.isBlank() || password.isBlank()) return fail(R.string.error_fill_all)
        run(onSuccess) {
            users.register(name.trim(), email.trim(), password, role)
            role
        }
    }

    // Shared loading/error handling for both actions. `viewModelScope.launch` starts a coroutine
    // that is cancelled automatically if the screen's ViewModel is cleared.
    private fun run(onSuccess: (Role) -> Unit, action: suspend () -> Role?) {
        _state.value = AuthUiState(loading = true)
        viewModelScope.launch {
            val role = try {
                action()
            } catch (e: Exception) {
                return@launch fail(errorFor(e))
            }
            if (role == null) {
                users.signOut()
                return@launch fail(R.string.error_no_profile)
            }
            _state.value = AuthUiState()
            onSuccess(role)
        }
    }

    private fun fail(@StringRes message: Int) = _state.update { AuthUiState(error = message) }

    private fun errorFor(e: Exception): Int = when (e) {
        is FirebaseAuthWeakPasswordException -> R.string.error_weak_password
        is FirebaseAuthUserCollisionException -> R.string.error_email_taken
        is FirebaseAuthInvalidCredentialsException, is FirebaseAuthInvalidUserException -> R.string.error_wrong_login
        else -> R.string.error_network
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { AuthViewModel((this[APPLICATION_KEY] as JajanYuksApp).container.userRepository) }
        }
    }
}
