package edu.utap.gaggle

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {

    private val _loginSuccess = MutableLiveData<Boolean>()
    val loginSuccess: LiveData<Boolean> = _loginSuccess

    fun login(email: String, password: String) {
        // TODO: Replace this with real Firebase logic
        viewModelScope.launch {
            _loginSuccess.value = email == "test@example.com" && password == "password123"
        }
    }
}
