package de.rearstudio

import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HubIntegrationTest {
    @Test fun sportsHubOpensWithoutNetworkOrCachedData(){ActivityScenario.launch(SportsHubActivity::class.java).use{}}
    @Test fun musicPlayerOpensWithoutASession(){ActivityScenario.launch(MusicDeckActivity::class.java).use{}}
}
