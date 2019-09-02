package com.ifchange.tob.common.cluster;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public interface IMessageHandler {
    Logger LOG = LoggerFactory.getLogger(IMessageHandler.class);
    default Object accept(RpcRequest request){
        throw new UnsupportedOperationException("you must implements your own MessageHandler.....");
    }
}
