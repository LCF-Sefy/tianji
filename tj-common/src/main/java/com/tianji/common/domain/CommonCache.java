package com.tianji.common.domain;

import java.io.Serializable;

public class CommonCache implements Serializable {
    private static final long serialVersionUID = 2448735813082442223L;
    //缓存数据是否存在
    protected boolean exist;

    //稍后再试
    protected boolean retryLater;

    public boolean isExist() {
        return exist;
    }

    public void setExist(boolean exist) {
        this.exist = exist;
    }


    public boolean isRetryLater() {
        return retryLater;
    }

    public void setRetryLater(boolean retryLater) {
        this.retryLater = retryLater;
    }
}