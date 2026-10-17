package com.wardclick.app.ui.viewmodels

import androidx.lifecycle.ViewModel
import com.wardclick.app.data.local.SessionManager
import com.wardclick.app.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class UserProfile(
    val email: String,
    val fullName: String,
    val role: String,
    val isAdmin: Boolean
)

class ProfileViewModel(
    private val authRepository: AuthRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile: StateFlow<UserProfile?> = _userProfile.asStateFlow()

    fun loadProfile() {
        if (sessionManager.isLoggedIn) {
            val email = sessionManager.email ?: ""
            val fullName = sessionManager.fullName ?: ""
            val role = sessionManager.role ?: ""
            val isAdmin = sessionManager.isAdmin
            _userProfile.value = UserProfile(email, fullName, role, isAdmin)
        } else {
            _userProfile.value = null
        }
    }

    fun logout() {
        authRepository.logout()
        _userProfile.value = null
    }
}
