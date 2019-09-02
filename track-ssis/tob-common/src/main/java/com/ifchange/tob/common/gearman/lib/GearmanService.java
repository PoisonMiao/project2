package com.ifchange.tob.common.gearman.lib;

/**
 * All Gearman services implements the {@link GearmanService} interface
 */
public interface GearmanService {

	/**
	 * Closes the gearman service and all services created or maintained by this service
	 */
    void shutdown();

	/**
	 * Tests if this gearman service has been shutdown
	 * @return
	 * 		<code>true</code> if this gearman service is shutdown
	 */
    boolean isShutdown();

	/**
	 * Returns the creating {@link Gearman} instance
	 */
    Gearman getGearman();
}
