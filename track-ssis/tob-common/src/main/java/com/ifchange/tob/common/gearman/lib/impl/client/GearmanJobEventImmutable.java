package com.ifchange.tob.common.gearman.lib.impl.client;

import com.ifchange.tob.common.gearman.lib.GearmanJobEvent;
import com.ifchange.tob.common.gearman.lib.GearmanJobEventType;
import com.ifchange.tob.common.gearman.lib.helpers.GearmanUtils;

class GearmanJobEventImmutable extends GearmanJobEventImpl {

	public static final GearmanJobEvent GEARMAN_EOF = new GearmanJobEventImmutable(GearmanJobEventType.GEARMAN_EOF, "EOF".getBytes(GearmanUtils.getCharset()));
	public static final GearmanJobEvent GEARMAN_SUBMIT_FAIL_CONNECTION_FAILED = new GearmanJobEventImmutable(GearmanJobEventType.GEARMAN_SUBMIT_FAIL, "Connection Failed".getBytes(GearmanUtils.getCharset()));
	public static final GearmanJobEvent GEARMAN_SUBMIT_FAIL_SERVER_NOT_AVAILABLE = new GearmanJobEventImmutable(GearmanJobEventType.GEARMAN_SUBMIT_FAIL, "Server Not Available".getBytes(GearmanUtils.getCharset()));
	public static final GearmanJobEvent GEARMAN_SUBMIT_FAIL_SERVICE_SHUTDOWN = new GearmanJobEventImmutable(GearmanJobEventType.GEARMAN_SUBMIT_FAIL, "Service Shutdown".getBytes(GearmanUtils.getCharset()));
	public static final GearmanJobEvent GEARMAN_SUBMIT_FAIL_SEND_FAILED = new GearmanJobEventImmutable(GearmanJobEventType.GEARMAN_SUBMIT_FAIL, "Failed to Send Job".getBytes(GearmanUtils.getCharset()));
	public static final GearmanJobEvent GEARMAN_JOB_DISCONNECT = new GearmanJobEventImmutable(GearmanJobEventType.GEARMAN_JOB_FAIL, "Server Disconnect".getBytes(GearmanUtils.getCharset()));
	public static final GearmanJobEvent GEARMAN_JOB_FAIL = new GearmanJobEventImmutable(GearmanJobEventType.GEARMAN_JOB_FAIL, "Failed By Worker".getBytes(GearmanUtils.getCharset()));

	private GearmanJobEventImmutable(GearmanJobEventType type, byte[] data) {
		super(type, data);
	}

	@Override
	public byte[] getData() {
		return super.getData().clone();
	}

}
