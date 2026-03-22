package com.example.driver_app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.driver_app.data.model.SoberBooking
import com.example.driver_app.data.model.VehicleRental
import com.example.driver_app.data.repo.DriverJobsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class DriverJobsViewModel : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    private val _pendingSober = MutableStateFlow<List<SoberBooking>>(emptyList())
    val pendingSober: StateFlow<List<SoberBooking>> = _pendingSober

    private val _assignedSober = MutableStateFlow<List<SoberBooking>>(emptyList())
    val assignedSober: StateFlow<List<SoberBooking>> = _assignedSober

    private val _pendingRentals = MutableStateFlow<List<VehicleRental>>(emptyList())
    val pendingRentals: StateFlow<List<VehicleRental>> = _pendingRentals

    private val _assignedRentals = MutableStateFlow<List<VehicleRental>>(emptyList())
    val assignedRentals: StateFlow<List<VehicleRental>> = _assignedRentals

    fun loadAll() {
        viewModelScope.launch {
            refreshJobs()
        }
    }

    fun login(email: String, pass: String, onDone: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            if (email.isBlank() || pass.isBlank()) {
                onDone(false, "Vui long nhap day du email va mat khau")
                return@launch
            }

            val res = DriverJobsRepository.login(email, pass)
            _errorMessage.value = if (res.ok) null else (res.message ?: "Dang nhap that bai")
            onDone(res.ok, _errorMessage.value)
        }
    }

    fun acceptSober(id: Long, onDone: (Boolean) -> Unit = {}) {
        runAction({ DriverJobsRepository.acceptSober(id) }, onDone)
    }

    fun arriveSober(id: Long, onDone: (Boolean) -> Unit = {}) {
        runAction({ DriverJobsRepository.arriveSober(id) }, onDone)
    }

    fun startSober(id: Long, onDone: (Boolean) -> Unit = {}) {
        runAction({ DriverJobsRepository.startSober(id) }, onDone)
    }

    fun completeSober(id: Long, onDone: (Boolean) -> Unit = {}) {
        runAction({ DriverJobsRepository.completeSober(id) }, onDone)
    }

    fun acceptRental(id: Long, onDone: (Boolean) -> Unit = {}) {
        runAction({ DriverJobsRepository.acceptRental(id) }, onDone)
    }

    fun startRental(id: Long, onDone: (Boolean) -> Unit = {}) {
        runAction({ DriverJobsRepository.startRental(id) }, onDone)
    }

    fun completeRental(id: Long, onDone: (Boolean) -> Unit = {}) {
        runAction({ DriverJobsRepository.completeRental(id) }, onDone)
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun logout() {
        _pendingSober.value = emptyList()
        _assignedSober.value = emptyList()
        _pendingRentals.value = emptyList()
        _assignedRentals.value = emptyList()
        _errorMessage.value = null
        _isLoading.value = false
    }

    private fun runAction(action: suspend () -> Result<Unit>, onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            _errorMessage.value = null
            val result = action()
            val ok = result.isSuccess

            if (!ok) {
                _errorMessage.value = result.exceptionOrNull()?.message ?: "Khong the thuc hien thao tac"
            }

            refreshJobs()
            onDone(ok)
        }
    }

    private suspend fun refreshJobs() {
        _isLoading.value = true
        _errorMessage.value = null

        val pendingSoberResult = DriverJobsRepository.pendingSober()
        val assignedSoberResult = DriverJobsRepository.assignedSober()
        val pendingRentalsResult = DriverJobsRepository.pendingAllRentals()
        val assignedRentalsResult = DriverJobsRepository.assignedRentals()

        _pendingSober.value = pendingSoberResult.getOrDefault(emptyList())
        _assignedSober.value = assignedSoberResult.getOrDefault(emptyList())
        _pendingRentals.value = pendingRentalsResult.getOrDefault(emptyList())
        _assignedRentals.value = assignedRentalsResult.getOrDefault(emptyList())

        _errorMessage.value = listOf(
            pendingSoberResult,
            assignedSoberResult,
            pendingRentalsResult,
            assignedRentalsResult
        ).firstNotNullOfOrNull { it.exceptionOrNull()?.message }

        _isLoading.value = false
    }
}
