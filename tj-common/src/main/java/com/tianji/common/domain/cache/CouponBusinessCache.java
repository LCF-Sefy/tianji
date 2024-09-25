package com.tianji.common.domain.cache;

import com.tianji.common.domain.CommonCache;

public class CouponBusinessCache<T> extends CommonCache {

    private T data;

    public CouponBusinessCache<T> with(T data){
        this.data = data;
        this.exist = true;
        return this;
    }


    public CouponBusinessCache<T> retryLater(){
        this.retryLater = true;
        return this;
    }

    public CouponBusinessCache<T> notExist(){
        this.exist = false;
        return this;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}