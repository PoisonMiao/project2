package com.ifchange.tob.common.view;

import com.ifchange.tob.common.core.RpcReply;
import com.ifchange.tob.common.helper.BytesHelper;
import com.ifchange.tob.common.helper.DateHelper;
import com.ifchange.tob.common.helper.ExceptionHelper;
import com.ifchange.tob.common.ibatis.DataSourceManager;
import com.ifchange.tob.common.support.IConstant;
import com.ifchange.tob.common.view.parser.RequestContext;
import com.ifchange.tob.common.view.parser.RequestSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Enumeration;
import java.util.zip.GZIPOutputStream;

/** URI Response 大于 2K 的做压缩处理 **/
public class Reply2kCompressionFilter implements Filter {
    private static final Logger LOG = LoggerFactory.getLogger(Reply2kCompressionFilter.class);

    private static final String CONTENT_ENCODING = "Content-Encoding";
    private static final String ACCEPT_ENCODING = "Accept-Encoding";
    private static final Long NEED_COM_SIZE = 2 * 1024L;
    private static final String GZIP = "gzip";

    static void writeResponse(HttpServletRequest request, HttpServletResponse response, byte[] bytes) throws IOException {
        ExceptionHelper.resetResponseError(response);
        response.setContentType(ExceptionHelper.contentType(request));
        ByteArrayOutputStream bos = null; GZIPOutputStream gzip = null;
        try {
            //如果 Client 支持 GZIP, 压缩
            if(needCompress(request, bytes)) {
                response.addHeader(CONTENT_ENCODING, GZIP);
                bos = new ByteArrayOutputStream();
                gzip = new GZIPOutputStream(bos);
                gzip.write(bytes);
                gzip.finish(); bos.flush();
                bytes = bos.toByteArray();
            }
            ExceptionHelper.setTimeSpentHeader(response);
            response.setContentLength(bytes.length);
            OutputStream os = response.getOutputStream();
            os.write(bytes); os.flush();
        } finally {
            // 处理请求响应日志
            new RequestResponseLogger(RequestContext.get().getSession()).submit();
            // 清理资源
            BytesHelper.close(gzip); BytesHelper.close(bos);
            RequestContext.get().clear(); DataSourceManager.get().clear(); RpcReply.Helper.get().clear();
        }
    }

    public void init(FilterConfig config) {
        LOG.debug("compression filter init......");
    }

    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest)req;
        if (IConstant.isSpringCloudEndpoint(request.getRequestURI())) {
            chain.doFilter(request, res);
            return;
        }
        ExceptionHelper.resetResponseError((HttpServletResponse)res);
        HttpServletResponse response = (HttpServletResponse) res;
        RequestContext.get().setSession(RequestSession.newborn(request));

        response.setHeader(IConstant.X_IN_TIME, (DateHelper.time() + ""));
        ResponseWrapper wrapper = new ResponseWrapper(response);
        chain.doFilter(req, wrapper); if(!response.isCommitted()) {
            wrapper.flushBuffer(); wrapper.finish();
            byte[] bytes = ExceptionHelper.hasError(wrapper) ? ExceptionHelper.body(request, wrapper): wrapper.body();
            writeResponse(request, response, bytes); wrapper.reset();
        }
    }

    public void destroy() {
        LOG.debug("compression filter destroy......");
    }

    /** 判断请求返回体是需要压缩 **/
    private static boolean needCompress(HttpServletRequest request, byte[] src) {
        return src.length >= NEED_COM_SIZE && supportGzip(request);
    }

    /** 判断请求是否支持 GZIP **/
    private static boolean supportGzip(HttpServletRequest request) {
        Enumeration headers = request.getHeaders(ACCEPT_ENCODING);
        while (headers.hasMoreElements()) {
            String value = (String) headers.nextElement();
            if (value.contains(GZIP)) {
                return true;
            }
        }
        return false;
    }

    private final class ResponseWrapper extends HttpServletResponseWrapper {
        private static final int OT_NONE = 0, OT_WRITER = 1, OT_STREAM = 2;
        private int outputType = OT_NONE;
        private ByteArrayOutputStream buffer;
        private ServletOutputStream output = null;
        private PrintWriter writer = null;

        ResponseWrapper(HttpServletResponse response) {
            super(response);
            buffer = new ByteArrayOutputStream();
        }

        @Override
        public PrintWriter getWriter() throws IOException {
            if (outputType == OT_STREAM)
                throw new IllegalStateException();
            else if (outputType == OT_WRITER)
                return writer;
            else {
                outputType = OT_WRITER;
                writer = new PrintWriter(new OutputStreamWriter(buffer, getCharacterEncoding()));
                return writer;
            }
        }

        @Override
        public ServletOutputStream getOutputStream() {
            if (outputType == OT_WRITER)
                throw new IllegalStateException();
            else if (outputType == OT_STREAM)
                return output;
            else {
                outputType = OT_STREAM;
                output = new WrappedOutputStream(buffer);
                return output;
            }
        }

        @Override
        public void flushBuffer() throws IOException {
            if (outputType == OT_WRITER)
                writer.flush();
            if (outputType == OT_STREAM)
                output.flush();
        }

        @Override
        public void reset() {
            outputType = OT_NONE;
            buffer.reset();
        }

        public byte[] body() throws IOException {
            flushBuffer();
            return null != buffer ? buffer.toByteArray() : new byte[0];
        }

        void finish() throws IOException {
            if (writer != null) {
                writer.close();
            }
            if (output != null) {
                output.close();
            }
        }

        class WrappedOutputStream extends ServletOutputStream {
            private ByteArrayOutputStream buffer;

            WrappedOutputStream(ByteArrayOutputStream buffer) {
                this.buffer = buffer;
            }

            public void write(int b) {
                buffer.write(b);
            }

            @Override public boolean isReady() {
                return false;
            }

            @Override public void setWriteListener(WriteListener writeListener) {}
        }
    }
}