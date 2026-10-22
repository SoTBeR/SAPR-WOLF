package ru.sotber.sapr_wolf.model

enum class ValidationSeverity {
    ERROR,
    WARNING,
}

data class ValidationIssue(
    val code: String,
    val severity: ValidationSeverity,
    val message: String,
    val rodIndex: Int? = null,
    val nodeIndex: Int? = null,
) {
    val isError: Boolean get() = severity == ValidationSeverity.ERROR
    val isWarning: Boolean get() = severity == ValidationSeverity.WARNING
}
