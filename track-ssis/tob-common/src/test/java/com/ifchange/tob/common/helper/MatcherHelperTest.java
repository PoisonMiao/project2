package com.ifchange.tob.common.helper;

import org.junit.Assert;
import org.junit.Test;

public class MatcherHelperTest {

    @Test
    public void isBase64() {
        Assert.assertEquals(false, MatcherHelper.isBase64("YWJjZGVmZ2g"));
        Assert.assertEquals(true, MatcherHelper.isBase64("YWJjZGVmZ2g="));
        Assert.assertEquals(false, MatcherHelper.isBase64("YWJjZGVmZ2g=="));
        Assert.assertEquals(false, MatcherHelper.isBase64("YWJjZGVmZ2g==="));

        Assert.assertEquals(true, MatcherHelper.isEmail("lee_silvia@126.com"));
        Assert.assertEquals(true, MatcherHelper.isEmail("s-ong.zhan_g@126.com.cn"));
        Assert.assertEquals(true, MatcherHelper.isEmail("s-ong.zhan_g@a.cn"));
        Assert.assertEquals(false, MatcherHelper.isEmail("s-ong.zhan_g@.cn"));
    }
}