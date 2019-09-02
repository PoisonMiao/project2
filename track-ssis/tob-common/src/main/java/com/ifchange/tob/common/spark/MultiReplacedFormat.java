package com.ifchange.tob.common.spark;

import com.ifchange.tob.common.helper.JvmOSHelper;
import com.ifchange.tob.common.helper.StringHelper;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.RecordWriter;
import org.apache.hadoop.mapred.lib.MultipleTextOutputFormat;
import org.apache.hadoop.util.Progressable;

import java.io.IOException;

public class MultiReplacedFormat<K, V> extends MultipleTextOutputFormat<K, V> {
    @Override
    protected RecordWriter<K, V> getBaseRecordWriter(FileSystem fs, JobConf job, String name, Progressable arg3) throws IOException {
        return super.getBaseRecordWriter(fs, job, name, arg3);
    }

    @Override
    public void checkOutputSpecs(FileSystem ignored, JobConf job) throws IOException {
        FileChecksHelper.checkOutputSpecs(job, kvSeparator());
    }

    @Override
    protected K generateActualKey(K key, V value) {
        return actualKey(key, value);
    }

    @Override
    protected String generateFileNameForKeyValue(K key, V value, String name) {
        return fileName(key, value, name);
    }

/****-*+**+*+*++*+*+++**+**++*+*+ 重写以下方法 *+*+***+*+*+**+*+*+*+*+*+*+*+**++++*+***/
    // KEY VALUE 输出时的分割符
    protected String kvSeparator() {
        return StringHelper.EMPTY;
    }
    // 输出的 KEY 值
    protected K actualKey(K key, V value) {
        return null;
    }
    // 输出的文件名
    protected String fileName(K key, V value, String name) {
        return StringHelper.join(JvmOSHelper.fileSeparator(), key, name);
    }
}
