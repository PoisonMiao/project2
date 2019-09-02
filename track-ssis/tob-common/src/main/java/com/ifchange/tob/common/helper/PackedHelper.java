package com.ifchange.tob.common.helper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public final class PackedHelper {
    private PackedHelper() {
    }

    /** GZIP 压缩 **/
    public static String gzip(String src) {
        if (StringHelper.isBlank(src)) {
            return StringHelper.EMPTY;
        }
        GZIPOutputStream gzip = null;
        ByteArrayOutputStream out = null;
        try {
            out = new ByteArrayOutputStream();
            gzip = new GZIPOutputStream(out);
            gzip.write(BytesHelper.utf8Bytes(src));
            gzip.finish(); out.flush();
            byte[] bytes = out.toByteArray();
            return EncryptHelper.encode64(bytes);
        } catch (IOException e) {
            throw new RuntimeException("Packed string using gzip error...", e);
        } finally {
            BytesHelper.close(gzip);
            BytesHelper.close(out);
        }
    }
    /** GZIP 解压缩 **/
    public static String unGzip(String compressed) {
        if (StringHelper.isBlank(compressed)) {
            return StringHelper.EMPTY;
        }
        InputStream is = EncryptHelper.decode64Stream(compressed);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        GZIPInputStream in = null;
        try {
            in = new GZIPInputStream(is);
            BytesHelper.copy(in, out);
            return out.toString(BytesHelper.UTF8.name());
        } catch (IOException e) {
            throw new RuntimeException("Unpacked gzip string error...", e);
        } finally {
            BytesHelper.close(is);
            BytesHelper.close(in);
            BytesHelper.close(out);
        }
    }
}
