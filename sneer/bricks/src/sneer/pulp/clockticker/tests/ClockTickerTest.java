package sneer.pulp.clockticker.tests;

import static sneer.commons.environments.Environments.my;

import org.junit.Test;

import sneer.brickness.testsupport.BrickTest;
import sneer.pulp.clock.Clock;
import sneer.pulp.clockticker.ClockTicker;
import sneer.pulp.threads.Threads;

public class ClockTickerTest extends BrickTest {

	private final Clock _clock = my(Clock.class);

	{
		my(ClockTicker.class);
	}

	@Test (timeout = 3000)
	public void testTicking() {
		waitForATick();
		waitForATick();
	}

	private void waitForATick() {
		long t0 = _clock.time();
		while (t0 == _clock.time());
			my(Threads.class).sleepWithoutInterruptions(1);
	}
	
}
