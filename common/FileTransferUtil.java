package common;

import java.io.*;
import java.nio.charset.StandardCharsets;

/**
 * Tiện ích truyền file qua TCP Socket.
 *
 * Định dạng gói tin gửi file:
 *   [4 bytes] nameLength  - độ dài tên file (UTF-8 bytes)
 *   [N bytes] name        - tên file
 *   [8 bytes] dataLength  - độ dài dữ liệu file
 *   [M bytes] data        - nội dung file
 */
public class FileTransferUtil {

    /**
     * Gửi file qua DataOutputStream.
     * @param out    luồng output của socket
     * @param file   file cần gửi
     */
    public static void sendFile(DataOutputStream out, File file) throws IOException {
        byte[] nameBytes = file.getName().getBytes(StandardCharsets.UTF_8);
        out.writeInt(nameBytes.length);
        out.write(nameBytes);

        long fileLen = file.length();
        out.writeLong(fileLen);

        byte[] buffer = new byte[Protocol.BUFFER_SIZE];
        try (FileInputStream fis = new FileInputStream(file)) {
            int read;
            while ((read = fis.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
        }
        out.flush();
    }

    /**
     * Gửi dữ liệu byte array như là file.
     * @param out      luồng output
     * @param fileName tên file
     * @param data     nội dung file
     */
    public static void sendBytes(DataOutputStream out, String fileName, byte[] data) throws IOException {
        byte[] nameBytes = fileName.getBytes(StandardCharsets.UTF_8);
        out.writeInt(nameBytes.length);
        out.write(nameBytes);
        out.writeLong(data.length);
        out.write(data);
        out.flush();
    }

    /**
     * Nhận file từ DataInputStream.
     * @param in      luồng input của socket
     * @param destDir thư mục đích để lưu file
     * @return file đã nhận
     */
    public static File receiveFile(DataInputStream in, File destDir) throws IOException {
        int nameLen = in.readInt();
        byte[] nameBytes = new byte[nameLen];
        in.readFully(nameBytes);
        String fileName = new String(nameBytes, StandardCharsets.UTF_8);

        long dataLen = in.readLong();
        File outFile = new File(destDir, fileName);

        byte[] buffer = new byte[Protocol.BUFFER_SIZE];
        try (FileOutputStream fos = new FileOutputStream(outFile)) {
            long remaining = dataLen;
            while (remaining > 0) {
                int toRead = (int) Math.min(buffer.length, remaining);
                int read   = in.read(buffer, 0, toRead);
                if (read == -1) throw new EOFException("Mất kết nối khi nhận file: " + fileName);
                fos.write(buffer, 0, read);
                remaining -= read;
            }
        }
        return outFile;
    }

    /**
     * Nhận dữ liệu file dưới dạng byte array.
     * @param in luồng input
     * @return mảng {tên file bytes, nội dung bytes}
     */
    public static byte[][] receiveBytes(DataInputStream in) throws IOException {
        int nameLen = in.readInt();
        byte[] nameBytes = new byte[nameLen];
        in.readFully(nameBytes);

        long dataLen = in.readLong();
        byte[] data = new byte[(int) dataLen];
        in.readFully(data);

        return new byte[][] { nameBytes, data };
    }
}