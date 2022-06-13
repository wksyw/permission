package com.oumeng.auth.config;

import com.oumeng.auth.entity.AuthConst;
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

    private final int ErrorNoPermission = 2008;
    private final String ErrorMsgNoPermission = "没有权限操作";

    public static final int ERROR_USER_STATUS_LOCK = 20002;
    public static final String ERROR_USER_STATUS_LOCK_MSG = "用户已被停用";

    @Value("${not.interceptor.url:}")
    private String notInterceptorUrl;

    @Value("${not.permission.url:}")
    private String notPermissionUrl;

    @Value("${not.interceptor.token:}")
    private String notInterceptorToken;

    @Autowired
    protected StringRedisTemplate stringRedisTemplate;

    @Autowired
    private DataLogUtil dataLogUtil;

    @Autowired
    private Request request;

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
        String requestUrl ="";
        try {
            boolean notInterceptor = false;
            requestUrl = httpServletRequest.getRequestURI();
            logger.info("AuthTokenInterceptor: "+requestUrl);
            requestUrl = requestUrl.replace(httpServletRequest.getContextPath(), "");
            int urlIndex = requestUrl.indexOf("/", 1);
            String requestToken = httpServletRequest.getHeader("token");
            if(!notInterceptorToken.equals("") && notInterceptorToken.equals(requestToken)){
                return HandlerInterceptor.super.preHandle(httpServletRequest, response, handler);
            }
            if(urlIndex==-1){
                return HandlerInterceptor.super.preHandle(httpServletRequest, response, handler);
            }
            String leftUrl = requestUrl.substring(0, urlIndex);
            String rightUrl = requestUrl.substring(urlIndex);
            if(!notInterceptorUrl.equals("")){
                String[] notInterceptorUrlArr = notInterceptorUrl.split(",");
                for (String url : notInterceptorUrlArr) {
                    if ("/*".equals(url)) {
                        notInterceptor = true;
                        break;
                    }
                    String notInterceptorLeftUrl = url.substring(0, url.indexOf("/", 1));
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
                    }
                }
            }
            User user = request.getLoginUser();
            if (!notInterceptor) {
                if (requestToken == null) {
                    needAuthAccess(response, ErrorNeedToken, ErrorMsgNeedToken);
                    return false;
                }
                if (user == null) {
                    needAuthAccess(response, ErrorNeedToken, ErrorMsgNeedToken);
                    return false;
                }
                if (user.getStatus()==1) {
                    needAuthAccess(response, ERROR_USER_STATUS_LOCK, ERROR_USER_STATUS_LOCK_MSG);
                    return false;
                }
                boolean notPermission = false;
                if(notPermissionUrl!=null && !notPermissionUrl.equals("")){
                    String[] notPermissionUrlArr = notPermissionUrl.split(",");
                    for (String url : notPermissionUrlArr) {
                        if ("/*".equals(url)) {
                            notPermission = true;
                            break;
                        }
                        String notPermissionLeftUrl = url.substring(0, url.indexOf("/", 1));
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
                        }
                    }
                }
                //如果角色是admin
                if (user.getIsAdmin() == 1) {
                    if(!notPermission){
                        dataLogUtil.insertLog(httpServletRequest,requestUrl,user,1);
                    }
                    return HandlerInterceptor.super.preHandle(httpServletRequest, response, handler);
                }
                if(!notPermission){
                    String permission = stringRedisTemplate.opsForValue().get((AuthConst.getUrlKey(user.getUserId()+"", requestUrl)));
                    if ("255".equals(permission)) {
                        dataLogUtil.insertLog(httpServletRequest,requestUrl,user,2);
                        needAuthAccess(response, ErrorNoPermission, ErrorMsgNoPermission);
                        return false;
                    }else{
                        dataLogUtil.insertLog(httpServletRequest,requestUrl,user,1);
                    }
                }
            }else {
                if(user!=null && user.getStatus()==1){
                    needAuthAccess(response, ERROR_USER_STATUS_LOCK, ERROR_USER_STATUS_LOCK_MSG);
                    return false;
                }
            }
        } catch (Throwable e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            logger.error("requestUri="+requestUrl, e);
            needAuthAccess(response, -1, "exception error");
            return false;
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
}
