package com.billing.app.domain.model

import com.billing.app.core.localization.AppStrings

sealed class AppError(override val message: String) : Exception(message) {
    data class FractionalNotAllowed(val unit: String) :
        AppError("Fractional quantity is not allowed for discrete unit: $unit")

    data class UnitChangeNotAllowed(val productName: String, val reason: String) :
        AppError("Cannot change unit for '$productName': $reason")

    data class InvalidUnit(val rawUnit: String) :
        AppError("Unknown or invalid unit: $rawUnit")

    data class AccountLocked(val seconds: Long) :
        AppError("Too many incorrect attempts. Account locked for $seconds seconds.")

    data class IncorrectPin(val attemptsRemaining: Int) :
        AppError("Incorrect PIN. $attemptsRemaining attempt(s) remaining.")

    fun toMessage(strings: AppStrings): String = when (this) {
        is FractionalNotAllowed -> strings.fractionalNotAllowed(unit)
        is UnitChangeNotAllowed -> strings.unitChangeNotAllowed(productName, reason)
        is InvalidUnit -> strings.invalidUnit(rawUnit)
        is AccountLocked -> strings.accountLockedSeconds(seconds)
        is IncorrectPin -> strings.attemptsRemaining(attemptsRemaining)
    }
}
