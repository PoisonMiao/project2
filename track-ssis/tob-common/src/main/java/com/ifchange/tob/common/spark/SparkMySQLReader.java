package com.ifchange.tob.common.spark;

import org.apache.spark.sql.DataFrameReader;
import org.apache.spark.sql.SparkSession;

public class SparkMySQLReader extends DataFrameReader {
    public SparkMySQLReader(SparkSession spark) {
        super(spark);
    }

    public SparkMySQLReader ofTable(String tableName) {
        super.option("dbtable", tableName);
        return this;
    }

    @Override
    public SparkMySQLReader format(String source) {
        super.format(source);
        return this;
    }

    @Override
    public SparkMySQLReader option(String key, String value) {
        super.option(key, value);
        return this;
    }
}
