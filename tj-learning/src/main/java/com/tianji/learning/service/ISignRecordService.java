package com.tianji.learning.service;

import com.tianji.learning.domain.vo.SignResultVO;

import java.util.List;

public interface ISignRecordService {
    SignResultVO addSignRecords();

    Byte[] querySignRecords();
}
