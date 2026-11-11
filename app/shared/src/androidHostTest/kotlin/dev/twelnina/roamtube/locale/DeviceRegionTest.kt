package dev.twelnina.roamtube.locale

import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals

class DeviceRegionTest {
    @Test
    fun returnsCountryFromLocale() {
        assertEquals(
            "JP",
            getAndroidDeviceRegion(Locale.JAPAN)
        )
    }

    @Test
    fun fallsBackToUsWhenCountryIsMissing() {
        assertEquals(
            "US",
            getAndroidDeviceRegion(Locale.ROOT)
        )
    }

    @Test
    fun fallsBackToUsWhenCountryIsInvalid() {
        assertEquals(
            "US",
            getAndroidDeviceRegion(Locale.Builder().setRegion("ZZ").build())
        )
    }
}