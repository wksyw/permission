package com.oumeng.auth.config;

import com.oumeng.auth.entity.AuthConst;
import com.oumeng.auth.entity.GeneralQueryParam;
import com.oumeng.auth.entity.User;
import com.oumeng.auth.entity.UserPermission;
import com.oumeng.auth.utils.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class DataLogUtil {

    @Autowired(required = false)
    private GeneralDao generalDao;

    private static final Logger logger = LoggerFactory.getLogger(DataLogUtil.class);

    @Autowired
    protected StringRedisTemplate stringRedisTemplate;

    @Async
    public void insertLog(HttpServletRequest request, String requestUrl, User user, int success){
        try {
            //String permissionStr = stringRedisTemplate.opsForValue().get(AuthConst.getUrlKey(request.getHeader("token"), requestUrl)+"_permission");
            GeneralQueryParam generalQueryParam = new GeneralQueryParam();
            generalQueryParam.setTableName("tb_permission");
            generalQueryParam.setCondition("url='"+requestUrl+"'");
            List<Map<String, Object>> permission =  generalDao.query("*","tb_permission","url='"+requestUrl+"'");
            if(permission.size()!=0){
                String permissionStr = JsonUtil.toJson(permission.get(0));
                if(permissionStr!=null){
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
                        String name = user.getDisplayName();
                        data.put("name",name);
                        String imei = request.getHeader("imei");
                        data.put("imei",imei);
                        data.put("loginName",user.getLoginName());
                        String permissionId = userPermission.getPermissionId();
                        if(permissionId.startsWith("001")){
                            data.put("type",2);
                        }else {
                            data.put("type",3);
                        }
                        data.put("url",requestUrl);
                        data.put("action",userPermission.getAction());
                        data.put("object",userPermission.getObject());
                        generalDao.insert("tb_log",data);
                    }
                }
            }
        }catch (Exception e){
            e.printStackTrace();
            logger.error("", e);
        }
    }
}
