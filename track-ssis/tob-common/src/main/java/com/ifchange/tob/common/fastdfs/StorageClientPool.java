package com.ifchange.tob.common.fastdfs;

import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.csource.fastdfs.StorageClient;

public class StorageClientPool extends GenericObjectPool<StorageClient> {
    public StorageClientPool(StorageClientCreator creator, GenericObjectPoolConfig<StorageClient> config) {
        super(creator, config);
    }

    public StorageClient getResource() {
        try {
            return super.borrowObject();
        } catch (Exception e) {
            throw new FastDfsException("Could not get storage resource from the pool", e);
        }
    }

    public void returnResource(final StorageClient client) {
        if (null != client) {
            super.returnObject(client);
        }
    }
}
