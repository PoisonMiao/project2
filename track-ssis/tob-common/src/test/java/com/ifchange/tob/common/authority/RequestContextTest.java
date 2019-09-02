package com.ifchange.tob.common.authority;

import com.ifchange.tob.common.view.parser.RequestContext;
import com.ifchange.tob.common.view.parser.RequestSession;
import org.junit.Assert;
import org.junit.Test;

public class RequestContextTest {
    @Test
    public void get() {
        Assert.assertNotNull(RequestContext.get());
        Assert.assertTrue(RequestContext.get() instanceof RequestContext);
    }

    @Test
    public void getAuthoritySession() {
        RequestSession session = new RequestSession();
        session.tid = "000001";
        RequestContext.get().setSession(session);

        Assert.assertEquals(session, RequestContext.get().getSession());
        Assert.assertEquals(session.tid, RequestContext.get().getSession().tid);
    }


    @Test
    public void setAuthoritySession() {
        RequestSession session = new RequestSession();
        RequestContext.get().setSession(session);
        Assert.assertNotNull(RequestContext.get().getSession());
        Assert.assertEquals(session, RequestContext.get().getSession());
    }

    @Test
    public void clear() {
        RequestContext.get().setSession(new RequestSession());
        Assert.assertNotNull(RequestContext.get().getSession());
        RequestContext.get().clear();
        Assert.assertNull(RequestContext.get().getSession());
    }
}