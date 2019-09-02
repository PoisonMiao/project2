package com.ifchange.tob.common.gearman.lib.impl.client;

import com.ifchange.tob.common.gearman.lib.GearmanJobEvent;

interface BackendJobReturn {
	void put(GearmanJobEvent event);
	void eof(GearmanJobEvent lastevent);
}
