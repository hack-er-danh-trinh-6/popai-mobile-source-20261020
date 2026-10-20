package com.health.pop.core.network

sealed class ApiResult<out T> {
    data class Success<out T>(val data: T, val isDemoData: Boolean = false) : ApiResult<T>()
    data class Error(val message: String, val code: Int? = null) : ApiResult<Nothing>()
    object ServerUnconfigured : ApiResult<Nothing>()
    object NetworkError : ApiResult<Nothing>()
}
