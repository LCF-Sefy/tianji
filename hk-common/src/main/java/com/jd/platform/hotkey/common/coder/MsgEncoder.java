package com.jd.platform.hotkey.common.coder;

import com.jd.platform.hotkey.common.model.HotKeyMsg;
import com.jd.platform.hotkey.common.tool.Constant;
import com.jd.platform.hotkey.common.tool.ProtostuffUtils;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

/**
 * @author wuweifeng
 * @version 1.0
 * @date 2020-07-30
 */
public class MsgEncoder extends MessageToByteEncoder {

    @Override
    public void encode(ChannelHandlerContext ctx, Object in, ByteBuf out) {
        if (in instanceof HotKeyMsg) {
            byte[] bytes = ProtostuffUtils.serialize(in);
            byte[] delimiter = Constant.DELIMITER.getBytes();

            byte[] total = new byte[bytes.length + delimiter.length];
            System.arraycopy(bytes, 0, total, 0, bytes.length);
            System.arraycopy(delimiter, 0, total, bytes.length, delimiter.length);

            out.writeBytes(total);
        }
    }
}