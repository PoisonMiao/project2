package com.ifchange.tob.common.spark;

import com.google.common.collect.Maps;
import org.apache.spark.sql.DataFrameReader;

import java.util.Map;

public final class SparkSqlFactory {
    static final Map<String, DataFrameReader> DFR_MAP = Maps.newConcurrentMap();
    public static DataFrameReader get(String dsId) {
        return DFR_MAP.get(dsId);
    }

    public static SparkMySQLReader mysql(String dsId) {
        return (SparkMySQLReader)get(dsId);
    }

    public static DataFrameReader es(String dsId) {
        return get(dsId);
    }

    private SparkSqlFactory() {}
}
