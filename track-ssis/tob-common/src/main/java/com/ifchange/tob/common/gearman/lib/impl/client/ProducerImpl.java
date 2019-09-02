package com.ifchange.tob.common.gearman.lib.impl.client;

import com.ifchange.tob.common.gearman.lib.Gearman;
import com.ifchange.tob.common.gearman.lib.GearmanJobEvent;
import com.ifchange.tob.common.gearman.lib.GearmanJobEventCallback;
import com.ifchange.tob.common.gearman.lib.GearmanJobPriority;
import com.ifchange.tob.common.gearman.lib.GearmanJobReturn;
import com.ifchange.tob.common.gearman.lib.GearmanJobStatus;
import com.ifchange.tob.common.gearman.lib.GearmanJoin;
import com.ifchange.tob.common.gearman.lib.GearmanLostConnectionAction;
import com.ifchange.tob.common.gearman.lib.GearmanLostConnectionGrounds;
import com.ifchange.tob.common.gearman.lib.GearmanLostConnectionPolicy;
import com.ifchange.tob.common.gearman.lib.GearmanProducer;
import com.ifchange.tob.common.gearman.lib.GearmanServer;
import com.ifchange.tob.common.gearman.lib.helpers.ByteArray;
import com.ifchange.tob.common.gearman.lib.helpers.GearmanUtils;
import com.ifchange.tob.common.gearman.lib.helpers.TaskJoin;
import com.ifchange.tob.common.gearman.lib.impl.pools.AbstractJobServerPool;
import com.ifchange.tob.common.gearman.lib.impl.pools.ControllerState;
import com.ifchange.tob.common.gearman.lib.impl.pools.GearmanJobStatusImpl;

import java.util.Deque;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class ProducerImpl extends AbstractJobServerPool<ProducerImpl.InnerConnectionController> implements GearmanProducer {

	protected class InnerConnectionController extends ClientConnectionController {


		protected InnerConnectionController(GearmanServer key) {
			super(ProducerImpl.this, key);
		}

		@Override
		protected ClientJobSubmission pollNextJob() {
			return ProducerImpl.this.pollJob();
		}

		@Override
		protected void requeueJob(ClientJobSubmission jobSub) {
			ProducerImpl.this.requeueJob(jobSub);
		}

		@Override
		public void onLostConnection(GearmanLostConnectionPolicy policy, GearmanLostConnectionGrounds grounds) {
			super.close();

			if(this.getKey().isShutdown()) {
				policy.shutdownServer(this.getKey());
				super.dropServer();
				return;
			}

			GearmanLostConnectionAction action;

			try {
				action = policy.lostConnection(this.getKey(), grounds);
			} catch (Throwable t) {
				action = null;
			}

			if(action==null) {
				action = ProducerImpl.this.getDefaultPolicy().lostConnection(this.getKey(), grounds);
				assert action!=null;
			}

			if(action.equals(GearmanLostConnectionAction.DROP)) {
				super.dropServer();
			} else {
				switch(grounds) {
				case UNEXPECTED_DISCONNECT:
				case RESPONSE_TIMEOUT:
					ProducerImpl.this.removeFromOpen(this);
					break;
				case FAILED_CONNECTION:
					ProducerImpl.this.onFailedConnection(this);
					break;
				default:
					assert false;
				}
			}
		}

		@Override
		public void onNew() {
			ProducerImpl.this.addController(this);
		}

		@Override
		public void onDrop(ControllerState oldState) {
			super.close();
			ProducerImpl.this.dropController(this, oldState);
		}

		@Override
		public void onWait(ControllerState oldState) {
			super.close();
		}

		@Override
		public void onClose(ControllerState oldState) {
			super.close();
			super.onClose(oldState);
			if(oldState.equals(ControllerState.OPEN))
				ProducerImpl.this.onClose(this);
		}

		@Override
		public void onConnect(ControllerState oldState) {
			super.getKey().createGearmanConnection(this, this);
		}

		@Override
		public void onOpen(ControllerState oldState) {
			ProducerImpl.this.onConnectionOpen(this);
		}
	}

	/** The set of open connections */
	private final Queue<InnerConnectionController> open = new LinkedBlockingQueue<InnerConnectionController>();

	/** The set of available connections*/
	private final ClientConnectionList<InnerConnectionController, ClientJobSubmission> available = new ClientConnectionList<InnerConnectionController, ClientJobSubmission>();

	/** The set of jobs waiting to be submitted */
	private final Deque<ClientJobSubmission> jobQueue = new LinkedBlockingDeque<ClientJobSubmission>();

	public ProducerImpl(Gearman gearman) {
		super(gearman, new ClientLostConnectionPolicy(), 0L, TimeUnit.MILLISECONDS);
	}

	@Override
	protected InnerConnectionController createController(GearmanServer key) {
		return new InnerConnectionController(key);
	}

	private final void addJob(ClientJobSubmission job) {

		InnerConnectionController conn = null;

		synchronized(this.open) {

			if(!this.open.isEmpty()) {
				this.jobQueue.addLast(job);

				for(InnerConnectionController icc : this.open) {
					if(icc.grab()) return;
				}

				final InnerConnectionController icc;
				if ((icc = this.available.tryFirst(null))!=null){
					// Make a connection
					conn = icc;
				}

			} else {

				final InnerConnectionController icc;
				if ((icc = this.available.tryFirst(job))!=null){
					// Add job to job queue
					this.jobQueue.addLast(job);

					// Make a connection
					conn = icc;
				} else {
					// No available servers to connect to, fail job
					job.jobReturn.put(GearmanJobEventImmutable.GEARMAN_SUBMIT_FAIL_SERVICE_SHUTDOWN);
				}
			}
		}

		if(conn!=null) conn.openServer(false);
	}

	private final void onConnectionOpen(final InnerConnectionController icc) {
		synchronized(this.open) {
			if(this.open.isEmpty())
				this.available.clearFailKeys();

			assert this.available.contains(icc);

			Object t1;
			t1 = this.available.remove(icc);
			assert t1==null && !this.available.contains(icc);

			this.open.add(icc);

			icc.grab();
		}
	}

	private final void addController(final InnerConnectionController icc) {
		synchronized(this.open) {
			this.available.add(icc);
		}
	}

	private final void dropController(final InnerConnectionController icc, final ControllerState oldState) {
		synchronized(this.open) {
			assert icc.getState().equals(ControllerState.DROPPED);

			switch(oldState) {
			case CONNECTING:
			case CLOSED:
				assert this.available.contains(icc);
				assert !this.open.contains(icc);

				final ClientJobSubmission job = this.available.remove(icc);
				if(job!=null) {
					// There should be no fail keys while there are open connections
					assert this.open.isEmpty();
					this.failTo(job, GearmanJobEventImmutable.GEARMAN_SUBMIT_FAIL_CONNECTION_FAILED);
				}

				break;
			case CLOSE_PENDING:
			case OPEN:
				assert this.open.contains(icc);
				assert !this.available.contains(icc);

				boolean t = this.open.remove(icc);
				assert t;

				break;
			case WAITING:
				assert !this.open.contains(icc);
				assert !this.available.contains(icc);
				break;
			case DROPPED:
				assert false;
				break;
			default:
				throw new IllegalStateException("unknown controller state");
			}
		}
	}

	/**
	 * Call when there is an expected disconnect.
	 * @param icc
	 */
	private final void onClose(final InnerConnectionController icc) {

		/*
		 * Move the connection controller from the open set to the available set
		 */

		InnerConnectionController openNext = null;

		synchronized(this.open) {

			// The controller should be in the open set
			assert this.open.contains(icc);

			// remove the controller from the open set
			boolean test;
			test = this.open.remove(icc);
			assert test;

			/*
			 * If if the set of open connections is empty and there are still jobs in the
			 * queue, attempt to make a connection
			 */
			if(this.open.isEmpty()) {
				/*
				 * Note: if the disconnect causes jobs to be added to the job queue,
				 * it should be added before this method is called
				 */

				// Grab the last job added to the job queue, if one exits
				final ClientJobSubmission job = this.jobQueue.peekLast();
				if(job!=null) {
					// If there are jobs in the jobQueue, make a new connection

					// try to make a new connection and set the fail key
					final InnerConnectionController conn = this.available.tryFirst(job);

					if(conn!=null) {
						assert conn.getState().equals(ControllerState.CLOSED)
							|| conn.getState().equals(ControllerState.CONNECTING);

						openNext = conn;

					} else {
						// If conn is null, then there are no other available connections
						this.failTo(job, GearmanJobEventImmutable.GEARMAN_SUBMIT_FAIL_SERVER_NOT_AVAILABLE);
					}
				}
			}

			test= this.available.addFirst(icc);
			assert test;
		}

		if(openNext!=null) {
			boolean test = openNext.openServer(false);
			assert test;
		}
	}

	private final void onFailedConnection(final InnerConnectionController icc) {
		synchronized(this.open) {
			assert this.available.contains(icc);
			final ClientJobSubmission cjs = this.available.remove(icc);
			assert !this.available.contains(icc);

			if(cjs!=null) {
				// There should be no fail keys while there are open connections
				assert this.open.isEmpty();

				this.failTo(cjs, GearmanJobEventImmutable.GEARMAN_SUBMIT_FAIL_CONNECTION_FAILED);
			}

			this.available.add(icc);
		}
	}

	private final void failTo(final ClientJobSubmission job, GearmanJobEvent failevent) {

		synchronized(this.open) {
			assert this.open.isEmpty();
			assert this.jobQueue.contains(job);

			ClientJobSubmission current;
			do {
				current=this.jobQueue.pollFirst();
				current.jobReturn.eof(failevent);
			} while(current!=job);
		}
	}

	private final ClientJobSubmission pollJob() {
		return this.jobQueue.poll();
	}

	private final void requeueJob(ClientJobSubmission job) {
		this.jobQueue.addFirst(job);
	}

	private final void removeFromOpen(final InnerConnectionController icc) {
		synchronized(this.open) {
			assert icc.getState().equals(ControllerState.CLOSED);
			assert this.open.contains(icc);

			this.open.remove(icc);
		}
	}

	@Override
	public final void shutdown() {
		synchronized(this.open) {
			for(ClientJobSubmission jobSub : this.jobQueue){
				jobSub.jobReturn.eof(GearmanJobEventImmutable.GEARMAN_SUBMIT_FAIL_SERVICE_SHUTDOWN);
			}
			this.open.clear();
			this.available.clear();

			super.shutdown();
			super.getGearman().onServiceShutdown(this);
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public GearmanJobStatus getStatus(byte[] jobHandle) {
		final ByteArray byteArray = new ByteArray(jobHandle);

		List<InnerConnectionController> avail;

		TaskJoin<GearmanJobStatus>[] taskJoins;
		synchronized(this.open) {
			taskJoins = new TaskJoin[this.open.size()];
			int i=0;
			for(InnerConnectionController icc : this.open) {
				taskJoins[i] = icc.getStatus(byteArray);
			}
			avail = this.available.createList();
		}

		for(TaskJoin<GearmanJobStatus> taskJoin : taskJoins) {
			GearmanJobStatus jobStatus = taskJoin.getValue();
			if(jobStatus.isKnown()) return jobStatus;
		}

		for(InnerConnectionController icc : avail) {
			TaskJoin<GearmanJobStatus> taskJoin = icc.getStatus(byteArray);
			GearmanJobStatus jobStatus = taskJoin.getValue();
			if(jobStatus.isKnown()) return jobStatus;
		}

		return GearmanJobStatusImpl.NOT_KNOWN;
	}

	@Override
	public GearmanJobReturn submitJob(String functionName, byte[] data) {
		return submitJob(functionName, data, GearmanJobPriority.NORMAL_PRIORITY, false);
	}

	@Override
	public GearmanJobReturn submitJob(String functionName, byte[] data, GearmanJobPriority priority) {
		return submitJob(functionName, data, priority, false);
	}

	@Override
	public GearmanJobReturn submitBackgroundJob(String functionName, byte[] data) {
		return submitJob(functionName, data, GearmanJobPriority.NORMAL_PRIORITY, true);
	}

	@Override
	public GearmanJobReturn submitBackgroundJob(String functionName, byte[] data, GearmanJobPriority priority) {
		return submitJob(functionName, data, priority, true);
	}

	private GearmanJobReturn submitJob(String functionName, byte[] data, GearmanJobPriority priority, boolean isBackground) {
		final GearmanJobReturnImpl jobReturn = new GearmanJobReturnImpl();
		submitJob(jobReturn, functionName, data, priority, isBackground);
		return jobReturn;
	}

	private void submitJob(BackendJobReturn jobReturn, String functionName, byte[] data, GearmanJobPriority priority, boolean isBackground) {
		if(functionName==null) throw new NullPointerException();
		if(data==null) data = new byte[0];
		if(priority==null) priority = GearmanJobPriority.NORMAL_PRIORITY;

		if(this.isShutdown()) {
			jobReturn.eof(GearmanJobEventImmutable.GEARMAN_SUBMIT_FAIL_SERVICE_SHUTDOWN);
			return;
		} else if (super.getServerCount()==0) {
			jobReturn.eof(GearmanJobEventImmutable.GEARMAN_SUBMIT_FAIL_SERVER_NOT_AVAILABLE);
			return;
		}

		this.addJob(new ClientJobSubmission(functionName, data, GearmanUtils.createUID() , jobReturn, priority, isBackground));
	}

	@Override
	public <A> GearmanJoin<A> submitJob(String functionName, byte[] data, A attachment, GearmanJobEventCallback<A> callback) {
		return submitJob(functionName, data, GearmanJobPriority.NORMAL_PRIORITY, false, attachment, callback);
	}

	@Override
	public <A> GearmanJoin<A> submitJob(String functionName, byte[] data, GearmanJobPriority priority, A attachment, GearmanJobEventCallback<A> callback) {
		return submitJob(functionName, data, priority, false, attachment, callback);
	}

	@Override
	public <A> GearmanJoin<A> submitBackgroundJob(String functionName, byte[] data, A attachment, GearmanJobEventCallback<A> callback) {
		return submitJob(functionName, data, GearmanJobPriority.NORMAL_PRIORITY, true, attachment, callback);
	}

	@Override
	public <A> GearmanJoin<A> submitBackgroundJob(String functionName, byte[] data, GearmanJobPriority priority, A attachment, GearmanJobEventCallback<A> callback) {
		return submitJob(functionName, data, priority, true, attachment, callback);
	}

	private <A> GearmanJoin<A> submitJob(String functionName, byte[] data, GearmanJobPriority priority, boolean isBackground, A attachment, GearmanJobEventCallback<A> callback) {
		if(callback==null) throw new NullPointerException();

		final GearmanJobEventCallbackCaller<A> jobReturn = new GearmanJobEventCallbackCaller<A>(attachment, callback, this.getGearman().getScheduler());
		submitJob(jobReturn, functionName, data, priority, isBackground);
		return jobReturn;
	}
}
