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
        if (!Validators.isValidEmail(email)) {
            _authState.value = AuthState.Error("Неверный формат email")
            return
        }
        if (!Validators.isValidPassword(password)) {
            _authState.value = AuthState.Error("Пароль должен быть не менее 6 символов")
            return
        }

        _authState.value = AuthState.Loading
        viewModelScope.launch {
            val request = LoginRequest(email, password)
            when (val result = repository.login(request)) {
                is ApiResult.Success -> {
                    _authState.value = AuthState.Success
                }
                is ApiResult.Error -> {
                    _authState.value = AuthState.Error(result.message)
                }
                is ApiResult.Exception -> {
                    _authState.value = AuthState.Error(result.message)
                }
            }
        }
    }

    fun register(email: String, password: String, fullName: String) {
        if (!Validators.isValidEmail(email)) {
            _authState.value = AuthState.Error("Неверный формат email")
            return
        }
        if (!Validators.isValidPassword(password)) {
            _authState.value = AuthState.Error("Пароль должен быть не менее 6 символов")
            return
        }
        if (fullName.isBlank()) {
            _authState.value = AuthState.Error("Имя не может быть пустым")
            return
        }

        _authState.value = AuthState.Loading
        viewModelScope.launch {
            val request = RegisterRequest(email, password, fullName, "USER")
            when (val result = repository.register(request)) {
                is ApiResult.Success -> {
                    _authState.value = AuthState.Success
                }
                is ApiResult.Error -> {
                    _authState.value = AuthState.Error(result.message)
                }
                is ApiResult.Exception -> {
                    _authState.value = AuthState.Error(result.message)
                }
            }
        }
    }
    
    fun resetState() {
        _authState.value = AuthState.Idle
    }
}
