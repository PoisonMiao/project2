package com.ifchange.tob.common.gearman;

import com.ifchange.tob.common.core.ICodeMSG;
import com.ifchange.tob.common.core.RpcException;
import com.ifchange.tob.common.core.RpcReply;
import com.ifchange.tob.common.gearman.lib.Gearman;
import com.ifchange.tob.common.gearman.lib.GearmanConsumer;
import com.ifchange.tob.common.gearman.lib.GearmanFunction;
import com.ifchange.tob.common.gearman.lib.GearmanFunctionCallback;
import com.ifchange.tob.common.helper.BytesHelper;
import com.ifchange.tob.common.helper.JsonHelper;
import com.ifchange.tob.common.support.CommonCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PreDestroy;

public abstract class GearmanWorker implements GearmanFunction {
    protected final Logger LOG;
    protected final Gearman gearman;

    protected abstract Object handle(byte[] data, GearmanFunctionCallback callback);

    public GearmanWorker(Gearman gearman) {
        this.gearman = gearman;
        LOG = LoggerFactory.getLogger(this.getClass());
    }

    public void start() {
        GearmanConsumer worker = gearman.createGearmanWorker();
        worker.subscribeFunction(gearman.gearmanName(), this);
        worker.registryServer(gearman.createGearmanServer());
    }

    @Override
    public byte[] work(String function, byte[] data, GearmanFunctionCallback callback) {
        Object response;
        try {
            response =  handle(data, callback);
        } catch (Exception e) {
            RpcException ex = (e instanceof RpcException) ? (RpcException)e : new RpcException(CommonCode.SvError, e);
            if (null != ex.cause) {
                LOG.error("Gearman work error ", ex.cause);
            }
            response = RpcReply.onFail(ICodeMSG.create(ex.code(), ex.msg()));
        }
        System.out.println("data=" + BytesHelper.string(data));
        String replay = (response instanceof byte[]) ? BytesHelper.string((byte[])response) : JsonHelper.toJSONString(response);
        System.out.println("response=" + replay);
        return (response instanceof byte[]) ? (byte[])response : JsonHelper.toJSONBytes(response);
    }

    @PreDestroy @SuppressWarnings("unused")
    private void destroy() {
        if (null != this.gearman && !this.gearman.isShutdown()) {
            this.gearman.shutdown();
        }
    }
}
