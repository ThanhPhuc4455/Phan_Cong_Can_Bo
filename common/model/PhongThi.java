package model;

import java.io.Serializable;

/**
 * Model: Phòng thi
 */
public class PhongThi implements Serializable {
    private int    stt;
    private String tenPhong;  // Tên/mã phòng (VD: C101, 128)
    private String diaDiem;   // Địa điểm (VD: Đà Nẵng, Huế)

    public PhongThi() {}

    public PhongThi(int stt, String tenPhong, String diaDiem) {
        this.stt      = stt;
        this.tenPhong = tenPhong;
        this.diaDiem  = diaDiem;
    }

    public int    getStt()          { return stt; }
    public void   setStt(int s)     { this.stt = s; }

    public String getTenPhong()           { return tenPhong; }
    public void   setTenPhong(String t)   { this.tenPhong = t; }

    public String getDiaDiem()            { return diaDiem; }
    public void   setDiaDiem(String d)    { this.diaDiem = d; }

    @Override
    public String toString() {
        return String.format("Phòng %s - %s", tenPhong, diaDiem);
    }
}