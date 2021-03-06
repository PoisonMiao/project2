/*
 * Copyright (c) 2012, Isaiah van der Elst (isaiah.v@comcast.net)
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * - Redistributions of source code must retain the above copyright notice,
 *   this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package com.ifchange.tob.common.gearman.lib.impl.server;

import com.ifchange.tob.common.gearman.lib.Gearman;
import com.ifchange.tob.common.gearman.lib.GearmanServer;
import com.ifchange.tob.common.gearman.lib.impl.core.GearmanCallbackHandler;
import com.ifchange.tob.common.gearman.lib.impl.core.GearmanConnection;
import com.ifchange.tob.common.gearman.lib.impl.core.GearmanConnectionHandler;
import com.ifchange.tob.common.gearman.lib.impl.core.GearmanConnectionManager.ConnectCallbackResult;
import com.ifchange.tob.common.gearman.lib.impl.core.GearmanPacket;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static com.ifchange.tob.common.gearman.lib.helpers.GearmanUtils.LOGGER;

/**
 * A object representing a remote gearman server
 * @author isaiah
 */
public class GearmanServerRemote implements GearmanServer {

	/** The id for this remote server */
	private final String id;

	/** The address to the remote server */
	private final InetSocketAddress address;

	/** The gearman service that created this GearmanServerRemote object*/
	private final Gearman gearman;

	/** The internal lock used to synchronize events */
	private final ReadWriteLock lock = new ReentrantReadWriteLock();

	/** The connections created by this GearmanServerRemote */
	private final Set<GearmanConnection<?>> connections = new HashSet<>();

	/** A variable indicating if this GearmanServerRemote is shutdown */
	private boolean isShutdown = false;

	private final Set<ServerShutdownListener> listeners = new HashSet<>();

	/**
	 * Constructor
	 */
	public GearmanServerRemote(Gearman gearman, InetSocketAddress address) {
		this.gearman = gearman;
		this.address = address;
		this.id = "remote: "+ address.toString();
	}

	@Override
	public void shutdown() {
		try {
			this.lock.writeLock().lock();
			this.isShutdown = true;
		} finally {
			this.lock.writeLock().unlock();
		}
		for(GearmanConnection<?> conn : this.connections) {
			try {
				conn.close();
			} catch (IOException e) {
				LOGGER.error("failed to close gearman connection", e);
			}
		}
		for(ServerShutdownListener l : listeners) {
			l.onShutdown(this);
		}
		this.getGearman().onServiceShutdown(this);
	}

	@Override
	public boolean isShutdown() {
		return this.isShutdown;
	}

	@Override
	public Gearman getGearman() {
		return this.gearman;
	}

	@Override
	public String toString() {
		return this.id;
	}

	@Override
	public int hashCode() {
		return this.id.hashCode();
	}

	@Override
	public boolean equals(Object o) {
		if(!(o instanceof GearmanServerRemote))
			return false;
		return this.toString().equals(o.toString());
	}

	@Override
	public <A> void createGearmanConnection(GearmanConnectionHandler<A> handler, GearmanCallbackHandler<GearmanServer, ConnectCallbackResult> failCallback) {
		boolean isShutdown;
		try {
			lock.readLock().lock();
			if(!(isShutdown=this.isShutdown())) {
				final InnerGearmanConnectionHandler<A> innerHandler = new InnerGearmanConnectionHandler<>(handler, failCallback);
				gearman.getGearmanConnectionManager().createGearmanConnection(address, innerHandler, innerHandler);
			}
		} finally {
			lock.readLock().unlock();
		}

		if(isShutdown) failCallback.onComplete(this, ConnectCallbackResult.SERVICE_SHUTDOWN);
	}

	@Override
	public void finalize() {
		this.shutdown();
	}

	/**
	 * A GearmanConnectionHandler wrapper class that adds and removes connections to the connection set
	 * @author isaiah
	 */
	private final class InnerGearmanConnectionHandler<A> implements GearmanConnectionHandler<A>, GearmanCallbackHandler<InetSocketAddress, ConnectCallbackResult> {

		private final GearmanConnectionHandler<A> handler;
		private final GearmanCallbackHandler<GearmanServer, ConnectCallbackResult> failCallback;

		private InnerGearmanConnectionHandler(GearmanConnectionHandler<A> handler, GearmanCallbackHandler<GearmanServer, ConnectCallbackResult> failCallback) {
			this.handler = handler;
			this.failCallback = failCallback;
		}

		@Override
		public void onAccept(GearmanConnection<A> conn) {
			try {
				lock.readLock().lock();
				if(isShutdown()) {
					try {
						conn.close();
					} catch (IOException e) {
						failCallback.onComplete(GearmanServerRemote.this, ConnectCallbackResult.SERVICE_SHUTDOWN);
						LOGGER.error("failed to close gearman connection", e);
					}
					return;
				}
			} finally {
				lock.readLock().unlock();
			}

			connections.add(conn);
			handler.onAccept(conn);
		}

		@Override
		public void onPacketReceived(GearmanPacket packet, GearmanConnection<A> conn) {
			handler.onPacketReceived(packet, conn);
		}

		@Override
		public void onDisconnect(GearmanConnection<A> conn) {
			connections.remove(conn);
			handler.onDisconnect(conn);
		}

		@Override
		public void onComplete(InetSocketAddress data, ConnectCallbackResult result) {
			this.failCallback.onComplete(GearmanServerRemote.this, result);
		}
	}

	@Override
	public void addShutdownListener(ServerShutdownListener listener) {
		synchronized(this.listeners) {
			this.listeners.add(listener);
		}
	}

	@Override
	public void removeShutdownListener(ServerShutdownListener listener) {
		synchronized(this.listeners) {
			this.listeners.remove(listener);
		}
	}

}
