package com.ifchange.tob.common.gearman.lib;

import java.io.Serializable;

/**
 * Used to define the job priority at the time of submission
 * @author isaiah
 */
public enum GearmanJobPriority implements Serializable {
	/** Low job priority */
	LOW_PRIORITY,
	/** Normal job priority */
	NORMAL_PRIORITY,
	/** High job priority */
	HIGH_PRIORITY
}
