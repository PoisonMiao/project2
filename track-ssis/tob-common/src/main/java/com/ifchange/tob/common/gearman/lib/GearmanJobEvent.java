package com.ifchange.tob.common.gearman.lib;

/**
 * An object representing job update
 * @author isaiah
 */
public interface GearmanJobEvent {

	/**
	 * The job event type. This defines the type of job update received.
	 * @return
	 * 		The job event type
	 * @see GearmanJobEventType
	 */
    GearmanJobEventType getEventType();

	/**
	 * The result's data
	 * @return
	 * 		The data defined by the result type.
	 */
    byte[] getData();
}
