package com.ifchange.tob.common.helper;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NetworkHelperTest {
    private static final Logger LOG = LoggerFactory.getLogger(NetworkHelperTest.class);
    @Test
    public void minLocalIp() {
        LOG.info(NetworkHelper.minLocalIp());
    }

    @Test
    public void maxLocalIp() {
        LOG.info(NetworkHelper.maxLocalIp());
    }

    @Test
    public void minInnerIp() {
        LOG.info(NetworkHelper.minInnerIp());
    }

    @Test
    public void maxInnerIp() {
        LOG.info(NetworkHelper.maxInnerIp());
    }

    @Test
    public void minOuterIp() {
        LOG.info(NetworkHelper.minOuterIp());
    }

    @Test
    public void maxOuterIp() {
        LOG.info(NetworkHelper.maxOuterIp());
    }

    @Test
    public void ip2long() {
    }

    @Test
    public void long2ip() {
    }

    @Test
    public void ofClientIp() {
    }

    @Test
    public void localHostName() {
    }

    @Test
    public void needReplyJson() {
    }

    @Test
    public void isJsonp() {
    }

    @Test
    public void dnsAIp() {
    }

    @Test
    public void dnsCName() {
    }

    @Test
    public void ipLocation() {
    }

    @Test
    public void phoneLocation() {
    }
}