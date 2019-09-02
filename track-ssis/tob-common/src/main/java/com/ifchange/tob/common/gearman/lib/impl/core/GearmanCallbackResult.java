package com.ifchange.tob.common.gearman.lib.impl.core;

public interface GearmanCallbackResult {

	/**
	 * Tests if the operation completed successfully
	 * @return
	 * 		<code>true</code> if and only if the operation was successful
	 */
    boolean isSuccessful();
}
