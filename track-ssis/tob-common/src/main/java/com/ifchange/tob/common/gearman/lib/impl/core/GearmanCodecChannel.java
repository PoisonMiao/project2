package com.ifchange.tob.common.gearman.lib.impl.core;

import java.nio.ByteBuffer;

public interface GearmanCodecChannel<X> {
	ByteBuffer getBuffer();
	void setBuffer(ByteBuffer buffer);
	void setCodecAttachement(X att);
	X getCodecAttachement();
	void onDecode(GearmanPacket packet);
}
