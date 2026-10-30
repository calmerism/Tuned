package com.tuned.app.data.migration

import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class TamedAccountReaderTest {

    private val cookie = stringPreferencesKey("innerTubeCookie")
    private val name = stringPreferencesKey("accountName")
    private val email = stringPreferencesKey("accountEmail")
    private val handle = stringPreferencesKey("accountChannelHandle")

    @Test
    fun readsASignedInAccount() {
        val account = TamedAccountReader.from(
            mutablePreferencesOf(cookie to "SID=a; SAPISID=b; HSID=c", name to "Sam", handle to "@sam"),
        )
        assertThat(account).isEqualTo(TamedAccount("SID=a; SAPISID=b; HSID=c", "Sam", "@sam"))
    }

    @Test
    fun fallsBackToTheEmailWhenThereIsNoName() {
        val account = TamedAccountReader.from(mutablePreferencesOf(cookie to "SAPISID=b", email to "sam@example.com"))
        assertThat(account?.name).isEqualTo("sam@example.com")
        assertThat(account?.channelHandle).isNull()
    }

    @Test
    fun aCookieWithoutSapisidIsSignedOut() {
        // Tamed keeps a visitor cookie when signed out; it can't sign requests.
        assertThat(TamedAccountReader.from(mutablePreferencesOf(cookie to "VISITOR_INFO1_LIVE=x"))).isNull()
        assertThat(TamedAccountReader.from(mutablePreferencesOf())).isNull()
    }
}
