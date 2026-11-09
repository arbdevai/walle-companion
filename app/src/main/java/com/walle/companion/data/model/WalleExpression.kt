package com.walle.companion.data.model

enum class WalleExpression {
    IDLE,
    LOOK_LEFT,
    LOOK_RIGHT,
    LOOK_UP,
    HAPPY,
    LAUGHING,
    CURIOUS,
    SURPRISED,
    SLEEPY,
    WINK,
    DIZZY,
    ANGRY,
    LOVE,
    LISTENING,
    THINKING;

    companion object {
        fun fromString(value: String): WalleExpression {
            val sanitized = value.trim().uppercase()
            return entries.firstOrNull { it.name == sanitized } ?: IDLE
        }
    }
}
