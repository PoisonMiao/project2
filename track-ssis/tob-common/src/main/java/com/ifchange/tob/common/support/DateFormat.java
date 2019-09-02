package com.ifchange.tob.common.support;

import com.ifchange.tob.common.core.StringEnum;

/** 时间格式化 **/
public enum DateFormat implements StringEnum {
    ShortNumDate("yyMMdd"),

    NumDate("yyyyMMdd"),

    StrikeDate("yyyy-MM-dd"),

    NumDateTime("yyyyMMddHHmmss"),

    TwoYearNumDateTime("yyMMddHHmmss"),

    StrikeDateTime("yyyy-MM-dd HH:mm:ss"),

    DoubleDateTime("yyyyMMddHHmmss.SSS"),

    MillisecondTime("yyyy-MM-dd HH:mm:ss SSS"),

    NumTime("HHmmss"),

    ColonTime("HH:mm:ss");

    DateFormat(String value) {
	    changeNameTo(this, value);
    }
}
