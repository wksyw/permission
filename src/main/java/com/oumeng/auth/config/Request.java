package com.oumeng.auth.config;

import com.oumeng.auth.entity.AuthConst;
import com.oumeng.auth.entity.User;
import com.oumeng.auth.utils.JsonUtil;
import com.oumeng.auth.utils.ProcessResult;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Component
public class Request {
    @Autowired
    private HttpServletRequest request;

    @Value("${authentication.url:http://192.168.13.17:8090}")
    private String authUrl;

    @Autowired(required = false)
    private RestTemplate restTemplate;

    @Autowired
    protected StringRedisTemplate stringRedisTemplate;

    public User getLoginUser(String token) {
        if(token==null){
            return null;
        }
        String userStr;
        if(token.contains("-")){
            String userInfoKey = stringRedisTemplate.opsForValue().get(token);
            if(userInfoKey==null){
                return null;
            }
            userStr = stringRedisTemplate.opsForValue().get(AuthConst.getUserInfoKey(userInfoKey));
            stringRedisTemplate.opsForValue().set(token, userInfoKey, Long.parseLong(stringRedisTemplate.opsForValue().get("tokenTimeOutTime")), TimeUnit.SECONDS);
        }else {
            userStr = stringRedisTemplate.opsForValue().get(AuthConst.getUserInfoKey(token));
        }
        User user = JsonUtil.fromJson(userStr, User.class);
        return user;
    }

    public User getLoginUser() {
        String requestToken = request.getHeader("token");
        return getLoginUser(requestToken);
    }

    public User getUser(int userId) {
        String userInfoKey = DigestUtils.md5Hex(userId+"");
        String userStr = stringRedisTemplate.opsForValue().get(AuthConst.getUserInfoKey(userInfoKey));
        if(userStr!=null && !userStr.equals("")){
            return JsonUtil.fromJson(userStr, User.class);
        }
        User user = new User();
        user.setUserId(userId);
        HttpHeaders headers = new HttpHeaders();
        headers.add("token", request.getHeader("token"));
        HttpEntity<User> formEntity = new HttpEntity<>(user, headers);
        ProcessResult getUserResult = restTemplate.postForObject(authUrl+"/userInfo/getUserByUserId", formEntity, ProcessResult.class);
        String userStrDb = JsonUtil.toJson(getUserResult.getResponseInfo());
        if(userStrDb!=null){
            stringRedisTemplate.opsForValue().set(AuthConst.getUserInfoKey(userInfoKey),userStrDb);
        }
        return JsonUtil.fromJson(userStrDb, User.class);
    }

    public String getSubUserStrList(int userId) {
        String redisKey = "getSubUserStrList:"+userId;
        String getSubUserStrList = stringRedisTemplate.opsForValue().get(redisKey);
        if(getSubUserStrList!=null){
            return getSubUserStrList;
        }
        Map<String, Object> hashMap = new HashMap<String, Object>();
        hashMap.put("userId",userId);
        Object getUserResult = restTemplate.postForObject(authUrl+"/user/getSubUserStrList", hashMap, ProcessResult.class).getResponseInfo();
        if(getUserResult==null){
            getSubUserStrList = "";
        }else {
            getSubUserStrList = getUserResult+"";
        }
        stringRedisTemplate.opsForValue().set(redisKey,getSubUserStrList);
        return getSubUserStrList;
    }

    public boolean isLeader(int leader,int userId) {
        return (leader+"").equals(getUser(userId).getLeader());
    }
}
