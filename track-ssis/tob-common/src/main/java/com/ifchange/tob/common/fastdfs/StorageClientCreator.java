package com.ifchange.tob.common.fastdfs;

import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.csource.fastdfs.StorageClient;
import org.csource.fastdfs.StorageServer;
import org.csource.fastdfs.TrackerClient;
import org.csource.fastdfs.TrackerServer;

public class StorageClientCreator extends BasePooledObjectFactory<StorageClient> {
    private final TrackerClient tracker;
    private TrackerServer trackerServer;
    private StorageServer storageServer = null;

    public StorageClientCreator(String uri) {
        this.tracker = FastDfsHelper.trackerClient(uri);
        try {
            trackerServer = tracker.getConnection();
            if (null == trackerServer) {
                throw new FastDfsException("FastDFS tracker server getConnection fail, errno code: " + tracker.getErrorCode());
            }
        } catch (Exception e) {
            if(e instanceof FastDfsException) {
                throw (FastDfsException)e;
            } else {
                throw new FastDfsException("FastDFS init StorageClientCreator by uri=" + uri + " error...", e);
            }
        }
    }

    @Override
    public StorageClient create() {
        return new StorageClient(trackerServer, storageServer);
    }

    @Override
    public PooledObject<StorageClient> wrap(StorageClient client) {
        return new DefaultPooledObject<>(client);
    }
}
