
package com.ifchange.tob.common.gearman.lib;

import java.io.IOException;
import java.util.Collection;

/**
 * The gearman client is used to submit jobs to the job server.
 * @author isaiah
 */
public interface GearmanProducer extends GearmanService {

	/**
	 * Polls for the job status. This is a blocking operation. The current thread may block and wait
	 * for the operation to complete
	 * @param jobHandle
	 * 		The job handle of the of the job in question.
	 * @return
	 * 		The job status of the job in question.
	 * @throws IOException
	 * 		If an I/O exception occurs while performing this operation
	 */
	GearmanJobStatus getStatus(byte[] jobHandle);

	/**
	 * Sends a job to a registered job server.
	 * @param functionName
	 * 		gearman function name
	 * @param data
	 * 		gearman job data
	 * @return
	 * 		The job return used to poll result data
	 * @throws NullPointerException
	 * 		if the function name is <code>null</code>
	 */
	GearmanJobReturn submitJob(String functionName, byte[] data);

	/**
	 * Sends a job to a registered job server.
	 * @param functionName
	 * 		gearman function name
	 * @param data
	 * 		gearman job data
	 * @param attachment
	 * 		An object used to identify this job from within the
	 * @param callback
	 * 		An asynchronous callback object used to receive result data
	 * @return
	 * 		A joining object used to synchronize jobs
	 * @throws NullPointerException
	 * 		if the function name is <code>null</code> or the callback is null
	 */
	<A> GearmanJoin<A> submitJob(String functionName, byte[] data, A attachment, GearmanJobEventCallback<A> callback);

	/**
	 * Sends a job to a registered job server.
	 * @param functionName
	 * 		gearman function name
	 * @param data
	 * 		gearman job data
	 * @param priority
	 * 		gearman job priority
	 * @return
	 * 		The job return used to poll result data
	 * @throws NullPointerException
	 * 		if the function name is <code>null</code>
	 */
	GearmanJobReturn submitJob(String functionName, byte[] data, GearmanJobPriority priority);

	/**
	 * Sends a job to a registered job server.
	 * @param functionName
	 * 		gearman function name
	 * @param data
	 * 		gearman job data
	 * @param priority
	 * 		gearman job priority
	 * @param attachment
	 * 		An object used to identify this job from within the
	 * @param callback
	 * 		An asynchronous callback object used to receive result data
	 * @return
	 * 		A joining object used to synchronize jobs
	 * @throws NullPointerException
	 * 		If the function name or callback is <code>null</code>
	 */
	<A> GearmanJoin<A> submitJob(String functionName, byte[] data, GearmanJobPriority priority, A attachment, GearmanJobEventCallback<A> callback);

	/**
	 * Submits a background job to a registered job server
	 * @param functionName
	 * 		gearman function name
	 * @param data
	 * 		gearman job data
	 * @return
	 * 		The job return used to poll submit operation status
	 * @throws NullPointerException
	 * 		If the function name is <code>null</code>
	 */
	GearmanJobReturn submitBackgroundJob(String functionName, byte[] data);

	/**
	 * Submits a background job to a registered job server
	 * @param functionName
	 * 		gearman function name
	 * @param data
	 * 		gearman job data
	 * @param attachment
	 * 		An object used to identify this job from within the
	 * @param callback
	 * 		An asynchronous callback object used to receive submit operation status
	 * @return
	 * 		A joining object used to synchronize jobs
	 * @throws NullPointerException
	 * 		if the function name or callback is <code>null</code>
	 */
	<A> GearmanJoin<A> submitBackgroundJob(String functionName, byte[] data, A attachment, GearmanJobEventCallback<A> callback);

	/**
	 * Submits a background job to a registered job server
	 * @param functionName
	 * 		gearman function name
	 * @param data
	 * 		gearman job data
	 * @param priority
	 * 		gearman job priority
	 * @return
	 * 		The job return used to poll submit operation status
	 * @throws NullPointerException
	 * 		If the function name is <code>null</code>
	 */
	GearmanJobReturn submitBackgroundJob(String functionName, byte[] data, GearmanJobPriority priority);

	/**
	 * Submits a background job to a registered job server
	 * @param functionName
	 * 		gearman function name
	 * @param data
	 * 		gearman job data
	 * @param priority
	 * 		gearman job priority
	 * @param attachment
	 * 		An object used to identify this job from within the
	 * @param callback
	 * 		An asynchronous callback object used to receive submit operation status
	 * @return
	 * 		A joining object used to synchronize jobs
	 * @throws NullPointerException
	 * 		if the function name or callback is <code>null</code>
	 */
	<A> GearmanJoin<A> submitBackgroundJob(String functionName, byte[] data, GearmanJobPriority priority, A attachment, GearmanJobEventCallback<A> callback);

	boolean registryServer(GearmanServer server);

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

	/**
	 * Removes the given server from the list of available server to
	 * @param server
	 * 		The server to remove
	 * @return
	 * 		<code>true</code> if the service contained the given server and it was successfully removed. <code>false</code> if the service did not contain the given server
	 */
	boolean removeServer(GearmanServer server);

	/**
	 * Sets the client ID
	 * @param id
	 * 		the new client ID
	 */
	void setClientID(String id);

	/**
	 * Gets the current client ID
	 * @return
	 * 		The current client ID
	 */
	String getClientID();

	/**
	 * Tests if this client has the given server
	 * @param server
	 * 		The given server
	 * @return
	 * 		<code>true</code> if this client contains the given server
	 */
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
}
