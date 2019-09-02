package com.ifchange.tob.common.gearman.lib.impl.worker;

import com.ifchange.tob.common.gearman.lib.Gearman;
import com.ifchange.tob.common.gearman.lib.GearmanConsumer;
import com.ifchange.tob.common.gearman.lib.GearmanFunction;
import com.ifchange.tob.common.gearman.lib.GearmanLostConnectionAction;
import com.ifchange.tob.common.gearman.lib.GearmanLostConnectionGrounds;
import com.ifchange.tob.common.gearman.lib.GearmanLostConnectionPolicy;
import com.ifchange.tob.common.gearman.lib.GearmanServer;
import com.ifchange.tob.common.gearman.lib.impl.pools.AbstractJobServerPool;
import com.ifchange.tob.common.gearman.lib.impl.pools.ControllerState;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class GearmanWorkerImpl extends AbstractJobServerPool<WorkerConnectionController> implements GearmanConsumer {

	// TODO Do something else. I don't like the heart-beat mechanism
	private static final long HEARTBEAT_PERIOD = 20000000000L; // TODO decouple property

	/**
	 * Periodically checks the state of the connections while jobs can be pulled
	 * @author isaiah
	 */
	private final class Heartbeat implements Runnable {

		@Override
		public void run() {
			final long time = System.currentTimeMillis();

			for(WorkerConnectionController cc : GearmanWorkerImpl.super.getConnections().values()) {
				switch(cc.getState()) {
				case CONNECTING:
					// If connecting, nothing to do until a connection is established
				case DROPPED:
					// If dropped, the controller id
				case WAITING:
					break;
				case OPEN:
					cc.timeoutCheck(time);
					break;
				case CLOSED:
					cc.openServer(false);
					break;
				default:
					assert false;
				}
			}
		}
	}

	private class InnerConnectionController extends WorkerConnectionController {
		private final class Reconnect implements Runnable {
			@Override
			public void run() {
				if(!GearmanWorkerImpl.this.funcMap.isEmpty()) {
					InnerConnectionController.super.openServer(false);
				}
			}
		}

		private Reconnect r;

		InnerConnectionController(GearmanServer key) {
			super(GearmanWorkerImpl.this, key);
		}

		@Override
		public void onOpen(ControllerState oldState) {
			if(GearmanWorkerImpl.this.funcMap.isEmpty()) {
				super.closeServer();
			} else {
				super.onOpen(oldState);
			}
		}

		@Override
		protected Dispatcher getDispatcher() {
			return GearmanWorkerImpl.this.dispatcher;
		}

		@Override
		protected GearmanWorkerImpl getWorker() {
			return GearmanWorkerImpl.this;
		}

		@Override
		public void onConnect(ControllerState oldState) {
			connections.incrementAndGet();
			super.getKey().createGearmanConnection(this, this);
		}

		@Override
		public void onLostConnection(GearmanLostConnectionPolicy policy, GearmanLostConnectionGrounds grounds) {
			GearmanServer server = this.getKey();
			if(server==null) {
				// TODO log error
			}

			if(server.isShutdown()) {
				return;
			} else {
				GearmanLostConnectionAction action;
				try {
					action = policy.lostConnection(server, grounds);
				} catch (Throwable t) {
					action = null;
				}

				if(action==null) {
					action = GearmanWorkerImpl.super.getDefaultPolicy().lostConnection(super.getKey(), grounds);
				}

				switch(action) {
				case DROP:
					super.dropServer();
					break;
				case RECONNECT:
					super.waitServer(r==null? (r=new Reconnect()): r);
					break;
				default:
					throw new IllegalStateException("Unknown Action: " + action);
				}
			}
		}

		@Override
		public void onDrop(ControllerState oldState) {
			// No cleanup required
		}

		@Override
		public void onNew() {
			if(!GearmanWorkerImpl.this.funcMap.isEmpty()) {
				super.openServer(false);
			}
		}

		@Override
		public void onClose(ControllerState oldState) {
			connections.decrementAndGet();
			super.onClose(oldState);
		}

		@Override
		public void onWait(ControllerState oldState) { }
	}

	private final class FunctionInfo {
		private final GearmanFunction function;

		public FunctionInfo(GearmanFunction function) {
			this.function = function;
		}
	}

	private final ConcurrentHashMap<String, FunctionInfo> funcMap = new ConcurrentHashMap<String, FunctionInfo>();

	private final Heartbeat heartbeat = new Heartbeat();

	private AtomicInteger connections = new AtomicInteger(0);
	private ScheduledFuture<?> future;
	private final Dispatcher dispatcher;

	private boolean isConnected() {
		return connections.get()>0;
	}



	public GearmanWorkerImpl(final Gearman gearman, int workers) {
		super(gearman, new GearmanLostConnectionPolicyImpl(), 60, TimeUnit.SECONDS);
		this.dispatcher = new Dispatcher(workers);
	}

	@Override
	protected WorkerConnectionController createController(GearmanServer key) {
		return new InnerConnectionController(key);
	}

	@Override
	public GearmanFunction subscribeFunction(String name, GearmanFunction function) {
		if(name==null || function==null) throw new IllegalArgumentException("null paramiter");

		final FunctionInfo newFunc = new FunctionInfo(function);

		synchronized(this.funcMap) {
			final FunctionInfo oldFunc = this.funcMap.put(name, newFunc);

			if(oldFunc!=null) return oldFunc.function;
			if(this.isConnected()) {
				for(WorkerConnectionController cc : GearmanWorkerImpl.super.getConnections().values())
					cc.canDo(name);

				if(this.future==null)this.future = super.getGearman().getScheduler().scheduleAtFixedRate(this.heartbeat, HEARTBEAT_PERIOD, HEARTBEAT_PERIOD, TimeUnit.NANOSECONDS);
				return null;
			}

			if(this.future==null)this.future = super.getGearman().getScheduler().scheduleAtFixedRate(this.heartbeat, HEARTBEAT_PERIOD, HEARTBEAT_PERIOD, TimeUnit.NANOSECONDS);

			for(WorkerConnectionController cc : GearmanWorkerImpl.super.getConnections().values()) {
				cc.openServer(false);
			}

			return null;
		}
	}

	@Override
	public GearmanFunction getFunction(String name) {
		final FunctionInfo info = this.funcMap.get(name);
		return info==null? null: info.function;
	}

	@Override
	public int getMaximumConcurrency() {
		return this.dispatcher.getMaxCount();
	}

	@Override
	public Set<String> getRegisteredFunctions() {
		return Collections.unmodifiableSet(this.funcMap.keySet());
	}

	@Override
	public boolean removeFunction(String functionName) {
		synchronized(this.funcMap) {
			final FunctionInfo info = this.funcMap.remove(functionName);
			if(info==null) return false;

			if(this.funcMap.isEmpty()) {
				if(this.future!=null) {
					future.cancel(false);
					future=null;
				}

				for(WorkerConnectionController cc : GearmanWorkerImpl.super.getConnections().values()) {
					cc.cantDo(functionName);
					cc.closeIfNotWorking();
				}
			} else {
				for(WorkerConnectionController cc : GearmanWorkerImpl.super.getConnections().values()) {
					cc.cantDo(functionName);
				}
			}
			return true;
		}
	}

	@Override
	public void setMaximumConcurrency(int maxConcurrentJobs) {
		this.dispatcher.setMaxCount(maxConcurrentJobs);
	}

	@Override
	public void removeAllServers() {

		synchronized(this.funcMap) {
			if(this.future!=null) {
				future.cancel(false);
				future = null;
			}
		}

		super.removeAllServers();
	}

	@Override
	public void shutdown() {
		super.shutdown();
		this.getGearman().onServiceShutdown(this);
	}

	@Override
	public void removeAllFunctions() {
		synchronized(this.funcMap) {
			this.funcMap.clear();
			if(this.future!=null) {
				future.cancel(false);
				future = null;
			}

			if(this.funcMap.isEmpty()) {
				if(this.future!=null) {
					future.cancel(false);
					future=null;
				}

				for(WorkerConnectionController cc : GearmanWorkerImpl.super.getConnections().values()) {
					cc.resetAbilities();
					cc.closeIfNotWorking();
				}
			} else {
				for(WorkerConnectionController cc : GearmanWorkerImpl.super.getConnections().values()) {
					cc.resetAbilities();
				}
			}
		}
	}

	@Override
	public void finalize() throws Throwable {
		super.finalize();
		this.shutdown();
	}
}
