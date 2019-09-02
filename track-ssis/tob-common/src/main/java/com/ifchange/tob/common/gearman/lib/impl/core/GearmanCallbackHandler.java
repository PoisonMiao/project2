package com.ifchange.tob.common.gearman.lib.impl.core;

public interface GearmanCallbackHandler<D,R extends GearmanCallbackResult> {
	void onComplete(D data, R result);
}
