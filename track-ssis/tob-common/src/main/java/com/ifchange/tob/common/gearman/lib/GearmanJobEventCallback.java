package com.ifchange.tob.common.gearman.lib;

/**
 * The GearmanJobEventCallback class is used to handle job result events (
 * {@link GearmanJobEvent}) asynchronously. The callback will be called using
 * gearman threads. Only one event, for a given job, is processed at a time.
 *
 * @author isaiah.v
 * @param <A>
 *            attachment type
 */
public interface GearmanJobEventCallback<A> {

	/**
	 * This method is called when a job event occurs.
	 *
	 * @param attachment
	 *            The attachment defined in the submit operation
	 * @param event
	 *            The job event that has occurred
	 */
    void onEvent(A attachment, GearmanJobEvent event);
}
