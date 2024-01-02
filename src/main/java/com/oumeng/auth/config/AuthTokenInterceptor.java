package com.oumeng.auth.config;

import com.oumeng.auth.entity.AuthConst;
import com.oumeng.auth.entity.LicenseData;
import com.oumeng.auth.entity.User;
import com.oumeng.auth.utils.JsonUtil;
import com.oumeng.auth.utils.ProcessResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

/**
 * shiro token 拦截器，负责对鉴权等业务进行拦截
 *
 * @author helms
 */
@Service
public class AuthTokenInterceptor implements HandlerInterceptor, InitializingBean {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final int ErrorNeedToken = 3008;
    private final String ErrorMsgNeedToken = "用户没有登录";

    private final int ErrorVersionCheck = 2009;
    private final String ErrorMsgVersionCheck = "版本不一致";

    private final int ErrorNoPermission = 2008;
    private final String ErrorMsgNoPermission = "没有权限操作";

    public static final int ERROR_USER_STATUS_LOCK = 20002;
    public static final String ERROR_USER_STATUS_LOCK_MSG = "用户已被停用";

    @Autowired
    private LicenseCheckService licenseCheckService;


    /*@Value("${not.interceptor.url:}")
    private String notInterceptorUrl;*/

    @Value("${not.permission.url:/*}")
    private String notPermissionUrl;

    @Value("${not.interceptor.token:}")
    private String notInterceptorToken;

    @Value("${spring.application.name:defaultService}")
    private String applicationName;

    @Autowired
    protected StringRedisTemplate stringRedisTemplate;

    @Autowired
    private DataLogUtil dataLogUtil;

    @Autowired
    private Request request;

    @Value("${initLicense:0}")
    private String initLicense;

    @Override
    public void afterPropertiesSet() throws Exception {
        // TODO Auto-generated method stub

    }

    protected boolean isAjaxRequest(HttpServletRequest request) {
        try {
            String ajaxRequest = request.getHeader("x-requested-with");
            logger.debug("ajax");
            if (ajaxRequest != null && ajaxRequest.contains("XMLHttpRequest")) {
                return true;
            }
            ajaxRequest = request.getHeader("Content-Type");
            logger.debug("ajax:" + ajaxRequest);
            if (ajaxRequest != null && (ajaxRequest.contains("application/x-www-form-urlencoded")
                    || ajaxRequest.contains("application/json"))) {
                return true;
            }
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public boolean preHandle(HttpServletRequest httpServletRequest, HttpServletResponse response, Object handler)
            throws Exception {
        // TODO Auto-generated method stub
        String requestUrl = "";
        try {
            String version = httpServletRequest.getHeader("version");
            Map<String, Object> data = JsonUtil.fromJson(stringRedisTemplate.opsForValue().get("versionData"), Map.class);
            if (version != null) {
                if (data != null) {
                    String configVersion = (String) data.get("version");
                    if (configVersion != null && !configVersion.equals("")) {
                        Object versionCheckObject = data.get("versionCheck");
                        if (versionCheckObject != null) {
                            String versionCheckStr = versionCheckObject.toString();
                            if (versionCheckStr.equals("1")) {
                                if (versionChanged(version, configVersion)) {
                                    logger.info("AuthTokenInterceptor version: " + version + " configVersion:" + configVersion);
                                    needAuthAccess(response, ErrorVersionCheck, ErrorMsgVersionCheck);
                                    return false;
                                }
                            }
                        }
                    }
                }
            }
            boolean notInterceptor = false;
            requestUrl = httpServletRequest.getRequestURI();
            logger.info("AuthTokenInterceptor: " + requestUrl + " applicationName: " + applicationName);
            requestUrl = requestUrl.replace(httpServletRequest.getContextPath(), "");
            int urlIndex = requestUrl.indexOf("/", 1);
            String requestToken = httpServletRequest.getHeader("token");
            logger.info("AuthTokenInterceptor token: " + requestToken);
            if (!notInterceptorToken.equals("") && notInterceptorToken.equals(requestToken)) {
                return HandlerInterceptor.super.preHandle(httpServletRequest, response, handler);
            }
            if (urlIndex == -1) {
                return HandlerInterceptor.super.preHandle(httpServletRequest, response, handler);
            }
            /*String leftUrl = requestUrl.substring(0, urlIndex);
            String rightUrl = requestUrl.substring(urlIndex);*/
            String notInterceptorUrl = stringRedisTemplate.opsForValue().get("notInterceptorUrl:" + applicationName);
            if (notInterceptorUrl == null || notInterceptorUrl.equals("")) {
                notInterceptorUrl = "/*";
            }
            String[] notInterceptorUrlArr = notInterceptorUrl.split(",");
            for (String url : notInterceptorUrlArr) {
                if ("/*".equals(url)) {
                    notInterceptor = true;
                    break;
                }
                /*if(url.contains("*")){
                    String notInterceptorLeftUrl = url.replace("*", "");
                    if (requestUrl.startsWith(notInterceptorLeftUrl)) {
                        notInterceptor = true;
                        break;
                    }
                }else {
                    if (requestUrl.equals(url)) {
                        notInterceptor = true;
                        break;
                    }
                }*/
                /*String notInterceptorLeftUrl = url.substring(0, url.indexOf("/", 1));
                String notInterceptorRightUrl = url.substring(url.indexOf("/", 1));
                if (leftUrl.equals(notInterceptorLeftUrl)) {
                    if("/*".equals(notInterceptorRightUrl)){
                        notInterceptor = true;
                        break;
                    }
                    if(rightUrl.equals(notInterceptorRightUrl)){
                        notInterceptor = true;
                        break;
                    }
                }*/
                String notInterceptorLeftUrl = url.replace("*", "");
                if (requestUrl.startsWith(notInterceptorLeftUrl)) {
                    notInterceptor = true;
                    break;
                }
            }
            User user = request.getLoginUser();
            if (!notInterceptor) {
                if (requestToken == null) {
                    logger.info("AuthTokenInterceptor token: " + requestToken);
                    needAuthAccess(response, ErrorNeedToken, ErrorMsgNeedToken);
                    return false;
                }
                if (user == null) {
                    logger.info("AuthTokenInterceptor token: " + requestToken);
                    needAuthAccess(response, ErrorNeedToken, ErrorMsgNeedToken);
                    return false;
                }
                if (user.getStatus() == 1) {
                    logger.info("AuthTokenInterceptor token: " + requestToken);
                    needAuthAccess(response, ERROR_USER_STATUS_LOCK, ERROR_USER_STATUS_LOCK_MSG);
                    return false;
                }
                boolean notPermission = false;
                if (notPermissionUrl != null && !notPermissionUrl.equals("")) {
                    String[] notPermissionUrlArr = notPermissionUrl.split(",");
                    for (String url : notPermissionUrlArr) {
                        if ("/*".equals(url)) {
                            notPermission = true;
                            break;
                        }
                        /*String notPermissionLeftUrl = url.substring(0, url.indexOf("/", 1));
                        String notPermissionRightUrl = url.substring(url.indexOf("/", 1));
                        if (leftUrl.equals(notPermissionLeftUrl)) {
                            if("/*".equals(notPermissionRightUrl)){
                                notPermission = true;
                                break;
                            }
                            if(rightUrl.equals(notPermissionRightUrl)){
                                notPermission = true;
                                break;
                            }
                        }*/
                        String notInterceptorLeftUrl = url.replace("*", "");
                        if (requestUrl.startsWith(notInterceptorLeftUrl)) {
                            notPermission = true;
                            break;
                        }
                    }
                }
                //验证license
                LicenseData licenseData = licenseCheckService.getLicenseData();
                if(licenseData!=null){
                    int licenseStatus = licenseData.getLicenseStatus();
                    if (licenseStatus != 0 && licenseStatus!=-2014) {
                        needAuthAccess(response, licenseData.getLicenseStatus(), "license信息错误");
                        return false;
                    }
                }
                //如果角色是admin
                if (user.getIsAdmin() == 1) {
                    if (!notPermission) {
                        dataLogUtil.insertLog(httpServletRequest, requestUrl, user, 1);
                    }
                    return HandlerInterceptor.super.preHandle(httpServletRequest, response, handler);
                }
                if (!notPermission) {
                    String permission = stringRedisTemplate.opsForValue().get((AuthConst.getUrlKey(user.getUserId() + "", requestUrl)));
                    if ("255".equals(permission)) {
                        logger.info("AuthTokenInterceptor token: " + requestToken);
                        dataLogUtil.insertLog(httpServletRequest, requestUrl, user, 2);
                        needAuthAccess(response, ErrorNoPermission, ErrorMsgNoPermission);
                        return false;
                    } else {
                        dataLogUtil.insertLog(httpServletRequest, requestUrl, user, 1);
                    }
                }
            } else {
                if (user != null && user.getStatus() == 1) {
                    logger.info("AuthTokenInterceptor token: " + requestToken);
                    needAuthAccess(response, ERROR_USER_STATUS_LOCK, ERROR_USER_STATUS_LOCK_MSG);
                    return false;
                }
            }
        } catch (Throwable e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            logger.error("requestUri=" + requestUrl, e);
            //needAuthAccess(response, -1, "exception error");
            return true;
        }
        return HandlerInterceptor.super.preHandle(httpServletRequest, response, handler);
    }


    /**
     * @param response
     */
    protected void needAuthAccess(HttpServletResponse response, int value, String errorMsg) {
        // response.sendRedirect(this.loginUrl);
        response.setContentType("application/json; charset=UTF-8");

        response.setCharacterEncoding("UTF-8");
        PrintWriter out = null;
        try {
            out = response.getWriter();
            // 将返回的对象转换为Json串，返回到输出流 ?
            ProcessResult ret = ProcessResult.getProcessResult(value);
            ret.setErrorMsg(errorMsg);
            logger.debug(ret.toString());
            out.println(JsonUtil.toJson(ret));
            out.flush();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            logger.error("", e);
        }

    }

    private static boolean versionChanged(String version, String minVersion) {
        String[] versionArr = version.split("\\.");
        String[] minVersionArr = minVersion.split("\\.");
        for (int i = 0; i < versionArr.length; i++) {
            if (Integer.parseInt(getNumberArray(minVersionArr[i])) > Integer.parseInt(getNumberArray(versionArr[i]))) {
                return true;
            }else if(Integer.parseInt(getNumberArray(minVersionArr[i])) < Integer.parseInt(getNumberArray(versionArr[i]))){
                return false;
            }
        }
        return false;
    }

    private static String getNumberArray(String minVersionArr) {
        String returnNumber = "";
        for (int i = 0; i < minVersionArr.length(); i++) {
            if (minVersionArr.charAt(i) >= '0' && minVersionArr.charAt(i) <= '9') {
                returnNumber = returnNumber + minVersionArr.charAt(i);
            }
        }
        return returnNumber;
    }
}
