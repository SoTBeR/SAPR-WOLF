package ru.sotber.sapr_wolf

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform