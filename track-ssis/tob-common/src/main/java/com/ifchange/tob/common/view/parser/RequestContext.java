package com.ifchange.tob.common.view.parser;

/** 请求认证的 ThreadLocal **/
public final class RequestContext {
    private static ThreadLocal<RequestContext> holder = ThreadLocal.withInitial(RequestContext::new);

    private RequestSession session;

    /** 获取 RequestContext **/
    public static RequestContext get(){
        return holder.get();
    }

    /** 获取HTTP认证 SESSION **/
    public RequestSession getSession() {
        return session;
    }

    /** 设置HTTP的认证 RequestSession **/
    public void setSession(RequestSession session) {
        this.session = session;
    }

    /** 每个请求完成清除 ThreadLocal 中的数据 **/
    public void clear() {
        holder.remove();
    }
}
