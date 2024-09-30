package com.jd.platform.hotkey.common.convert;

import com.alibaba.fastjson.serializer.JSONSerializer;
import com.alibaba.fastjson.serializer.ObjectSerializer;
import com.alibaba.fastjson.serializer.SerializeWriter;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.concurrent.atomic.LongAdder;

/**
 * @author qiujw wrote on 2021-06-29
 * @version 1.0
 */
public class LongAdderSerializer implements ObjectSerializer {

    @Override
    public void write(JSONSerializer serializer, Object object, Object fieldName, Type fieldType, int features) throws IOException {
        SerializeWriter out = serializer.out;
        if (object instanceof Long) {
            out.writeFieldValue('{', "value", ((Long) object));
            out.write('}');
        }else if (object instanceof LongAdder) {
            out.writeFieldValue('{', "value", ((LongAdder) object).longValue());
            out.write('}');
        }
    }
}
