package com.ifchange.tob.common.view.parser;

import com.ifchange.tob.common.core.StringEnum;

public enum RequestHeader implements StringEnum {
    ReplyFormat("X-Reply-Format", "要求返回类型 HTML, JSON, STREAM；默认返回 HTML, STREAM 文件下载"),

    RID("X-Request-ID", "每次请求唯一编号"),

    Vno("X-Vno", "APP版本， 如： V_1.0, V_2.0, V_3.0"),

    DeviceID("X-Device-NO", "设备编号，每个设备的唯一标识"),

    ClientTime("X-Client-Time", "请求时的客户端时间戳毫秒数"),

    DataSecret("X-Data-Secret", "请求数据的签值"),

    Signature("X-Signature", "权限签名数据");

    RequestHeader(String value, String desc) {
        changeNameTo(this, value);
    }
}
