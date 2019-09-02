package com.ifchange.tob.common.core;

import com.ifchange.tob.common.helper.GenericHelper;

import java.io.Serializable;

interface IGeneric<T> extends Serializable {
    default Class<?> clazz() {
        return GenericHelper.type(this.getClass());
    }
}
