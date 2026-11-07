package de.rearstudio
import org.junit.Assert.*
import org.junit.Test
class StepDayTest {
 @Test fun incrementsWithoutInventingEarlierHistory(){val day=StepDay("2026-11-07",0,5000);assertEquals(30L,day.observe(day.date,5030).steps)}
 @Test fun rebootCounterResetsKeepAccumulatedSteps(){val day=StepDay("2026-11-07",200,5000);assertEquals(207L,day.observe(day.date,7).steps)}
 @Test fun newDayDoesNotAttributeUnknownMidnightSteps(){val day=StepDay("2026-11-07",200,5000);val next=day.observe("2026-11-08",5100);assertEquals(0L,next.steps);assertEquals(5100L,next.lastCounter)}
 @Test fun duplicateEventsDoNotDoubleCount(){val day=StepDay("2026-11-07",100,5000);assertEquals(day,day.observe(day.date,5000))}
}
