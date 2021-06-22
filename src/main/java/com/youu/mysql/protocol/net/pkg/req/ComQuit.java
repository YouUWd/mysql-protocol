package com.youu.mysql.protocol.net.pkg.req;

import com.youu.mysql.protocol.net.pkg.MySQLPacket;
import io.netty.buffer.ByteBuf;
import lombok.Data;

/**
 * @Author Timmy
 * @Description
 * @Date 2021/6/17
 */
@Data
public class ComQuit extends MySQLPacket {
    private static final byte ID = 0x01;

    @Override
    protected void readPayload(ByteBuf buffer) {
        buffer.skipBytes(1);//skip command id
    }

    @Override
    protected void writePayload(ByteBuf buffer) {
        buffer.writeByte(ID);
    }

}
