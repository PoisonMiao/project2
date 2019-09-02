package com.ifchange.tob.common.gearman.lib.impl.pools;

import com.ifchange.tob.common.gearman.lib.GearmanJobStatus;
import com.ifchange.tob.common.gearman.lib.GearmanLostConnectionGrounds;
import com.ifchange.tob.common.gearman.lib.GearmanLostConnectionPolicy;
import com.ifchange.tob.common.gearman.lib.helpers.ByteArray;
import com.ifchange.tob.common.gearman.lib.helpers.TaskJoin;
import com.ifchange.tob.common.gearman.lib.impl.core.GearmanCallbackHandler;
import com.ifchange.tob.common.gearman.lib.impl.core.GearmanConnection.SendCallbackResult;
import com.ifchange.tob.common.gearman.lib.impl.core.GearmanPacket;

import java.util.concurrent.TimeUnit;

public interface ConnectionController {
	ControllerState getControllerState();
	TaskJoin<GearmanJobStatus> getStatus(final ByteArray jobHandle);
	void dropServer();
	void closeServer();
	void waitServer(Runnable callback);
	void waitServer(Runnable callback, long waittime, final TimeUnit unit);
	boolean openServer(final boolean force);
	boolean sendPacket(GearmanPacket packet, GearmanCallbackHandler<GearmanPacket, SendCallbackResult> callback);

	void onConnect(ControllerState oldState);
	void onOpen(ControllerState oldState);
	void onClose(ControllerState oldState);
	void onDrop(ControllerState oldState);
	void onWait(ControllerState oldState);
	void onNew();
	void onLostConnection(GearmanLostConnectionPolicy policy, GearmanLostConnectionGrounds grounds);
}
