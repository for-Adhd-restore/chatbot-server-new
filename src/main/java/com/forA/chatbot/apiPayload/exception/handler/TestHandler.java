package com.forA.chatbot.apiPayload.exception.handler;

import com.forA.chatbot.apiPayload.code.BaseErrorCode;
import com.forA.chatbot.apiPayload.exception.GeneralException;

public class TestHandler extends GeneralException {
    public TestHandler(BaseErrorCode code) {
        super(code);
    }
}
