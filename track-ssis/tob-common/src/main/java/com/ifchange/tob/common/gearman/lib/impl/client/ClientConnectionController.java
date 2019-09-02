package com.ifchange.tob.common.gearman.lib.impl.client;

import com.ifchange.tob.common.gearman.lib.GearmanJobEventType;
import com.ifchange.tob.common.gearman.lib.GearmanJobPriority;
import com.ifchange.tob.common.gearman.lib.GearmanServer;
import com.ifchange.tob.common.gearman.lib.helpers.ByteArray;
import com.ifchange.tob.common.gearman.lib.helpers.GearmanUtils;
import com.ifchange.tob.common.gearman.lib.impl.core.GearmanCallbackHandler;
import com.ifchange.tob.common.gearman.lib.impl.core.GearmanConnection;
import com.ifchange.tob.common.gearman.lib.impl.core.GearmanConnection.SendCallbackResult;
import com.ifchange.tob.common.gearman.lib.impl.core.GearmanPacket;
import com.ifchange.tob.common.gearman.lib.impl.pools.AbstractConnectionController;
import com.ifchange.tob.common.gearman.lib.impl.pools.AbstractJobServerPool;
import com.ifchange.tob.common.gearman.lib.impl.pools.ControllerState;

import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;

import static com.ifchange.tob.common.gearman.lib.helpers.GearmanUtils.LOGGER;

abstract class ClientConnectionController extends AbstractConnectionController {

	private static final int RESPONCE_TIMEOUT = 19000;	// TODO decouple property
	private static final int IDLE_TIMEOUT = 9000;		// TODO decouple property

	private final InnerGearmanCallback jobSendCallback = new InnerGearmanCallback();

	/**
	 * The set of executing jobs. The key is the job's handle and the value is the job itself
	 */
	private final ConcurrentHashMap<ByteArray, BackendJobReturn> jobs = new ConcurrentHashMap<>();
	private ClientJobSubmission pendingJob = null;

	private long responceTimeout = Long.MAX_VALUE;
	private long idleTimeout = Long.MAX_VALUE;

	protected ClientConnectionController(AbstractJobServerPool<?> sc, GearmanServer key) {
		super(sc, key);
	}

	@Override
	public void onClose(ControllerState oldState) {
		for(BackendJobReturn bjr : jobs.values()) {
			bjr.eof(GearmanJobEventImmutable.GEARMAN_JOB_DISCONNECT);
		}
	}

	public final void timeoutCheck(long time) {
		if(time-this.responceTimeout>RESPONCE_TIMEOUT) {
			super.timeout();
		} else if(this.jobs.isEmpty() && this.pendingJob==null && time-this.idleTimeout>IDLE_TIMEOUT) {
			this.closeServer();
		}
	}

	protected final void close() {
		if(this.pendingJob!=null) {
			this.requeueJob(pendingJob);
			this.pendingJob = null;
		}

		Iterator<BackendJobReturn> it = this.jobs.values().iterator();
		while(it.hasNext()) {
			BackendJobReturn jobReturn = it.next();
			it.remove();
			jobReturn.eof(isShutdown() ? GearmanJobEventImmutable.GEARMAN_SUBMIT_FAIL_SERVICE_SHUTDOWN : GearmanJobEventImmutable.GEARMAN_JOB_DISCONNECT);
		}

		this.responceTimeout = Long.MAX_VALUE;
		this.idleTimeout = Long.MAX_VALUE;
	}

	protected abstract ClientJobSubmission pollNextJob();
	protected abstract void requeueJob(ClientJobSubmission jobSub);

	protected final boolean grab() {

		final ClientJobSubmission jobSub;
		synchronized(this) {
			if(this.pendingJob!=null) return false;
			jobSub = this.pollNextJob();

			if(jobSub!=null)
				this.pendingJob = jobSub;
			else
				return false;
		}

		final GearmanJobPriority p = jobSub.priority;
		final String funcName = jobSub.functionName;
		final byte[] data = jobSub.data;
		final byte[] uID = jobSub.uniqueID;

		if(jobSub.isBackground) {
			switch(p) {
			case LOW_PRIORITY:
				this.sendPacket(GearmanPacket.createSUBMIT_JOB_LOW_BG(funcName, uID, data), jobSendCallback);
				break;
			case HIGH_PRIORITY:
				this.sendPacket(GearmanPacket.createSUBMIT_JOB_HIGH_BG(funcName, uID, data), jobSendCallback);
				break;
			case NORMAL_PRIORITY:
				this.sendPacket(GearmanPacket.createSUBMIT_JOB_BG(funcName, uID, data), jobSendCallback);
				break;
			}
		} else {

			switch(p) {
			case LOW_PRIORITY:
				this.sendPacket(GearmanPacket.createSUBMIT_JOB_LOW(funcName, uID, data), jobSendCallback);
				break;
			case HIGH_PRIORITY:
				this.sendPacket(GearmanPacket.createSUBMIT_JOB_HIGH(funcName, uID, data), jobSendCallback);
				break;
			case NORMAL_PRIORITY:
				this.sendPacket(GearmanPacket.createSUBMIT_JOB(funcName, uID, data), jobSendCallback);
				break;
			}
		}

		return true;
	}

	@Override
	public void onPacketReceived(GearmanPacket packet, GearmanConnection<Object> conn) {
		switch (packet.getPacketType()) {
		case JOB_CREATED:
			jobCreated(packet);
			break;
		case WORK_STATUS:
			workStatus(packet);
			break;
		case WORK_COMPLETE:
			workComplete(packet);
			break;
		case WORK_FAIL:
			workFail(packet);
			break;
		case ECHO_RES:	// Not used
			assert false;
			break;
		case ERROR:
			error(packet);
			break;
		case STATUS_RES:
			super.onStatusReceived(packet);
			break;
		case WORK_EXCEPTION:
			//workException(mysql);  //TODO Don't implement yet
			assert false;
			break;
		case OPTION_RES:
			//TODO
			break;
		case WORK_DATA:
			workData(packet);
			break;
		case WORK_WARNING:
			workWarning(packet);
			break;
		default: // Not a client response
			assert false;
		}
	}

	private final void workWarning(final GearmanPacket packet) {
		final ByteArray jobHandle = new ByteArray(packet.getArgumentData(0));
		final BackendJobReturn jobReturn = this.jobs.get(jobHandle);

		if(jobReturn==null) {
			LOGGER.warn("Unexspected Packet : WORK_WARNING : "+ jobHandle.toString(GearmanUtils.getCharset()));
			return;
		}

		final byte[] warning = packet.getArgumentData(1);
		jobReturn.put(new GearmanJobEventImpl(GearmanJobEventType.GEARMAN_JOB_WARNING, warning));
	}

	private final void workData(final GearmanPacket packet) {
		final ByteArray jobHandle = new ByteArray(packet.getArgumentData(0));
		final BackendJobReturn jobReturn = this.jobs.get(jobHandle);

		if(jobReturn==null) {
			LOGGER.warn("Unexspected Packet : WORK_DATA : "+ jobHandle.toString(GearmanUtils.getCharset()));
			return;
		}

		final byte[] data= packet.getArgumentData(1);
		jobReturn.put(new GearmanJobEventImpl(GearmanJobEventType.GEARMAN_JOB_DATA, data));
	}


	private final void workFail(final GearmanPacket packet) {
		final ByteArray jobHandle = new ByteArray(packet.getArgumentData(0));

		final BackendJobReturn jobReturn = this.jobs.remove(jobHandle);
		if(this.jobs.isEmpty()) {
			this.idleTimeout = System.currentTimeMillis();
		}

		if(jobReturn==null) {
			LOGGER.warn("Unexspected Packet : WORK_FAIL : "+ jobHandle.toString(GearmanUtils.getCharset()));
			return;
		}

		jobReturn.eof(GearmanJobEventImmutable.GEARMAN_JOB_FAIL);
	}

	private final void jobCreated(final GearmanPacket packet) {
		final ClientJobSubmission jobSub;

		synchronized(this.jobs) {
			jobSub = this.pendingJob;
			this.pendingJob = null;
		}

		BackendJobReturn jobReturn = jobSub.jobReturn;

		final byte[] jobHandle = packet.getArgumentData(0);

		if(jobSub.isBackground) {
			jobReturn.eof(new GearmanJobEventImpl(GearmanJobEventType.GEARMAN_SUBMIT_SUCCESS, jobHandle));
		} else {
			jobReturn.put(new GearmanJobEventImpl(GearmanJobEventType.GEARMAN_SUBMIT_SUCCESS, jobHandle));
			this.jobs.put(new ByteArray(jobHandle), jobReturn);
		}

		this.grab();
	}

	private final void workStatus(final GearmanPacket packet) {

		final ByteArray jobHandle = new ByteArray(packet.getArgumentData(0));
		final BackendJobReturn jobReturn = ClientConnectionController.this.jobs.get(jobHandle);

		if(jobReturn==null) {
			LOGGER.warn("Unexspected Packet : WORK_STATUS : "+ jobHandle.toString(GearmanUtils.getCharset()));
			return;
		}

		byte[] numerator = packet.getArgumentData(1);
		byte[] denominator = packet.getArgumentData(2);
		byte[] data = new byte[numerator.length + denominator.length + 1];

		for(int i=0; i<numerator.length; i++) {
			data[i] = numerator[i];
		}
		for(int i=0; i<denominator.length; i++) {
			data[i+1+numerator.length] = denominator[i];
		}

		jobReturn.put(new GearmanJobEventImpl(GearmanJobEventType.GEARMAN_JOB_STATUS, data));
	}

	private final void workComplete(final GearmanPacket packet) {
		final ByteArray jobHandle = new ByteArray(packet.getArgumentData(0));

		final BackendJobReturn jobReturn = this.jobs.remove(jobHandle);
		if(this.jobs.isEmpty()) {
			this.idleTimeout = System.currentTimeMillis();
		}

		if(jobReturn==null) {
			LOGGER.warn("Unexspected Packet : WORK_COMPLETE : "+ jobHandle.toString(GearmanUtils.getCharset()));
			return;
		}

		final byte[] data = packet.getArgumentData(1);
		jobReturn.eof(new GearmanJobEventImpl(GearmanJobEventType.GEARMAN_JOB_SUCCESS, data));
	}

	private final void error(final GearmanPacket packet) {
		final String errorCode = new String(packet.getArgumentData(0), GearmanUtils.getCharset());
		final String errorText = new String(packet.getArgumentData(1), GearmanUtils.getCharset());

		LOGGER.error("Recived Error Packet: " + errorText + "(" + errorCode + ")");
	}

	private final class InnerGearmanCallback implements GearmanCallbackHandler<GearmanPacket, SendCallbackResult>  {
		@Override
		public void onComplete(GearmanPacket data, SendCallbackResult result) {
			if(!result.isSuccessful()) {
				final ClientJobSubmission jobSub;

				synchronized(jobs) {
					jobSub = pendingJob;
					pendingJob = null;
				}

				// TODO log
				jobSub.jobReturn.eof(GearmanJobEventImmutable.GEARMAN_SUBMIT_FAIL_SEND_FAILED);

				grab();
			}
		}
	}
}
