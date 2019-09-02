package com.ifchange.tob.common.gearman.lib.impl.pools;

import com.ifchange.tob.common.gearman.lib.impl.core.GearmanCallbackHandler;
import com.ifchange.tob.common.gearman.lib.impl.core.GearmanConnection.SendCallbackResult;
import com.ifchange.tob.common.gearman.lib.impl.core.GearmanPacket;

import static com.ifchange.tob.common.gearman.lib.helpers.GearmanUtils.LOGGER;

class SendCallback implements GearmanCallbackHandler<GearmanPacket, SendCallbackResult> {

	private final GearmanCallbackHandler<GearmanPacket, SendCallbackResult> callback;

	SendCallback(GearmanCallbackHandler<GearmanPacket, SendCallbackResult> callback) {
		this.callback = callback;
	}

	@Override
	public void onComplete(GearmanPacket data, SendCallbackResult result) {
		if(!result.isSuccessful()) {
			LOGGER.warn("FAILED TO SEND PACKET : " + data.getPacketType().toString());
		}

		if(callback!=null)
			callback.onComplete(data, result);
	}
}
