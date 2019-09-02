package com.ifchange.tob.common.core;

import com.ifchange.tob.common.helper.CollectsHelper;
import com.ifchange.tob.common.helper.StringHelper;
import com.ifchange.tob.common.support.CommonCode;

import java.util.ArrayList;
import java.util.List;

/** 异常信息 **/
public class RpcException extends RuntimeException {
    private final ICodeMSG icm;
    private final List<String> packs;

    public final Throwable cause;

    public RpcException(ICodeMSG icm) {
        super(icm.message());
        this.icm = icm;
        this.cause = null;
        this.packs = new ArrayList<>();
    }

    public RpcException(ICodeMSG icm, List<String> packs) {
        super(icmOfMsg(icm, packs));
        this.icm = icm;
        this.cause = null;
        this.packs = packs;
    }

    public RpcException(ICodeMSG icm, Throwable cause) {
        super(icm.message(), cause);
        this.icm = icm;
        this.cause = cause;
        this.packs = new ArrayList<>();
    }

    public RpcException(ICodeMSG icm, Throwable cause, List<String> packs) {
        super(icmOfMsg(icm, packs));
        this.icm = icm;
        this.cause = cause;
        this.packs = packs;
    }

    public int code () {
        return null == icm ? CommonCode.SuccessOk.code() : icm.code();
    }

    public String msg() {
        return RpcException.icmOfMsg(icm, packs);
    }

    private static String icmOfMsg(ICodeMSG icm, List<String> packs) {
        if(null == icm) {
            return StringHelper.EMPTY;
        }
        if(CollectsHelper.isNullOrEmpty(packs)) {
            return icm.msg();
        }
        return String.format(icm.msg(), packs.toArray());
    }
}