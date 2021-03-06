package com.ifchange.tob.common.gearman.lib;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * A Gearman Worker is responsible for executing the jobs it receives from the
 * job server. A Worker registers with the Job Server the types of jobs that it
 * may execute, the server will use this information, along with other
 * attributes, to determine which Worker will execute a particular job request.
 * As data is generated or as a job's state changes, the worker passes this
 * information back to the Job Server.
 */
public interface GearmanConsumer extends GearmanService {

	/**
	 * Sets the maximum number of jobs that can execute at a given time
	 *
	 * @param maxConcurrentJobs
	 *            The maximum number of jobs that can execute at a given time
	 */
	void setMaximumConcurrency(int maxConcurrentJobs);

	/**
	 * The maximum number of jobs that may execute at a given time
	 *
	 * @return The number of jobs that that may execute concurrently
	 */
	int getMaximumConcurrency();

	/**
	 * Registers a particular {@link GearmanFunction} with the worker. Once a
	 * function has been registered with a worker, the worker is capable of
	 * executing any job that matches the registered function. Upon registering
	 * a function, the Worker notifies all Gearman Job Servers that is can
	 * accept any job that contains the applicable function.
	 *
	 * @param name
	 *            The gearman function name
	 * @param function
	 *            The function being registered with the worker.
	 * @return The gearman function who was previously assigned to the given
	 *         function name
	 */
	GearmanFunction subscribeFunction(String name, GearmanFunction function);

	/**
	 * Returns the gearman function associated with the given function name
	 *
	 * @param name
	 *            The function name
	 * @return The gearman function registered with the given function name
	 */
	GearmanFunction getFunction(String name);

	/**
	 * Retrieve the names of all functions that have been registered with this
	 * worker. If no functions have been registered, any empty set should be
	 * returned.
	 *
	 * @return The name of all registered functions.
	 */
	Set<String> getRegisteredFunctions();

	/**
	 * Unregisters a particular {@link GearmanFunction} from the worker. Once a
	 * function has been unregistered from the Worker, a Worker will no longer
	 * accept jobs which require the execution of the unregistered function.
	 *
	 * @param functionName
	 *            The name of the function to unregister
	 */
	boolean removeFunction(String functionName);

	/**
	 * Unregisters all{@link GearmanFunction} from the worker. The effect of
	 * which is that the worker will not execute any new jobs.
	 */
	void removeAllFunctions();

	/**
	 * Adds a {@link GearmanServer} to the service.<br>
	 * <br>
	 * Note: connections are not made to the server at this time. A connection
	 * is only established when needed
	 *
	 * @param server
	 *            The gearman server to add
	 * @return <code>true</code> if the server was added to the service
	 */
	boolean registryServer(GearmanServer server);

	/**
	 * Returns the default reconnect period
	 *
	 * @param unit
	 *            The time unit
	 * @return The about of time before the service attempts to reconnect to a
	 *         disconnected server
	 */
	long getReconnectPeriod(TimeUnit unit);

	/**
	 * Returns the number of servers managed by this service
	 *
	 * @return The number of servers managed by this service
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
	 *
	 * @return The collection of servers this service is managing
	 */
	Collection<GearmanServer> getServers();

	/**
	 * Sets the {@link GearmanLostConnectionPolicy}. The lost connection policy
	 * describes what should be done in the event that the server unexpectedly
	 * disconnects
	 *
	 * @param policy
	 *            The policy for handling unexpected disconnects
	 */
	void setLostConnectionPolicy(GearmanLostConnectionPolicy policy);

	/**
	 * Sets the default reconnect period. When a connection is unexpectedly
	 * disconnected, the will wait a period of time before attempting to
	 * reconnect unless otherwise specified by the
	 * {@link GearmanLostConnectionPolicy}
	 *
	 * @param time
	 *            The amount of time before a reconnect is attempted unless
	 *            otherwise specified by the {@link GearmanLostConnectionPolicy}
	 * @param unit
	 *            The time unit
	 */
	void setReconnectPeriod(long time, TimeUnit unit);
}
