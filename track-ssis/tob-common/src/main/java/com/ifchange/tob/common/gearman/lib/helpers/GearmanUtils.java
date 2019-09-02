package com.ifchange.tob.common.gearman.lib.helpers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.Charset;
import java.util.UUID;

public final class GearmanUtils {
	public static final Logger LOGGER = LoggerFactory.getLogger("gearman");
	private static final Charset charset = Charset.forName("UTF-8");
	private GearmanUtils() {
	}

	public static final byte[] createUID() {
		return UUID.randomUUID().toString().getBytes();
	}

	public static final Charset getCharset() {
		return charset;
	}
}
