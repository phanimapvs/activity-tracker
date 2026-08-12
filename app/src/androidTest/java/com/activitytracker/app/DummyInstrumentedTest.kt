package com.activitytracker.app

import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class DummyInstrumentedTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Test
    fun dummyTest() {
        assertTrue(true)
    }
}
