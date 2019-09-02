package com.ifchange.tob.common.view;

import com.alibaba.fastjson.support.spring.FastJsonHttpMessageConverter;
import com.alibaba.fastjson.util.IOUtils;
import com.ifchange.tob.common.helper.JsonHelper;
import org.springframework.http.MediaType;

import java.util.Arrays;

public final class FastJsonMessageConverter extends FastJsonHttpMessageConverter {
    public static final FastJsonMessageConverter INSTANCE = new FastJsonMessageConverter();

    private FastJsonMessageConverter() {
        super.setDefaultCharset(IOUtils.UTF8);
        super.getFastJsonConfig().setSerializerFeatures(JsonHelper.serializerFeatures());
        super.setSupportedMediaTypes(Arrays.asList(MediaType.TEXT_XML,
                                                   MediaType.APPLICATION_XML,
                                                   MediaType.APPLICATION_JSON,
                                                   MediaType.APPLICATION_JSON_UTF8,
                                                   MediaType.APPLICATION_FORM_URLENCODED));
    }
}
