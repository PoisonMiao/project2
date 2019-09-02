package com.ifchange.tob.common.gearman.lib;

/**
 * An immutable object specifying a job's status
 * @author isaiah
 */
public interface GearmanJobStatus {

	/**
	 * Tests if the server knew the status of the job in question.
	 *
	 * Most job servers will return unknown if it never received the job or
	 * if the job has already been completed.
	 *
	 * If the status is known but not running, then a worker has not yet
	 * polled the job
	 *
	 * @return
	 * 		<code>true</code> if the server knows the status of the job in question
	 */
    boolean isKnown();

	/**
	 * Tests if the job is currently running.
	 *
	 * If the status is unknown, this value will always be false.
	 *
	 * @return
	 * 		<code>true</code> if the job is currently being worked on. <code>
	 * 		false</code> otherwise.
	 */
    boolean isRunning();

	/**
	 * The percent complete numerator.
	 * @return
	 * 		the percent complete numerator.
	 */
    long getNumerator();

	/**
	 * The percent complete denominator.
	 * @return
	 * 		the percent complete denominator.
	 */
    long getDenominator();
}
