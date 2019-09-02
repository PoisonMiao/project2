package com.ifchange.tob.common.gearman.lib.impl.client;

import com.ifchange.tob.common.gearman.lib.GearmanLostConnectionAction;
import com.ifchange.tob.common.gearman.lib.GearmanLostConnectionGrounds;
import com.ifchange.tob.common.gearman.lib.GearmanLostConnectionPolicy;
import com.ifchange.tob.common.gearman.lib.GearmanServer;

class ClientLostConnectionPolicy implements GearmanLostConnectionPolicy {

	@Override
	public GearmanLostConnectionAction lostConnection(GearmanServer server, GearmanLostConnectionGrounds grounds) {
		return GearmanLostConnectionAction.RECONNECT;
	}

	@Override
	public void shutdownServer(GearmanServer server) {
	}

}
