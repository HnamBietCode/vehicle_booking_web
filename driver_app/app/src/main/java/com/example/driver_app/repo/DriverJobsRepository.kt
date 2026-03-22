package com.example.driver_app.data.repo

import com.example.driver_app.BuildConfig
import com.example.driver_app.data.api.ApiClient
import com.example.driver_app.data.model.DeviceTokenRequest
import com.example.driver_app.data.model.DriverLocationRequest
import com.example.driver_app.data.model.LoginRequest
import com.example.driver_app.data.model.LoginResponse
import com.example.driver_app.data.model.SoberBooking
import com.example.driver_app.data.model.VehicleRental
import com.example.driver_app.data.store.TokenStore
import java.io.IOException
import retrofit2.HttpException
import retrofit2.Response

object DriverJobsRepository {

    suspend fun login(email: String, pass: String): LoginResponse {
        return try {
            val res = ApiClient.api.login(LoginRequest(email, pass))
            if (res.ok && res.token != null) {
                TokenStore.saveToken(res.token)
            }
            res
        } catch (e: Exception) {
            LoginResponse(false, e.toUserMessage("Dang nhap that bai"), null, null, null)
        }
    }

    suspend fun registerDevice(token: String) {
        runResponseRequest("Khong dang ky duoc thiet bi") {
            ApiClient.api.registerDevice(DeviceTokenRequest(token))
        }
    }

    suspend fun pendingSober(): Result<List<SoberBooking>> = runRequest("Khong tai duoc don tai xe moi") {
        ApiClient.api.pendingSober()
    }

    suspend fun assignedSober(): Result<List<SoberBooking>> = runRequest("Khong tai duoc don tai xe da nhan") {
        ApiClient.api.assignedSober()
    }

    suspend fun pendingAllRentals(): Result<List<VehicleRental>> = runRequest("Khong tai duoc don thue xe moi") {
        ApiClient.api.pendingAllRentals()
    }

    suspend fun assignedRentals(): Result<List<VehicleRental>> = runRequest("Khong tai duoc don thue xe da nhan") {
        ApiClient.api.assignedRentals()
    }

    suspend fun acceptSober(id: Long): Result<Unit> = runResponseRequest("Khong nhan duoc don tai xe") {
        ApiClient.api.acceptSober(id)
    }

    suspend fun arriveSober(id: Long): Result<Unit> = runResponseRequest("Khong cap nhat duoc trang thai da toi noi") {
        ApiClient.api.arriveSober(id)
    }

    suspend fun startSober(id: Long): Result<Unit> = runResponseRequest("Khong bat dau duoc don tai xe") {
        ApiClient.api.startSober(id)
    }

    suspend fun completeSober(id: Long): Result<Unit> = runResponseRequest("Khong hoan thanh duoc don tai xe") {
        ApiClient.api.completeSober(id)
    }

    suspend fun acceptRental(id: Long): Result<Unit> = runResponseRequest("Khong nhan duoc don thue xe") {
        ApiClient.api.acceptRental(id)
    }

    suspend fun startRental(id: Long): Result<Unit> = runResponseRequest("Khong bat dau duoc don thue xe") {
        ApiClient.api.startRental(id)
    }

    suspend fun completeRental(id: Long): Result<Unit> = runResponseRequest("Khong hoan thanh duoc don thue xe") {
        ApiClient.api.completeRental(id)
    }

    suspend fun sendLocation(req: DriverLocationRequest) {
        runResponseRequest("Khong gui duoc vi tri tai xe") {
            ApiClient.api.sendLocation(req)
        }
    }

    private suspend fun <T> runRequest(defaultMessage: String, block: suspend () -> T): Result<T> {
        return try {
            Result.success(block())
        } catch (e: Exception) {
            Result.failure(RuntimeException(e.toUserMessage(defaultMessage), e))
        }
    }

    private suspend fun runResponseRequest(
        defaultMessage: String,
        block: suspend () -> Response<*>
    ): Result<Unit> {
        return try {
            val response = block()
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                val errorBody = response.errorBody()?.string()?.takeIf { it.isNotBlank() }
                Result.failure(RuntimeException(errorBody ?: "$defaultMessage (HTTP ${response.code()})"))
            }
        } catch (e: Exception) {
            Result.failure(RuntimeException(e.toUserMessage(defaultMessage), e))
        }
    }

    private fun Throwable.toUserMessage(defaultMessage: String): String {
        val baseUrl = TokenStore.getBaseUrl() ?: BuildConfig.API_BASE_URL
        return when (this) {
            is HttpException -> when (code()) {
                401 -> "Phien dang nhap da het han hoac khong hop le"
                403 -> "Tai khoan hien khong duoc phep thuc hien thao tac nay"
                404 -> "Khong tim thay du lieu tren backend"
                else -> "$defaultMessage (HTTP ${code()})"
            }

            is IOException -> "Khong ket noi duoc toi backend ($baseUrl). Kiem tra lai URL server va may backend vehicle_booking_web."
            else -> message?.takeIf { it.isNotBlank() } ?: defaultMessage
        }
    }
}
