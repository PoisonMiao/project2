package com.ifchange.tob.common.view;

import com.ifchange.tob.common.helper.ThreadFactoryHelper;
import com.ifchange.tob.common.view.parser.RequestSession;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

final class RequestResponseLogger {
    private static final ExecutorService LOG_EXECUTOR = Executors.newSingleThreadExecutor(ThreadFactoryHelper.threadFactoryOf("request-response"));

    private final RequestSession session;

    RequestResponseLogger(RequestSession session) {
        this.session = session;
    }

    void submit() {

    }
}
