package com.na.common.result.enums;

public interface INaStatusProvider {
    Integer getCode();
    String getEnMsg();
    String getZhMsg();
    String getMsg();

    String msgKey();
    String getLanguageMsg();
}
