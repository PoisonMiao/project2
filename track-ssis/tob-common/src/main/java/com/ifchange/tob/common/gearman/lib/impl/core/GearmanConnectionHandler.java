package com.ifchange.tob.common.gearman.lib.impl.core;

public interface GearmanConnectionHandler<X> {
	void onAccept(GearmanConnection<X> conn);
	void onPacketReceived(GearmanPacket packet, GearmanConnection<X> conn);
	void onDisconnect(GearmanConnection<X> conn);
}
