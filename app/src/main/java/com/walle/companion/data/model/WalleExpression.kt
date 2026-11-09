package com.walle.companion.data.model

enum class WalleExpression {
    IDLE,
    HAPPY,
    CURIOUS,
    SURPRISED,
    SLEEPY,
    LISTENING,
    THINKING;

    companion object {
        fun fromString(value: String): WalleExpression {
            return entries.firstOrNull { it.name.equals(value.trim(), ignoreCase = true) } ?: IDLE
        }
    }
}
