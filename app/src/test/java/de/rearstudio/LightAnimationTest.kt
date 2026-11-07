package de.rearstudio

import org.junit.Assert.*
import org.junit.Test

class LightAnimationTest {
    @Test fun delayedFiniteAnimationFadesAndStops() {
        val a=LightAnimation(enabled=true,brightness=100,periodMs=1000,delayMs=500,fadeMs=200,repeats=2)
        assertEquals(0f,a.frame(499).alpha,0f)
        assertEquals(.5f,a.frame(600).alpha,.001f)
        assertEquals(1f,a.frame(1000).alpha,0f)
        assertEquals(.5f,a.frame(2400).alpha,.001f)
        assertEquals(0f,a.frame(2500).alpha,0f)
    }
    @Test fun reducedMotionKeepsLightStaticAndDisabledIsDark() {
        val a=LightAnimation(enabled=true,effect="blink",brightness=100,fadeMs=0)
        assertEquals(0f,a.frame(1800).alpha,0f)
        assertEquals(1f,a.frame(1800,true).alpha,0f)
        assertEquals(0f,a.frame(1800,true).phase,0f)
        assertEquals(0f,a.copy(enabled=false).frame(1800).alpha,0f)
    }
    @Test fun optionsRoundTripAndBadInputsAreBounded() {
        val a=LightAnimation(enabled=true,effect="chase",palette="dual",clockwise=false,delayMs=100)
        assertEquals(a,LightAnimation.read(a.json()))
        val b=LightAnimation.read(a.json().put("periodMs",0).put("brightness",900).put("repeats",-3))
        assertEquals(250L,b.periodMs);assertEquals(100,b.brightness);assertEquals(0,b.repeats)
    }
}
