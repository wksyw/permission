package com.oumeng.auth.config;


import com.alibaba.fastjson.JSONObject;
import com.oumeng.auth.entity.LicenseData;
import com.oumeng.auth.utils.JsonUtil;
import com.sun.jna.*;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Component
public class LicenseCheckService {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private StringRedisTemplate redisTemplate;

    //public static LicenseData licenseData;

    public LicenseData getLicenseData() {
        /*String licenseCheckTimeOut = redisTemplate.opsForValue().get("licenseCheckTimeOut");
        if(licenseCheckTimeOut==null || readLicenseErrorTime!=null){
            licenseData = licenseStatusCheck();
            redisTemplate.opsForValue().set("licenseCheckTimeOut","1",1, TimeUnit.DAYS);
        }
        return licenseData;*/
        return licenseStatusCheck();
    }

    @Value("${licenseFileName:/usr/oss/file/license/license.bin}")
    private String licenseFileName;

    public static class LICENSE_ONE_MODULE_DATA extends Structure {
        public String systemModuleName;

        public Pointer systemModule;

        public int maxSystemModuleLen;

        public static class ByReference extends LICENSE_ONE_MODULE_DATA implements Structure.ByReference {
        }

        public static class ByValue extends LICENSE_ONE_MODULE_DATA implements Structure.ByValue {
        }

        @Override
        protected List<String> getFieldOrder() {
            return Arrays.asList("systemModuleName", "systemModule", "maxSystemModuleLen");
        }
    }

    /*public interface CLibraryA extends Library {
    }*/
    public interface CLibrary extends Library {
        //CLibraryA INSTANCE1 = Native.load("/data/test/licensetest/libDeEncryption.so",CLibraryA.class);
        CLibrary INSTANCE = Native.load("/usr/oss/file/license/libLicenseControlDynamic.so", CLibrary.class);

        int readOneModuleData(String licenseFileName, LICENSE_ONE_MODULE_DATA.ByReference licenseOneModuleData);
    }

    public String readLicense() {
        String licenseJsonData = null;
        LicenseCheckService.LICENSE_ONE_MODULE_DATA.ByReference licenseOneModuleData = null;
        try {
            licenseOneModuleData = new LicenseCheckService.LICENSE_ONE_MODULE_DATA.ByReference();
            licenseOneModuleData.systemModuleName = "425";
            licenseOneModuleData.systemModule = new Memory(204800);
            licenseOneModuleData.maxSystemModuleLen = 204800;
            int result = LicenseCheckService.CLibrary.INSTANCE.readOneModuleData(licenseFileName, licenseOneModuleData);
            licenseJsonData = licenseOneModuleData.systemModule.getString(0);
            logger.info("result = "+result+" jsonData = " + licenseJsonData);
        } catch (Throwable e) {
            e.printStackTrace();
            logger.error("", e);
        } finally {
            if(licenseOneModuleData!=null){
                Native.free(Pointer.nativeValue(licenseOneModuleData.systemModule));
                Pointer.nativeValue(licenseOneModuleData.systemModule, 0);
            }
        }
        logger.info("licenseJsonData:"+licenseJsonData);
        return licenseJsonData;
        /*String jsonData = "{\"content\":\"{\\\"system\\\":\\\"425\\\",\\\"type\\\":1,\\\"dataVersion\\\":\\\"V1.0\\\",\\\"enableTime\\\":\\\"2023-06-05 00:00:01\\\",\\\"expiredTime\\\":\\\"2023-06-16 23:59:59\\\",\\\"childModule\\\":[{\\\"permissionType\\\":\\\"1\\\",\\\"enableTime\\\":\\\"2023-06-01 00:00:01\\\",\\\"expiredTime\\\":\\\"2099-12-31 23:59:59\\\",\\\"type\\\":1.0,\\\"id\\\":\\\"001\\\",\\\"code\\\":\\\"workplate\\\"},{\\\"permissionType\\\":\\\"2\\\",\\\"enableTime\\\":\\\"2023-06-01 00:00:01\\\",\\\"expiredTime\\\":\\\"2099-12-31 23:59:59\\\",\\\"type\\\":1.0,\\\"id\\\":\\\"001.001\\\",\\\"code\\\":\\\"sampletracking\\\"},{\\\"permissionType\\\":\\\"2\\\",\\\"enableTime\\\":\\\"2023-06-01 00:00:01\\\",\\\"expiredTime\\\":\\\"2099-12-31 23:59:59\\\",\\\"type\\\":1.0,\\\"id\\\":\\\"001.002\\\",\\\"code\\\":\\\"sampleCenter\\\"},{\\\"permissionType\\\":\\\"1\\\",\\\"enableTime\\\":\\\"2023-06-01 00:00:01\\\",\\\"expiredTime\\\":\\\"2099-12-31 23:59:59\\\",\\\"type\\\":1.0,\\\"id\\\":\\\"001.003\\\",\\\"code\\\":\\\"expCenter\\\"},{\\\"permissionType\\\":\\\"2\\\",\\\"enableTime\\\":\\\"2023-06-01 00:00:01\\\",\\\"expiredTime\\\":\\\"2099-12-31 23:59:59\\\",\\\"type\\\":1.0,\\\"id\\\":\\\"001.003.001\\\",\\\"code\\\":\\\"ymn\\\"},{\\\"permissionType\\\":\\\"2\\\",\\\"enableTime\\\":\\\"2023-06-01 00:00:01\\\",\\\"expiredTime\\\":\\\"2099-12-31 23:59:59\\\",\\\"type\\\":1.0,\\\"id\\\":\\\"001.003.002\\\",\\\"code\\\":\\\"mgi\\\"},{\\\"permissionType\\\":\\\"1\\\",\\\"enableTime\\\":\\\"2023-06-01 00:00:01\\\",\\\"expiredTime\\\":\\\"2099-12-31 23:59:59\\\",\\\"type\\\":1.0,\\\"id\\\":\\\"001.004\\\",\\\"code\\\":\\\"Analysis\\\"},{\\\"permissionType\\\":\\\"2\\\",\\\"enableTime\\\":\\\"2023-06-01 00:00:01\\\",\\\"expiredTime\\\":\\\"2099-12-31 23:59:59\\\",\\\"type\\\":1.0,\\\"id\\\":\\\"001.004.001\\\",\\\"code\\\":\\\"Analysis-YMN\\\"},{\\\"permissionType\\\":\\\"2\\\",\\\"enableTime\\\":\\\"2023-06-01 00:00:01\\\",\\\"expiredTime\\\":\\\"2099-12-31 23:59:59\\\",\\\"type\\\":1.0,\\\"id\\\":\\\"001.004.002\\\",\\\"code\\\":\\\"Analysis-MGI\\\"},{\\\"permissionType\\\":\\\"1\\\",\\\"enableTime\\\":\\\"2023-06-01 00:00:01\\\",\\\"expiredTime\\\":\\\"2099-12-31 23:59:59\\\",\\\"type\\\":1.0,\\\"id\\\":\\\"001.005\\\",\\\"code\\\":\\\"Report\\\"},{\\\"permissionType\\\":\\\"2\\\",\\\"enableTime\\\":\\\"2023-06-01 00:00:01\\\",\\\"expiredTime\\\":\\\"2099-12-31 23:59:59\\\",\\\"type\\\":1.0,\\\"id\\\":\\\"001.005.001\\\",\\\"code\\\":\\\"Report-YMN\\\"},{\\\"permissionType\\\":\\\"2\\\",\\\"enableTime\\\":\\\"2023-06-01 00:00:01\\\",\\\"expiredTime\\\":\\\"2099-12-31 23:59:59\\\",\\\"type\\\":1.0,\\\"id\\\":\\\"001.005.002\\\",\\\"code\\\":\\\"Report-MGI\\\"},{\\\"permissionType\\\":\\\"1\\\",\\\"enableTime\\\":\\\"2023-06-01 00:00:01\\\",\\\"expiredTime\\\":\\\"2099-12-31 23:59:59\\\",\\\"type\\\":1.0,\\\"id\\\":\\\"001.006\\\",\\\"code\\\":\\\"Transform\\\"},{\\\"permissionType\\\":\\\"2\\\",\\\"enableTime\\\":\\\"2023-06-01 00:00:01\\\",\\\"expiredTime\\\":\\\"2099-12-31 23:59:59\\\",\\\"type\\\":1.0,\\\"id\\\":\\\"001.006.001\\\",\\\"code\\\":\\\"Transform-mNGS\\\"},{\\\"permissionType\\\":\\\"2\\\",\\\"enableTime\\\":\\\"2023-06-01 00:00:01\\\",\\\"expiredTime\\\":\\\"2099-12-31 23:59:59\\\",\\\"type\\\":1.0,\\\"id\\\":\\\"001.006.002\\\",\\\"code\\\":\\\"Transform-tNGS\\\"},{\\\"permissionType\\\":\\\"2\\\",\\\"enableTime\\\":\\\"2023-06-01 00:00:01\\\",\\\"expiredTime\\\":\\\"2099-12-31 23:59:59\\\",\\\"type\\\":1.0,\\\"id\\\":\\\"001.007\\\",\\\"code\\\":\\\"checkedCenter\\\"},{\\\"permissionType\\\":\\\"2\\\",\\\"enableTime\\\":\\\"2023-06-01 00:00:01\\\",\\\"expiredTime\\\":\\\"2099-12-31 23:59:59\\\",\\\"type\\\":1.0,\\\"id\\\":\\\"001.008\\\",\\\"code\\\":\\\"anomalousSample\\\"},{\\\"permissionType\\\":\\\"2\\\",\\\"enableTime\\\":\\\"2023-06-01 00:00:01\\\",\\\"expiredTime\\\":\\\"2099-12-31 23:59:59\\\",\\\"type\\\":1.0,\\\"id\\\":\\\"001.009\\\",\\\"code\\\":\\\"hisDataManager\\\"}]}\",\"md5Key\":\"f62efc009c886d1a21f303a3c0d98ecd\"}";

        return jsonData;*/
    }

    public static void main(String[] args) {
        System.out.println(DigestUtils.md5Hex("2023-06-05 00:00:01" + "_" + "2023-06-16 23:59:59" + "_" + "1" + "_oumeng!@#$"));
    }

    public LicenseData licenseStatusCheck() {
        LicenseData licenseDataResult = new LicenseData();
        int licenseStatus = 0;
        try {
            Map<String, Object> appDbCheckMap = JSONObject.parseObject(redisTemplate.opsForValue().get("appCheckData"));
            String currentTime = TimeUtil.getCurrentDateTime();
            if (appDbCheckMap == null) {
                licenseStatus = -2012;
            } else {
                String sign = (String) appDbCheckMap.get("sign");
                String createTimeDb = (String) appDbCheckMap.get("createTime");
                String expireTimeDb = (String) appDbCheckMap.get("expireTime");
                int runDay = Integer.parseInt(appDbCheckMap.get("runDay").toString());
                //校验签名
                if (!sign.equals(DigestUtils.md5Hex(createTimeDb + "_" + expireTimeDb + "_" + runDay + "_oumeng!@#$"))) {
                    licenseStatus = -2012;
                }
                String jsonData = readLicense();
                if(StringUtils.isEmpty(jsonData)){
                    //再次读取失败 延期三天过期
                    licenseDataResult.setWarnStatus(1);
                    //如果过期时间小于当前时间记为过期 license过期校验（规则：expireTime-createTime的天数+buffer天数得大于runDay）
                    if (expireTimeDb.compareTo(currentTime) < 0 || TimeUtil.getTimeDay(createTimeDb, expireTimeDb) + 30 < runDay) {
                        licenseStatus = -2013;
                    }else {
                        String readLicenseErrorTime = redisTemplate.opsForValue().get("readLicenseErrorTime");
                        if(readLicenseErrorTime==null){
                            licenseStatus = -2014;
                            int licenseRunErrorMinute = 4320;
                            String licenseRunErrorMinuteStr = redisTemplate.opsForValue().get("licenseRunErrorMinute");
                            if(!StringUtils.isEmpty(licenseRunErrorMinuteStr)){
                                licenseRunErrorMinute = Integer.parseInt(licenseRunErrorMinuteStr);
                            }
                            readLicenseErrorTime = TimeUtil.getFormatTime(TimeUtil.DATE_FORMAT_STR_ALL,TimeUtil.timePastTenSecond(licenseRunErrorMinute,new Date(),Calendar.MINUTE));
                            redisTemplate.opsForValue().set("readLicenseErrorTime",readLicenseErrorTime);
                        }else{
                            if (readLicenseErrorTime.compareTo(currentTime) < 0) {
                                licenseStatus = -2015;
                            }
                        }
                        licenseDataResult.setExpireTime(readLicenseErrorTime);
                    }
                }else {
                    redisTemplate.delete("readLicenseErrorTime");
                    Map<String, Object> licenseAllData = (Map<String, Object>) JsonUtil.fromJson(jsonData);
                    String contentJsonStr = (String) licenseAllData.get("content");
                    Map<String, Object> licenseData = (Map<String, Object>) JsonUtil.fromJson(contentJsonStr);
                    //时间天数存储更新
                    String createTime = (String) licenseData.get("enableTime");
                    String expireTime = (String) licenseData.get("expiredTime");
                    licenseDataResult.setExpireTime(expireTime);
                    long lastDay = TimeUtil.getTimeDay(currentTime, expireTime);
                    licenseDataResult.setLastDay(lastDay);
                    //如果过期时间小于当前时间记为过期 license过期校验（规则：expireTime-createTime的天数+buffer天数得大于runDay）
                    if (expireTime.compareTo(currentTime) < 0 || TimeUtil.getTimeDay(createTime, expireTime) + 30 < runDay) {
                        licenseStatus = -2013;
                    }
                    String licenseWarnDay= redisTemplate.opsForValue().get("licenseWarnDay");
                    if(licenseWarnDay!=null){
                        if (lastDay < Integer.parseInt(licenseWarnDay)) {
                            licenseDataResult.setWarnStatus(1);
                        }
                    }
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
            logger.error("", e);
            licenseStatus = -2012;
        }
        licenseDataResult.setLicenseStatus(licenseStatus);
        return licenseDataResult;
    }

}
