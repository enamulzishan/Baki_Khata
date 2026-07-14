package com.example.profile

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class OwnerProfileUiState(
    val isLoading: Boolean = true,
    val profile: OwnerProfile? = null,
    val isSaving: Boolean = false,
    val error: String? = null,
    val saveSuccess: Boolean = false
)

class OwnerProfileViewModel : ViewModel() {
    private val repository = OwnerProfileRepository()
    private val _uiState = MutableStateFlow(OwnerProfileUiState())
    val uiState: StateFlow<OwnerProfileUiState> = _uiState.asStateFlow()

    init {
        loadProfile()
    }

    fun loadProfile() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val profile = repository.getProfile()
                _uiState.value = _uiState.value.copy(isLoading = false, profile = profile)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.localizedMessage)
            }
        }
    }

    fun saveProfile(profile: OwnerProfile, newImageUri: Uri?) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, error = null, saveSuccess = false)
            try {
                val savedProfile = repository.saveProfile(profile, newImageUri)
                _uiState.value = _uiState.value.copy(isSaving = false, profile = savedProfile, saveSuccess = true)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isSaving = false, error = e.localizedMessage)
            }
        }
    }

    fun resetSuccess() {
        _uiState.value = _uiState.value.copy(saveSuccess = false)
    }
}
