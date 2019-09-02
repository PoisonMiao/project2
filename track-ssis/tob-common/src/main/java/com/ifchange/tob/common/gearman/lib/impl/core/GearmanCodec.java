package com.ifchange.tob.common.gearman.lib.impl.core;

import java.nio.ByteBuffer;

public interface GearmanCodec<X> {

	/**
	 * The init method is called to allow the user to initialize the {@link GearmanCodecChannel}
	 * before the decode method is called.
	 * @param channel
	 * 		The channel to initialize
	 */
    void init(GearmanCodecChannel<X> channel);
	ByteBuffer createByteBuffer();
	void decode(GearmanCodecChannel<X> channel, int byteCount);
	byte[] encode(GearmanPacket packet);
}
