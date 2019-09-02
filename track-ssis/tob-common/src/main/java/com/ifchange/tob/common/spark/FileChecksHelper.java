package com.ifchange.tob.common.spark;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapred.FileOutputFormat;
import org.apache.hadoop.mapred.InvalidJobConfException;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapreduce.security.TokenCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public abstract class FileChecksHelper {
    private static final Logger LOG = LoggerFactory.getLogger(FileChecksHelper.class);

    private FileChecksHelper(){}

    public static void checkOutputSpecs(JobConf job, String kvSeparator) throws IOException {
        job.set("mapreduce.output.textoutputformat.separator", kvSeparator);

        Path path = FileOutputFormat.getOutputPath(job);
        if (path == null && job.getNumReduceTasks() != 0) {
            throw new InvalidJobConfException("Output directory not set in JobConf.");
        }
        if (path != null) {
            FileSystem fs = path.getFileSystem(job);
            path = fs.makeQualified(path);
            FileOutputFormat.setOutputPath(job, path);
            TokenCache.obtainTokensForNamenodes(job.getCredentials(), new Path[] {path}, job);
            if (fs.exists(path)) {
                fs.delete(path, true);
                LOG.info("already delete exists output directory: {}, then replaced", path);
            }
        }
    }
}
