package com.oumeng.auth.entity;

public class LicenseData {
    //status license的校验状态 0代表能正常使用 -2012代表读取失败（Licence授权文件不合法提示） -2013代表已过期 -2014代表信息被串改
    private int licenseStatus;

    //是否快过期警告 0代表否 1代表是
    private int warnStatus;

    //剩余多少天可用
    private long lastDay;

    //过期时间
    private String expireTime;

    public int getLicenseStatus() {
        return licenseStatus;
    }

    public void setLicenseStatus(int licenseStatus) {
        this.licenseStatus = licenseStatus;
    }

    public long getLastDay() {
        return lastDay;
    }

    public void setLastDay(long lastDay) {
        this.lastDay = lastDay;
    }

    public String getExpireTime() {
        return expireTime;
    }

    public void setExpireTime(String expireTime) {
        this.expireTime = expireTime;
    }

    public int getWarnStatus() {
        return warnStatus;
    }

    public void setWarnStatus(int warnStatus) {
        this.warnStatus = warnStatus;
    }
}
