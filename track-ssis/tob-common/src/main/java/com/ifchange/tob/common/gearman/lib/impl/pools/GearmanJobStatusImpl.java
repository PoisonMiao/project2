package com.ifchange.tob.common.gearman.lib.impl.pools;

import com.ifchange.tob.common.gearman.lib.GearmanJobStatus;

public class GearmanJobStatusImpl implements GearmanJobStatus {

	public static final GearmanJobStatus NOT_KNOWN = new GearmanJobStatusImpl(false, false, 0, 0);

	private final boolean isKnown;
	private final boolean isRunning;
	private final long numerator;
	private final long denominator;

	GearmanJobStatusImpl(boolean isKnown, boolean isRunning, long num, long den) {
		this.isKnown = isKnown;
		this.isRunning = isRunning;
		this.numerator = num;
		this.denominator = den;
	}

	@Override
	public boolean isKnown() {
		return isKnown;
	}

	@Override
	public boolean isRunning() {
		return isRunning;
	}

	@Override
	public long getNumerator() {
		return numerator;
	}

	@Override
	public long getDenominator() {
		return denominator;
	}

}
