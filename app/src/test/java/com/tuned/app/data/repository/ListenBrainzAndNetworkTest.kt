package com.tuned.app.data.repository

import com.google.common.truth.Truth.assertThat
import com.tuned.app.data.listenbrainz.ListenBrainzClient
import com.tuned.app.data.local.MiscSettings
import com.tuned.app.data.local.forNetwork
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Test

class ListenBrainzAndNetworkTest {
    @Test
    fun aListenCarriesItsTimeAndNames() {
        val body = ListenBrainzClient.listenBody("single", "Justice", "Stress", "Cross", listenedAt = 1_700_000_000)
        val item = body["payload"]!!.jsonArray.single().jsonObject
        assertThat(body["listen_type"]!!.jsonPrimitive.content).isEqualTo("single")
        assertThat(item["listened_at"]!!.jsonPrimitive.content).isEqualTo("1700000000")
        val meta = item["track_metadata"]!!.jsonObject
        assertThat(meta["artist_name"]!!.jsonPrimitive.content).isEqualTo("Justice")
        assertThat(meta["release_name"]!!.jsonPrimitive.content).isEqualTo("Cross")
    }

    @Test
    fun playingNowHasNoTimeAndBlankAlbumIsLeftOut() {
        val item = ListenBrainzClient.listenBody("playing_now", "A", "B", " ", listenedAt = null)["payload"]!!.jsonArray.single().jsonObject
        assertThat(item.containsKey("listened_at")).isFalse()
        assertThat(item["track_metadata"]!!.jsonObject.containsKey("release_name")).isFalse()
    }

    @Test
    fun mobileDataUsesItsOwnQualityOnlyWhenSet() {
        val settings = MiscSettings(losslessQuality = 27, mobileDataQuality = 5)
        assertThat(settings.forNetwork(onMobileData = true).losslessQuality).isEqualTo(5)
        assertThat(settings.forNetwork(onMobileData = false).losslessQuality).isEqualTo(27)
        assertThat(MiscSettings(losslessQuality = 27).forNetwork(onMobileData = true).losslessQuality).isEqualTo(27)
    }
}
