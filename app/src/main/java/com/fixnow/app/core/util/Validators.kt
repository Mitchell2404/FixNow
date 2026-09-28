package com.fixnow.app.core.util

private val EMAIL_REGEX = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

fun String.isValidEmail(): Boolean = EMAIL_REGEX.matches(this)
