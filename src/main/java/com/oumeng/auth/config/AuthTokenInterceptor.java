package com.oumeng.auth.config;

import com.oumeng.auth.entity.AuthConst;
import com.oumeng.auth.entity.GeneralInsertParam;
import com.oumeng.auth.entity.User;
import com.oumeng.auth.entity.UserPermission;
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
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

/**
 * shiro token 拦截器，负责对鉴权等业务进行拦截
 *
 * @author helms
 */
@Service("authShiroTokenInterceptor")
public class AuthTokenInterceptor implements HandlerInterceptor, InitializingBean {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final int ErrorNeedToken = 3008;
    private final String ErrorMsgNeedToken = "用户没有登录";

    private final int ErrorNoPermission = 2008;
    private final String ErrorMsgNoPermission = "没有权限操作";

    @Value("${not.interceptor.url:/*}")
    private String notInterceptorUrl;

    @Value("${not.permission.url:/*}")
    private String notPermissionUrl;

    @Autowired
    protected StringRedisTemplate stringRedisTemplate;

    @Autowired
    private GeneralDao generalDao;

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
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        // TODO Auto-generated method stub
        try {
            String requestUrl = request.getRequestURI();
            requestUrl = requestUrl.replace(request.getContextPath(), "");
            String leftUrl = requestUrl.substring(0, requestUrl.indexOf("/", 1));
            String rightUrl = requestUrl.substring(requestUrl.indexOf("/", 1));
            String[] notInterceptorUrlArr = notInterceptorUrl.split(",");
            boolean notInterceptor = false;
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
            if (!notInterceptor) {
                HttpSession httpSession = request.getSession();
                String requestToken = request.getHeader("token");
                if (requestToken == null) {
                    needAuthAccess(response, ErrorNeedToken, ErrorMsgNeedToken);
                    return false;
                }
                String userStr = (String) request.getSession().getAttribute(AuthConst.getUserInfoKey(requestToken));
                User user = JsonUtil.fromJson(userStr, User.class);
                if (user == null) {
                    needAuthAccess(response, ErrorNeedToken, ErrorMsgNeedToken);
                    return false;
                }
                String flag = stringRedisTemplate.opsForValue().get(AuthConst.getUserIdExpiredKey(user.getUserId() + ""));
                if ("true".equals(flag)) {
                    needAuthAccess(response, ErrorNeedToken, ErrorMsgNeedToken);
                    return false;
                }
                //如果角色是admin
                if (user.getIsAdmin() == 1) {
                    insertLoginLog(request,requestUrl,user,1);
                    return HandlerInterceptor.super.preHandle(request, response, handler);
                }
                String[] notPermissionUrlArr = notPermissionUrl.split(",");
                boolean notPermission = false;
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
                if(!notPermission){
                    String permission = (String) httpSession.getAttribute(AuthConst.getUrlKey(requestToken, requestUrl));
                    if (!AuthConst.permitCanAccess().equals(permission)) {
                        insertLoginLog(request,requestUrl,user,2);
                        needAuthAccess(response, ErrorNoPermission, ErrorMsgNoPermission);
                        return false;
                    }
                }
                insertLoginLog(request,requestUrl,user,2);
            }
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            logger.error("", e);
            needAuthAccess(response, -1, "exception error");
            return false;
        }
        return HandlerInterceptor.super.preHandle(request, response, handler);
    }

    private void insertLoginLog(HttpServletRequest request,String requestUrl,User user,int success){
        String permissionStr = (String) request.getSession().getAttribute(AuthConst.getUrlKey(request.getHeader("token"), requestUrl)+"_permission");
        UserPermission userPermission = JsonUtil.fromJson(permissionStr, UserPermission.class);
        if(userPermission.getAction()!=null && userPermission.getObject()!=null){
            Map<String,Object> data = new HashMap<>();
            data.put("result",success);
            String ip = ClientUtil.getIpAddr(request);
            data.put("ip",ip);
            boolean checkAgentIsMobile = ClientUtil.checkAgentIsMobile(request);
            if(checkAgentIsMobile){
                data.put("deviceType",1);
            }else {
                data.put("deviceType",2);
            }
            String ua = request.getHeader("user-agent");
            data.put("platform",ua);
            String imei = request.getHeader("imei");
            data.put("imei",imei);
            data.put("loginName",user.getLoginName());
            String permissionId = userPermission.getPermissionId();
            if(permissionId.startsWith("001")){
                data.put("type",1);
            }else {
                data.put("type",2);
            }
            data.put("url",requestUrl);
            data.put("action",userPermission.getAction());
            data.put("object",userPermission.getObject());
            generalDao.insert("oumengauthdb.tb_log",data);
        }
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
