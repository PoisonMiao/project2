package com.ifchange.tob.common.cluster;

import com.ifchange.tob.common.core.ICodeMSG;
import com.ifchange.tob.common.core.RemoteReply;
import com.ifchange.tob.common.helper.JsonHelper;
import com.ifchange.tob.common.support.CommonCode;

public class RpcResponse<R> extends RemoteReply<R> {
    /** 服务实例 **/
    public final SrvInstance instance;

    RpcResponse(SrvInstance instance, Class<R> rt, int code, String body) {
        super(rt, code, body);
        this.instance = instance;
    }

    public boolean success() {
        // 自定义 code
        return CommonCode.SuccessOk.code() == code;
    }

    public ICodeMSG icm() {
        return ICodeMSG.create(code, "[" + instance.name + "](RPC)" + body);
    }

    static Value val(int code, Object data) {
        return new Value(code, data instanceof String ? (String)data : JsonHelper.toJSONString(data));
    }

    public static class Value {
        public int code;
        public String data;
        @SuppressWarnings("unused")
        public Value() {}
        private Value(int code, String data) {
            this.code = code;
            this.data = data;
        }
    }
}
