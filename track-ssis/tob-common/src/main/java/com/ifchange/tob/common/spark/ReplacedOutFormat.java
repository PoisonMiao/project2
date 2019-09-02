package com.ifchange.tob.common.spark;

import com.ifchange.tob.common.helper.StringHelper;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.TextOutputFormat;

import java.io.IOException;

public class ReplacedOutFormat<K, V> extends TextOutputFormat<K, V> {
    @Override
    public void checkOutputSpecs(FileSystem ignored, JobConf job) throws IOException {
        FileChecksHelper.checkOutputSpecs(job, kvSeparator());
    }

/****-*+**+*+*++*+*+++**+**++*+*+ 重写以下方法 *+*+***+*+*+**+*+*+*+*+*+*+*+**++++*+***/
    protected String kvSeparator() {
        return StringHelper.EMPTY;
    }
}
