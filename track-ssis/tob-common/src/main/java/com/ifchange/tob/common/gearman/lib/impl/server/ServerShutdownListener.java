package com.ifchange.tob.common.gearman.lib.impl.server;

import com.ifchange.tob.common.gearman.lib.GearmanServer;

public interface ServerShutdownListener {
	void onShutdown(GearmanServer server);
}
