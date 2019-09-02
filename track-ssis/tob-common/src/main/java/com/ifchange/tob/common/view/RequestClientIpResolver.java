package com.ifchange.tob.common.view;

import com.ifchange.tob.common.helper.NetworkHelper;
import com.ifchange.tob.common.view.parser.ClientIP;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import javax.servlet.http.HttpServletRequest;

public class RequestClientIpResolver implements HandlerMethodArgumentResolver {
    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.getParameterAnnotation(ClientIP.class) != null;
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer container, NativeWebRequest request, WebDataBinderFactory binder) throws Exception {
        HttpServletRequest servletRequest = request.getNativeRequest(HttpServletRequest.class);
        String clientIP = NetworkHelper.ofClientIp(servletRequest);
        Class<?> type = parameter.getParameterType();
        if (String.class.equals(type)) {
            return clientIP;
        } else if (long.class.equals(type) || Long.class.isAssignableFrom(type)) {
            return NetworkHelper.ip2long(clientIP);
        } else {
            throw new ServletRequestBindingException("@ClientIP parameter should be declare as Strong or long or Long");
        }
    }
}
