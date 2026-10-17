package com.wardclick.app.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wardclick.app.data.model.LoginRequest
import com.wardclick.app.data.model.RegisterRequest
import com.wardclick.app.data.repository.ApiResult
import com.wardclick.app.data.repository.AuthRepository
import com.wardclick.app.util.Validators
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    object Success : AuthState()
    data class Error(val message: String) : AuthState()
}

class AuthViewModel(private val repository: AuthRepository) : ViewModel() {
    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    fun login(email: String, password: String) {
        val mail = email.trim()
        if (!Validators.isValidEmail(mail)) {
            _authState.value = AuthState.Error("Неверный формат email")
            return
        }
        if (!Validators.isValidPassword(password)) {
            _authState.value = AuthState.Error("Пароль должен быть не короче 6 символов")
            return
        }
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            _authState.value = when (val result = repository.login(LoginRequest(mail, password))) {
                is ApiResult.Success -> AuthState.Success
                is ApiResult.Error -> AuthState.Error(result.message)
                is ApiResult.Exception -> AuthState.Error(result.message)
            }
        }
    }

    fun register(fullName: String, email: String, password: String, phone: String) {
        val mail = email.trim()
        if (fullName.isBlank()) {
            _authState.value = AuthState.Error("Введите имя")
            return
        }
        if (!Validators.isValidEmail(mail)) {
            _authState.value = AuthState.Error("Неверный формат email")
            return
        }
        if (phone.isNotBlank() && !Validators.isValidPhone(phone.trim())) {
            _authState.value = AuthState.Error("Неверный формат телефона")
            return
        }
        if (!Validators.isValidPassword(password)) {
            _authState.value = AuthState.Error("Пароль должен быть не короче 6 символов")
            return
        }
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            // порядок аргументов: имя, email, пароль, телефон
            val request = RegisterRequest(fullName.trim(), mail, password, phone.trim())
            _authState.value = when (val result = repository.register(request)) {
                is ApiResult.Success -> AuthState.Success
                is ApiResult.Error -> AuthState.Error(result.message)
                is ApiResult.Exception -> AuthState.Error(result.message)
            }
        }
    }

    fun resetState() {
        _authState.value = AuthState.Idle
    }
}
