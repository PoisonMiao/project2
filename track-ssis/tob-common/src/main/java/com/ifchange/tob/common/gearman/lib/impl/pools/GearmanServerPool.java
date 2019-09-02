package com.ifchange.tob.common.gearman.lib.impl.pools;

import com.ifchange.tob.common.gearman.lib.GearmanConsumer;
import com.ifchange.tob.common.gearman.lib.GearmanLostConnectionPolicy;
import com.ifchange.tob.common.gearman.lib.GearmanProducer;
import com.ifchange.tob.common.gearman.lib.GearmanServer;
import com.ifchange.tob.common.gearman.lib.GearmanService;

import java.util.Collection;
import java.util.concurrent.TimeUnit;

/**
 * Both {@link GearmanProducer}s and {@link GearmanConsumer}s are <code>GearmanServerPool</code>s.
 * A gearman server pool allows the user manage the servers within a particular service
 * @author isaiah
 */
public interface GearmanServerPool extends GearmanService {
	/**
	 * Adds a {@link GearmanServer} to the service.<br>
	 * <br>
	 * Note: connections are not made to the server at this time. A connection is only established when needed
	 * @param server
	 * 		The gearman server to add
	 * @return
	 * 		<code>true</code> if the server was added to the service
	 */
    boolean registryServer(GearmanServer server);

	/**
	 * Returns the default reconnect period
	 * @param unit
	 * 		The time unit
	 * @return
	 * 		The about of time before the service attempts to reconnect to a disconnected server
	 */
    long getReconnectPeriod(TimeUnit unit);

	/**
	 * Returns the number of servers managed by this service
	 * @return
	 * 		The number of servers managed by this service
	 */
    int getServerCount();

	/**
	 * Removes all servers from this service
	 */
    void removeAllServers();

	boolean removeServer(GearmanServer server);
	void setClientID(String id);
	String getClientID();
	boolean hasServer(GearmanServer server);

	/**
	 * Returns the collection of servers this service is managing
	 * @return
	 * 		The collection of servers this service is managing
	 */
    Collection<GearmanServer> getServers();

	/**
	 * Sets the {@link GearmanLostConnectionPolicy}. The lost connection policy describes
	 * what should be done in the event that the server unexpectedly disconnects
	 * @param policy
	 * 		The policy for handling unexpected disconnects
	 */
    void setLostConnectionPolicy(GearmanLostConnectionPolicy policy);

	/**
	 * Sets the default reconnect period. When a connection is unexpectedly disconnected, the
	 * will wait a period of time before attempting to reconnect unless otherwise specified
	 * by the {@link GearmanLostConnectionPolicy}
	 * @param time
	 * 		The amount of time before a reconnect is attempted unless otherwise specified
	 * 		by the {@link GearmanLostConnectionPolicy}
	 * @param unit
	 * 		The time unit
	 */
    void setReconnectPeriod(long time, TimeUnit unit);
}
