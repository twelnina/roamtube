package dev.twelnina.roamtube

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform