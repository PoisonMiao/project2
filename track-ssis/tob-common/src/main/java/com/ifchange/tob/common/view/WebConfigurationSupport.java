package com.ifchange.tob.common.view;

import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.ifchange.tob.common.core.RpcException;
import com.ifchange.tob.common.core.RpcQuery;
import com.ifchange.tob.common.core.RpcReply;
import com.ifchange.tob.common.helper.BytesHelper;
import com.ifchange.tob.common.helper.DateHelper;
import com.ifchange.tob.common.helper.EncryptHelper;
import com.ifchange.tob.common.helper.ExceptionHelper;
import com.ifchange.tob.common.helper.JsonHelper;
import com.ifchange.tob.common.helper.SpringHelper;
import com.ifchange.tob.common.helper.StringHelper;
import com.ifchange.tob.common.support.CommonCode;
import com.ifchange.tob.common.support.IConstant;
import com.ifchange.tob.common.view.parser.ApiOperation;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.MultipartConfigFactory;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;

import javax.inject.Inject;
import javax.servlet.DispatcherType;
import javax.servlet.Filter;
import javax.servlet.MultipartConfigElement;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

public abstract class WebConfigurationSupport extends WebMvcConfigurationSupport {
    /** you can implements your own AuthenticationFilter **/
    @SuppressWarnings("WeakerAccess")
    protected IWebAuthenticationFilter injectAuthenticationFilter(@SuppressWarnings("unused") ApplicationContext context) {
        return new IWebAuthenticationFilter(){};
    }

    /** you can implements your own size (KB, MB ) **/
    protected String multipartMaxSize() {
        return "10MB";
    }

    /** 配置 HttpMessageConverters **/
    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        converters.addAll(Lists.newArrayList(FastJsonMessageConverter.INSTANCE,
                                             FormMessageConverter.INSTANCE,
                                             StringMessageConverter.INSTANCE
        ));
    }

    /** 静态资源配置 **/
    @Override
    protected void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/statics/**").addResourceLocations("classpath:/statics/");
    }

    /** HTTP 参数注入, ALL MAP args can not resolver **/
    @Override
    protected void addArgumentResolvers(List<HandlerMethodArgumentResolver> argResolvers) {
        argResolvers.add(new RequestClientIpResolver());
        argResolvers.add(new RequestQueriesResolver());
        argResolvers.add(new RequestSessionResolver());
    }

    /** 异步 Controller 支持 **/
    @Override
    public void configureAsyncSupport(AsyncSupportConfigurer configurer) {
        configurer.setDefaultTimeout(DateHelper.MINUTE_TIME);
    }

    /****/
    @Bean
    public MultipartConfigElement multipartConfigElement() {
        MultipartConfigFactory factory = new MultipartConfigFactory();
        String max = multipartMaxSize();
        factory.setMaxFileSize(max);
        factory.setMaxRequestSize(max);
        return factory.createMultipartConfig();
    }

    /** HTTP请求安全认证 **/
    @Bean
    @Inject
    public FilterRegistrationBean authenticationFilter(ApplicationContext context) {
        return createFilterRegistrationBean(injectAuthenticationFilter(context), (Integer.MIN_VALUE +1));
    }

    /** 请求返回 Response > 2k 压缩处理 **/
    @Bean
    public FilterRegistrationBean reply2KCompressionFilter(){
        return createFilterRegistrationBean(new Reply2kCompressionFilter(), Integer.MIN_VALUE);
    }

    /** 接口日志, 开启配置： operations.log.enable=true **/
    @Bean
    @Inject
    public ApiOperationInterceptor apiOperationInterceptor(ApplicationContext context) {
        return new ApiOperationInterceptor(context);
    }

    @Bean
    public BuildTimeHealth buildTimeHealth() {
        return new BuildTimeHealth();
    }
    public static class BuildTimeHealth implements HealthIndicator {
        private static String T_KEY = "time";
        @Override
        public Health health() {
            try {
                InputStream is = new ClassPathResource("/build-time.json").getInputStream();
                JSONObject json = JsonHelper.parseObject(is, JSONObject.class);
                if(null != json && json.containsKey(T_KEY) && !StringHelper.isBlank(json.getString(T_KEY))) {
                    return Health.up().withDetail(T_KEY, json.getString(T_KEY)).build();
                } else {
                    return Health.down().withDetail(T_KEY, 0).build();
                }
            } catch (Exception e) {
                return Health.down().withDetail(T_KEY, 0).build();
            }
        }
    }

    private FilterRegistrationBean createFilterRegistrationBean(Filter filter, int order) {
        FilterRegistrationBean frBean = new FilterRegistrationBean();
        //noinspection unchecked
        frBean.setFilter(filter);
        frBean.setDispatcherTypes(EnumSet.of(DispatcherType.REQUEST));
        frBean.setOrder(order);
        return frBean;
    }

    /** 全局 Controller **/
    @ControllerAdvice
    public static class GlobalControllerHandler {
        @ExceptionHandler(Throwable.class)
        public void errorHandler(HttpServletResponse response, Throwable cause) {
            ExceptionHelper.responseWrite(response, cause);
        }
        @Controller
        public static class Error404Controller implements ErrorController {
            private static final String ERROR_PATH = "/error";
            @RequestMapping(ERROR_PATH)
            public void error(HttpServletRequest request, HttpServletResponse response) throws IOException {
                response.setStatus(HttpStatus.OK.value());
                ExceptionHelper.responseWrite(response, new RpcException(CommonCode.NotFound));
                Reply2kCompressionFilter.writeResponse(request, response, ExceptionHelper.body(request, response));
            }
            @Override
            public String getErrorPath() {
                return ERROR_PATH;
            }

            @RequestMapping(IConstant.FAVICON_ICON)
            public void favicon(HttpServletResponse response) throws IOException {
                OutputStream os = response.getOutputStream();
                try { BytesHelper.copy(EncryptHelper.decode64Stream(IConstant.MIN_BASE64_IMAGE), os);os.flush();
                } finally { BytesHelper.close(os); }
            }
        }

        /** 只有使用PRC CMP 方式调用时才启作用 **/
        @Controller
        public static class RpcController {
            @ApiOperation(name = "RPC CMP 请求转发入口", note = false)
            @RequestMapping(path = {"", "/", "/rest"}, method = RequestMethod.POST)
            public void dispatcher(@RequestBody StringRpcQuery query, HttpServletRequest request, HttpServletResponse response) throws Exception {
                RpcReply.Helper.get().setQuery(query);
                final byte[] body = BytesHelper.utf8Bytes(query.args());
                request.getRequestDispatcher(uri(query.ctrl(), query.method())).forward(new HttpServletRequestWrapper(request) {
                    @Override
                    public int getContentLength() {
                        return body.length;
                    }
                    @Override
                    public String getMethod() {
                        return 0 == body.length ? "GET" : "POST";
                    }
                    @Override
                    public ServletInputStream getInputStream() throws IOException {
                        return 0 == body.length ? request.getInputStream() : BytesHelper.castServletInputStream(new ByteArrayInputStream(body));
                    }
                }, response);
            }

            private static String uri(String ctrl, String method) {
                if(StringHelper.isBlank(ctrl) && StringHelper.isBlank(method)) {
                    throw new RpcException(CommonCode.Unavailable);
                }
                if(!StringHelper.isBlank(ctrl)) {
                    if(!ctrl.startsWith("/")) {
                        ctrl = "/" + ctrl;
                    }
                    if(ctrl.endsWith("/")) {
                        ctrl = ctrl.substring(0, ctrl.length() - 1);
                    }
                }
                if(!StringHelper.isBlank(method) && !method.startsWith("/")) {
                    method = "/" + method;
                }
                return ctrl + method;
            }
            public static class StringRpcQuery extends RpcQuery<String> {
                private static final long serialVersionUID = 3645632047183378870L;
                @Override
                public void verify() {
                    String dispatcher = uri(ctrl(), method());
                    if(StringHelper.isBlank(dispatcher) || dispatcher.equals("/")) {
                        throw new RpcException(CommonCode.Unavailable);
                    }
                }
            }

            @ApiOperation(name = "服务状态监控")
            @RequestMapping(path = "/{wn}/health_check", method = RequestMethod.GET)
            public @ResponseBody Map<String, Object> healthCheck(@PathVariable("wn") String wn) {
                if(SpringHelper.applicationName().equalsIgnoreCase(wn)) {
                    return ImmutableMap.of("name", wn, "status", 200);
                } else {
                    return ImmutableMap.of("status", 404, "message", String.format("没有找到[%s]服务", wn));
                }
            }
        }
    }
}
