package com.hamradio.protocol;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class MessageCodec {
    public static final int MAGIC = 0x4852; // "HR"

    /**
     * Writes a framed message to the output stream.
     * Frame format: magic (2 bytes) + type (1 byte) + length (4 bytes) + payload.
     * Synchronized on the output stream to prevent interleaving from multiple threads.
     */
    public static void write(OutputStream out, Message msg) throws IOException {
        byte[] payload = msg.encodePayload();
        synchronized (out) {
            DataOutputStream dos = new DataOutputStream(out);
            dos.writeShort(MAGIC);
            dos.writeByte(msg.getType().getId());
            dos.writeInt(payload.length);
            dos.write(payload);
            dos.flush();
        }
    }

    /**
     * Reads a framed message from the input stream.
     * Verifies magic bytes, reads header, then decodes the payload.
     */
    public static Message read(InputStream in) throws IOException {
        DataInputStream dis = new DataInputStream(in);
        int magic = dis.readUnsignedShort();
        if (magic != MAGIC) {
            throw new IOException("Invalid magic bytes: 0x" + Integer.toHexString(magic)
                    + ", expected 0x" + Integer.toHexString(MAGIC));
        }
        int typeId = dis.readUnsignedByte();
        MessageType type = MessageType.fromId(typeId);
        int length = dis.readInt();
        byte[] payload = new byte[length];
        dis.readFully(payload);
        return Message.decode(type, payload);
    }

    /**
     * Writes a length-prefixed UTF-8 string to a DataOutputStream.
     * The length prefix is a single unsigned byte, supporting strings up to 255 bytes.
     */
    public static void writeString(DataOutputStream dos, String s) throws IOException {
        byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
        dos.writeByte(bytes.length);
        dos.write(bytes);
    }

    /**
     * Reads a length-prefixed UTF-8 string from a DataInputStream.
     */
    public static String readString(DataInputStream dis) throws IOException {
        int len = dis.readUnsignedByte();
        byte[] bytes = new byte[len];
        dis.readFully(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }
}
