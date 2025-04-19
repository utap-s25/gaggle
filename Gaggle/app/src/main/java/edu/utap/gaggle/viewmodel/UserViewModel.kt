package edu.utap.gaggle.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import edu.utap.gaggle.model.UserPreferences

class UserViewModel : ViewModel() {
    private val _preferences = MutableLiveData(UserPreferences())
    val preferences: LiveData<UserPreferences> = _preferences

    fun updatePreferences(newPrefs: UserPreferences) {
        _preferences.value = newPrefs
    }
}
