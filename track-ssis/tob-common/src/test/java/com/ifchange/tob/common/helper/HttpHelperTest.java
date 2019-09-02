package com.ifchange.tob.common.helper;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

@Ignore
public class HttpHelperTest {
    private static final Logger LOG = LoggerFactory.getLogger(HttpHelperTest.class);

    @Test
    public void invokeHttpsByHttpClient() throws IOException {
        HttpClient client = HttpClients.createDefault();
        HttpPost httpPost = new HttpPost("https://iflytek.ifchange.com/atsng/thirdParty/getToken");
        HttpResponse response = client.execute(httpPost);
        System.out.println(EntityUtils.toString(response.getEntity(), "UTF-8"));
    }
}
