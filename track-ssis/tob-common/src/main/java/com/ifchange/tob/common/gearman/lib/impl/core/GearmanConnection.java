package com.ifchange.tob.common.gearman.lib.impl.core;

import java.io.IOException;

public interface GearmanConnection<X> {
	enum SendCallbackResult implements GearmanCallbackResult{
		SEND_SUCCESSFUL,
		SEND_FAILED,
		SERVICE_SHUTDOWN;

		@Override
		public boolean isSuccessful() {
			return this.equals(SEND_SUCCESSFUL);
		}
	}

	/**
	 * A user may want to attach an object to maintain the state of communication.
	 */
    void setAttachment(X att);

	/**
	 * Gets the attached object, if an object has been attached
	 * @return
	 * 		The attached object
	 */
    X getAttachment();

	void sendPacket(GearmanPacket packet, GearmanCallbackHandler<GearmanPacket, SendCallbackResult> callback);

	int getPort();
	String getHostAddress();
	boolean isClosed();
	void close() throws IOException;
}
