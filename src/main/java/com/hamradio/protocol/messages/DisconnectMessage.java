package com.hamradio.protocol.messages;

import com.hamradio.protocol.Message;
import com.hamradio.protocol.MessageType;

public class DisconnectMessage extends Message {

    public DisconnectMessage() {
    }

    @Override
    public MessageType getType() {
        return MessageType.DISCONNECT;
    }

    @Override
    public byte[] encodePayload() {
        return new byte[0];
    }

    public static DisconnectMessage fromBytes(byte[] payload) {
        return new DisconnectMessage();
    }
}
