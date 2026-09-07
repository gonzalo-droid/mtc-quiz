package com.gondroid.configuration.presentation.customize

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class EvaluationSetupTest {

    @Test
    fun `the official setup needs 32 of 40 and gives a minute per question`() {
        val setup = EvaluationSetup(minutes = 40, questions = 40, passPercentage = 80)

        assertThat(setup.correctToPass).isEqualTo(32)
        assertThat(setup.allowedMistakes).isEqualTo(8)
        assertThat(setup.secondsPerQuestion).isEqualTo(60)
        assertThat(setup.pace).isEqualTo(Pace.COMFORTABLE)
    }

    @Test
    fun `a partial correct answer rounds up, never down`() {
        // 75 % of 10 is 7.5: passing with 7 would be 70 %, below the mark the user set
        assertThat(EvaluationSetup(10, 10, 75).correctToPass).isEqualTo(8)
        assertThat(EvaluationSetup(10, 3, 50).correctToPass).isEqualTo(2)
    }

    @Test
    fun `pace crosses at one minute and at half a minute per question`() {
        assertThat(EvaluationSetup(40, 40, 80).pace).isEqualTo(Pace.COMFORTABLE)
        assertThat(EvaluationSetup(30, 40, 80).pace).isEqualTo(Pace.TIGHT)
        assertThat(EvaluationSetup(15, 40, 80).pace).isEqualTo(Pace.AGAINST_THE_CLOCK)
    }

    @Test
    fun `seconds per question truncate, so the figure is time you can count on`() {
        // 5 minutes over 40 questions is 7.5 s: showing 8 would promise time that isn't there
        assertThat(EvaluationSetup(5, 40, 80).secondsPerQuestion).isEqualTo(7)
    }

    @Test
    fun `every derived value survives zero questions`() {
        val empty = EvaluationSetup(minutes = 40, questions = 0, passPercentage = 80)

        assertThat(empty.correctToPass).isEqualTo(0)
        assertThat(empty.allowedMistakes).isEqualTo(0)
        assertThat(empty.secondsPerQuestion).isEqualTo(0)
        assertThat(empty.pace).isEqualTo(Pace.COMFORTABLE)
    }

    @Test
    fun `passing at 100 percent leaves no room for mistakes`() {
        val strict = EvaluationSetup(minutes = 40, questions = 40, passPercentage = 100)

        assertThat(strict.correctToPass).isEqualTo(40)
        assertThat(strict.allowedMistakes).isEqualTo(0)
    }
}
