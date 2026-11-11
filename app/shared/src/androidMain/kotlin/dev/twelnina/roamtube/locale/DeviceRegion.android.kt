package dev.twelnina.roamtube.locale

import java.util.Locale

internal actual fun getDeviceRegion(): String =
    getAndroidDeviceRegion(Locale.getDefault())


internal fun getAndroidDeviceRegion(locale: Locale): String {
    val country = locale.country.uppercase(Locale.ROOT)

    return country.takeIf {
        it in Locale.getISOCountries()
    } ?: "US"
}