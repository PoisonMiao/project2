package com.ifchange.tob.common.gearman.lib.impl.pools;

import com.ifchange.tob.common.gearman.lib.Gearman;
import com.ifchange.tob.common.gearman.lib.GearmanLostConnectionPolicy;
import com.ifchange.tob.common.gearman.lib.GearmanServer;
import com.ifchange.tob.common.gearman.lib.impl.core.GearmanPacket;
import com.ifchange.tob.common.gearman.lib.impl.server.ServerShutdownListener;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * A class used to manage multiple job server connections
 *
 * @author isaiah
 */
public abstract class AbstractJobServerPool <X extends AbstractConnectionController> implements GearmanServerPool, ServerShutdownListener {

	static final String DEFAULT_CLIENT_ID = "-";

	private final Gearman gearman;

	private final ConcurrentHashMap<GearmanServer, X> connMap = new ConcurrentHashMap<>();
	private final GearmanLostConnectionPolicy defaultPolicy;
	private GearmanLostConnectionPolicy policy;
    private long waitPeriod;
	private boolean isShutdown = false;
	private String id = AbstractJobServerPool.DEFAULT_CLIENT_ID;

	private final ReadWriteLock closeLock = new ReentrantReadWriteLock();

	protected AbstractJobServerPool(Gearman gearman, GearmanLostConnectionPolicy defaultPolicy, long waitPeriod, TimeUnit unit) {
		this.defaultPolicy = defaultPolicy;
		this.policy = defaultPolicy;
		this.waitPeriod = unit.toNanos(waitPeriod);
		this.gearman = gearman;
	}

	public boolean registryServer(GearmanServer server) {
		GearmanServer key = server;
		try {
			closeLock.readLock().lock();

			if(this.isShutdown) throw new IllegalStateException("In Shutdown State");

			X x = this.createController(key);

			if(this.connMap.putIfAbsent(key, x)==null) {
				x.onNew();
				return true;
			} else {
				return false;
			}
		} finally {
			closeLock.readLock().unlock();
		}
	}

	@Override
	public Gearman getGearman() {
		return this.gearman;
	}

	@Override
	public String getClientID() {
		return this.id;
	}

	@Override
	public long getReconnectPeriod(TimeUnit unit) {
		return unit.convert(this.waitPeriod,TimeUnit.NANOSECONDS);
	}

	@Override
	public int getServerCount() {
		return this.connMap.size();
	}

	@Override
	public boolean hasServer(GearmanServer srvr) {
		return this.connMap.containsKey(srvr);
	}

	@Override
	public void removeAllServers() {
		removeAllServers(false);
	}

	private void removeAllServers(boolean isOnShutdown) {
		List<GearmanServer> srvrs = new ArrayList<GearmanServer>(this.connMap.keySet());
		for(GearmanServer srvr : srvrs) {
			this.removeServer(srvr, isOnShutdown);
		}
	}

	@Override
	public boolean removeServer(GearmanServer srvr) {
		return removeServer(srvr, false);
	}

	boolean removeServer(GearmanServer srvr, boolean isOnShutdown) {
		try {
			closeLock.readLock().lock();

			if(this.isShutdown && !isOnShutdown)
				throw new IllegalStateException("In Shutdown State");

			X x = this.connMap.remove(srvr);
			if(x!=null) {
				x.dropServer();
				return true;
			} else {
				return false;
			}
		} finally {
			closeLock.readLock().unlock();
		}
	}

	@Override
	public void setClientID(final String id) {
		try {
			closeLock.readLock().lock();

			if(this.isShutdown) throw new IllegalStateException("In Shutdown State");
			if(this.id.equals(id)) return;

			this.id = id;

			for(final Map.Entry<GearmanServer, X> entry : this.connMap.entrySet()) {
				entry.getValue().sendPacket(GearmanPacket.createSET_CLIENT_ID(id), (data, result) -> {
					if(result.isSuccessful()) return;
				});
			}
		} finally {
			closeLock.readLock().unlock();
		}
	}

	@Override
	public void setLostConnectionPolicy(GearmanLostConnectionPolicy policy) {
		try {
			closeLock.readLock().lock();

			if(this.isShutdown) throw new IllegalStateException("In Shutdown State");

			if(this.policy==null)
				this.policy = this.defaultPolicy;
			else
				this.policy = policy;
		} finally {
			closeLock.readLock().unlock();
		}
	}

	@Override
	public void setReconnectPeriod(long time, TimeUnit unit) {
		try {
			closeLock.readLock().lock();

			if(this.isShutdown) throw new IllegalStateException("In Shutdown State");
			this.waitPeriod = unit.toNanos(time);
		} finally {
			closeLock.readLock().unlock();
		}
	}

	@Override
	public boolean isShutdown() {
		return this.isShutdown;
	}

	@Override
	public synchronized void shutdown() {
		try {
			closeLock.writeLock().lock();
			if(this.isShutdown) return;
			this.isShutdown = true;

			this.removeAllServers(true);
		} finally {
			closeLock.writeLock().unlock();
		}
	}

	protected Map<GearmanServer, X> getConnections() {
		return Collections.unmodifiableMap(this.connMap);
	}

	protected GearmanLostConnectionPolicy getDefaultPolicy() {
		return this.defaultPolicy;
	}

	protected GearmanLostConnectionPolicy getPolicy() {
		return this.policy;
	}

	@Override
	public Collection<GearmanServer> getServers() {
		Collection<GearmanServer> value = new ArrayList<>(this.connMap.keySet());
		return value;
	}

	@Override
	public void onShutdown(GearmanServer server) {
		// from the shutdown listener interface

		this.removeServer(server);
		this.policy.shutdownServer(server);
	}

	/**
	 * Creates a new ConnectionControler to add to the JobServerPool<br>
	 * Note: The returned value is not guaranteed to be added to the set
	 * of connections.
	 * @param key
	 * 		The ConnectionControler's key
	 * @return
	 *
	 */
	protected abstract X createController(GearmanServer key);
}
