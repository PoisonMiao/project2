package com.ifchange.tob.common.gearman.lib.impl.client;

import com.ifchange.tob.common.gearman.lib.GearmanJobEvent;
import com.ifchange.tob.common.gearman.lib.GearmanJobEventType;

public class GearmanJobEventImpl implements GearmanJobEvent {

	private final GearmanJobEventType type;
	private final byte[] data;

	public GearmanJobEventImpl(GearmanJobEventType type, byte[] data) {
		this.type = type;
		this.data = data;
	}

	@Override
	public GearmanJobEventType getEventType() {
		return type;
	}

	@Override
	public byte[] getData() {
		return data;
	}

	@Override
	public String toString() {
		return "GearmanJobEvent: " + type + " : " + new String(data);
	}

}
