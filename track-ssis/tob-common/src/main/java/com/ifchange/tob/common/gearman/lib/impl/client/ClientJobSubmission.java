package com.ifchange.tob.common.gearman.lib.impl.client;

import com.ifchange.tob.common.gearman.lib.GearmanJobPriority;

class ClientJobSubmission {
	final String functionName;
	final byte[] data;
	final byte[] uniqueID;
	final BackendJobReturn jobReturn;
	final GearmanJobPriority priority;
	final boolean isBackground;

	public ClientJobSubmission(String functionName, byte[] data, byte[] uniqueID, BackendJobReturn jobReturn, GearmanJobPriority priority ,boolean isBackground) {
		this.functionName = functionName;
		this.data = data;
		this.uniqueID = uniqueID;
		this.jobReturn = jobReturn;
		this.priority = priority;
		this.isBackground = isBackground;
	}
}
