package com.ifchange.tob.common.gearman.lib;

/**
 * A GearmanFunction provides the interface to create functions that can be executed
 * by the {@link GearmanConsumer}.
 *
 * @author isaiah
 */
public interface GearmanFunction {
    byte[] work(String function, byte[] data, GearmanFunctionCallback callback) throws Exception;
}
