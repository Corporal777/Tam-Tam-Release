package org.otunjargych.tamtam.di.repo.auth

import android.util.Log
import io.reactivex.Completable
import io.reactivex.Maybe
import org.otunjargych.tamtam.api.ApiService
import org.otunjargych.tamtam.di.data.AppData
import org.otunjargych.tamtam.model.request.LoginRequestBody
import org.otunjargych.tamtam.model.request.LoginResponse
import org.otunjargych.tamtam.model.request.RegisterRequestBody

class AuthRepositoryImpl(val appData: AppData, val apiService: ApiService) : AuthRepository {

    override fun login(body: LoginRequestBody): Maybe<LoginResponse> {
        return apiService.login(body)
    }

    override fun logout(id: String): Completable {
        return apiService.logout(id)
    }

    override fun register(body: Map<String, String>): Maybe<LoginResponse> {
        return apiService.register(body)
    }

    override fun checkUserPassword(password: String): Completable {
        return apiService.checkUserPassword(appData.getUserId(), password)
    }

    override fun updateUserPassword(password: Map<String, String>): Completable {
       return apiService.updateUserPassword(appData.getUserId(), password)
    }

    override fun updateUserLogin(login: Map<String, String>): Completable {
        return apiService.updateUserLogin(appData.getUserId(), login)
    }

    override fun checkIfLoginExists(login: String): Completable {
        return apiService.checkLoginExists(login)
    }
}