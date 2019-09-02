package com.ifchange.tob.common.fastdfs;

import com.ifchange.tob.common.helper.BytesHelper;
import com.ifchange.tob.common.helper.EncryptHelper;
import com.ifchange.tob.common.helper.StringHelper;
import javafx.util.Pair;
import org.csource.common.NameValuePair;
import org.csource.fastdfs.DownloadCallback;
import org.csource.fastdfs.FileInfo;
import org.csource.fastdfs.ProtoCommon;
import org.csource.fastdfs.StorageClient;

import javax.annotation.PreDestroy;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

public class FastDfsOperations {
    private final StorageClientPool pool;

    FastDfsOperations(String uri) {
        this.pool = FastDfsHelper.storageClientPool(uri);
    }

    public String upload(String path) {
        return upload(path, null);
    }

    public String upload(String path, NameValuePair[] metas) {
        return upload(new File(path), metas);

    }

    public String upload(File file) {
        return upload(file, null);
    }

    public String upload(File file, NameValuePair[] metas) {
        if (file.exists()) {
            String path = file.getPath();
            if(file.isDirectory()) {
                throw new FastDfsException("Upload file=" + path + " is a directory...");
            } else {
                metas = mergeMetas(metas, new NameValuePair("original", path));
                String ext = path.substring(path.lastIndexOf(".") + 1);
                FileInputStream stream = null;
                try {
                    stream = new FileInputStream(file);
                    return upload(stream, ext, metas);
                } catch (FastDfsException e) {
                    throw new FastDfsException("Upload file=" + path + " to fastdfs error ", e.getCause());
                } catch (Exception e) {
                    throw new FastDfsException("Upload file=" + path + " to fastdfs error ", e);
                } finally {
                    BytesHelper.close(stream);
                }
            }
        } else {
            throw new FastDfsException("Upload file not exists...");
        }
    }

    public String upload(InputStream stream, String extN){
        return upload(stream, extN, null);
    }

    public String upload(InputStream stream, String extN, NameValuePair[] metas) {
        if (null == stream) {
            throw new FastDfsException("Upload file stream must not null...");
        }
        return upload(BytesHelper.toByteArray(stream), extN, metas);
    }

    public String upload(byte[] bytes, String extN) {
        return upload(bytes, extN, null);
    }

    public String upload(byte[] bytes, String extN, NameValuePair[] metas) {
        if (null == bytes || bytes.length < 1) {
            throw new FastDfsException("Upload file bytes must not null or empty...");
        }
        if (StringHelper.isBlank(extN)) {
            throw new FastDfsException("Upload file ext name must not null or empty...");
        }
        metas = mergeMetas(metas, new NameValuePair("md5", EncryptHelper.md5(bytes)));
        StorageClient client = null;
        try {
            client = pool.getResource();
            String[] rs = client.upload_file(bytes, extN, metas);
            return null == rs ? StringHelper.EMPTY : rs[0] + FastDfsHelper.SPLIT_GROUP_NAME_AND_FILENAME_SEPARATOR + rs[1];
        } catch (Exception e) {
            throw new FastDfsException("Upload file to fastdfs error...", e);
        } finally {
            pool.returnResource(client);
        }
    }

    public byte[] download(String fileId) {
        Pair<String, String> pair = FastDfsHelper.splitFileId(fileId);
        StorageClient client = null;
        try {
            client = pool.getResource();
            return client.download_file(pair.getKey(), pair.getValue());
        }catch (Exception e) {
            throw new FastDfsException("Download file by fileId=" + fileId + " error ", e);
        } finally {
            pool.returnResource(client);
        }
    }

    public InputStream read(String fileId) {
        return BytesHelper.toStream(download(fileId));
    }

    public void download(String fileId, DownloadCallback callback) {
        Pair<String, String> pair = FastDfsHelper.splitFileId(fileId);
        StorageClient client = null;
        try {
            client = pool.getResource();
            client.download_file(pair.getKey(), pair.getValue(), callback);
        } catch (Exception e) {
            throw new FastDfsException("Download file by fileId=" + fileId + " error ", e);
        } finally {
            pool.returnResource(client);
        }
    }

    public FileInfo fileInfoGet(String fileId) {
        Pair<String, String> pair = FastDfsHelper.splitFileId(fileId);
        StorageClient client = null;
        try {
            client = pool.getResource();
            return client.get_file_info(pair.getKey(), pair.getValue());
        } catch (Exception e) {
            throw new FastDfsException("Get file info by fileId=" + fileId + " error ", e);
        } finally {
            pool.returnResource(client);
        }
    }

    public NameValuePair[] metasGet(String fileId) {
        Pair<String, String> pair = FastDfsHelper.splitFileId(fileId);
        StorageClient client = null;
        try {
            client = pool.getResource();
            return client.get_metadata(pair.getKey(), pair.getValue());
        } catch (Exception e) {
            throw new FastDfsException("Get file metadata by fileId=" + fileId + " error ", e);
        } finally {
            pool.returnResource(client);
        }
    }

    public boolean metasSet (String fileId, NameValuePair[] metas, boolean merge) {
        Pair<String, String> pair = FastDfsHelper.splitFileId(fileId);
        StorageClient client = null;
        try {
            client = pool.getResource();
            byte flag = merge ? ProtoCommon.STORAGE_SET_METADATA_FLAG_MERGE : ProtoCommon.STORAGE_SET_METADATA_FLAG_OVERWRITE;
            return 0 == client.set_metadata(pair.getKey(), pair.getValue(), metas, flag);
        } catch (Exception e) {
            throw new FastDfsException("Set file metadata by fileId=" + fileId + " error ", e);
        } finally {
            pool.returnResource(client);
        }
    }

    public boolean delete(String fileId) {
        Pair<String, String> pair = FastDfsHelper.splitFileId(fileId);
        StorageClient client = null;
        try {
            client = pool.getResource();
            return 0 == client.delete_file(pair.getKey(), pair.getValue());
        } catch (Exception e) {
            throw new FastDfsException("Delete file by fileId=" + fileId + " error ", e);
        } finally {
            pool.returnResource(client);
        }
    }

    private NameValuePair[] mergeMetas(NameValuePair[] metas, NameValuePair meta) {
        if (null == metas || metas.length < 1) {
            return new NameValuePair[]{meta};
        } else {
            int assignLength = metas.length + 1;
            NameValuePair[] assign = new NameValuePair[assignLength];
            for (int idx = 0; idx < assignLength; idx++) {
                assign[idx] = idx == metas.length ? meta : metas[idx];
            }
            return assign;
        }
    }

    @PreDestroy @SuppressWarnings("unused")
    private void destroy() {
        if(null != pool) {
            pool.close();
        }
    }
}
