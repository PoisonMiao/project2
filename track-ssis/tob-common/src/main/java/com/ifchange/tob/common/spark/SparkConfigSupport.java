package com.ifchange.tob.common.spark;

import com.ifchange.tob.common.helper.SpringHelper;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.sql.DataFrameReader;
import org.apache.spark.sql.SQLContext;
import org.apache.spark.sql.SparkSession;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

import javax.inject.Inject;
import java.util.Map;

public abstract class SparkConfigSupport {
    /** SPARK master **/
    protected abstract String master(Environment env);

    /** 添加 SPARK 多数据源 **/
    protected void addMultiDataFrameReader(SparkSession spark, Environment env, Map<String, DataFrameReader> dfrMap){}

    @Bean
    @Inject
    public SparkSession sparkSession(Environment env) {
        SparkSession.Builder builder = SparkSession.builder()
                                                   .master(master(env))
                                                   .config("spark.driver.host", "localhost")
                                                   .config("spark.sql.warehouse.dir", "/tmp/spark-wh")
                                                   .appName(SpringHelper.applicationName().toUpperCase());
        SparkSession spark = builder.getOrCreate();
        addMultiDataFrameReader(spark, env, SparkSqlFactory.DFR_MAP);
        return spark;
    }

    @Bean
    @Inject
    public JavaSparkContext javaSparkContext(SparkSession session) {
        return JavaSparkContext.fromSparkContext(session.sparkContext());
    }

    @Bean
    @Inject
    public SQLContext sqlContext(SparkSession session) {
        return session.sqlContext();
    }

    /** DS_URI=jdbc:mysql://{host:port}/{db}?user={username}&password={password} **/
    protected SparkMySQLReader mysqlDFR(SparkSession spark, String dsUri) {
        return new SparkMySQLReader(spark).format("jdbc").option("driver", "com.mysql.jdbc.Driver").option("url", dsUri);
    }
    /** NODES=host:port,host:port,host:port **/
    protected DataFrameReader elasticsearcDFR(SparkSession spark, String nodes) {
        return new DataFrameReader(spark).option("es.nodes", nodes)
                                         .option("pushdown", true)
                                         .option("es.nodes.wan.only",true)
                                         .option("es.index.auto.create", true)
                                         .format("org.elasticsearch.spark.sql");
    }
}
