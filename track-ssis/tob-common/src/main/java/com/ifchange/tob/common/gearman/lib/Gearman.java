package com.ifchange.tob.common.gearman.lib;

import com.google.common.net.HostAndPort;
import com.ifchange.tob.common.gearman.lib.helpers.GearmanThreads;
import com.ifchange.tob.common.gearman.lib.helpers.Scheduler;
import com.ifchange.tob.common.gearman.lib.impl.client.ProducerImpl;
import com.ifchange.tob.common.gearman.lib.impl.core.GearmanConnectionManager;
import com.ifchange.tob.common.gearman.lib.impl.server.GearmanServerRemote;
import com.ifchange.tob.common.gearman.lib.impl.worker.GearmanWorkerImpl;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class Gearman implements GearmanService {
	private static final int MAX_POOL = 65535;
	private final ReadWriteLock lock = new ReentrantReadWriteLock();
	private final Set<GearmanService> serviceSet = Collections.synchronizedSet(new HashSet<>());
	private final int workers;
	private final String name;
	private final HostAndPort server;
	private final Scheduler scheduler;
	private final GearmanConnectionManager connectionManager;
	public Gearman(String name, HostAndPort server, int threads, int timeout, int workers) throws IOException {
		this.name = name;
		this.server = server;
		this.workers = workers < 1 ? 1 : workers;
		final int care = threads < 1 ? 1 : threads;
		final ThreadFactory factory = new GearmanThreads();
		final long threadTimeout = (timeout < 1 ? 1 : timeout) * 1000L;
		final ThreadPoolExecutor pool = new ThreadPoolExecutor(care, MAX_POOL, threadTimeout, TimeUnit.MILLISECONDS, new SynchronousQueue<>(), factory);
		pool.allowCoreThreadTimeOut(false); pool.prestartCoreThread();
		this.scheduler = new Scheduler(pool, factory);
		this.connectionManager = new GearmanConnectionManager(scheduler);
	}

	private boolean isShutdown = false;
	@Override
	public void shutdown() {
		try {
			lock.writeLock().lock();
			this.isShutdown = true;
			for(GearmanService service : this.serviceSet) {
				service.shutdown();
			}
			this.serviceSet.clear();
			this.connectionManager.shutdown();
		} finally {
			lock.writeLock().unlock();
		}
	}

	@Override
	public boolean isShutdown() {
		return this.isShutdown;
	}

	@Override
	public Gearman getGearman() {
		return this;
	}

	public GearmanServer createGearmanServer() {
		InetSocketAddress address = new InetSocketAddress(server.getHost(), server.getPort());

		lock.readLock().lock();
		try {
			if(this.isShutdown()) {
				throw new IllegalStateException("Shutdown Service");
			}
			final GearmanServer server = new GearmanServerRemote(this, address);
			this.serviceSet.add(server);
			return server;
		} finally {
			lock.readLock().unlock();
		}
	}

	public String gearmanName(){
		return name;
	}

	public GearmanConsumer createGearmanWorker() {
		lock.readLock().lock();
		try {
			if(this.isShutdown()) {
				throw new IllegalStateException("Shutdown Service");
			}
			final GearmanConsumer worker = new GearmanWorkerImpl(this, workers);
			this.serviceSet.add(worker);
			return worker;
		} finally {
			lock.readLock().unlock();
		}
	}

	public GearmanProducer createGearmanClient() {
		lock.readLock().lock();
		try {
			if(this.isShutdown()) {
				throw new IllegalStateException("Shutdown Service");
			}

			final GearmanProducer client = new ProducerImpl(this);
			this.serviceSet.add(client);

			return client;
		} finally {
			lock.readLock().unlock();
		}
	}

	public Scheduler getScheduler() {
		return this.scheduler;
	}

	public final GearmanConnectionManager getGearmanConnectionManager() {
		return this.connectionManager;
	}

	public void onServiceShutdown(GearmanService service) {
		lock.readLock().lock();
		try {
			if(this.isShutdown()) return;
			this.serviceSet.remove(service);
		} finally {
			lock.readLock().unlock();
		}
	}
}
