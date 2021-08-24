package com.oumeng.auth.config;

import com.oumeng.auth.entity.AuthConst;
import com.oumeng.auth.entity.User;
import com.oumeng.auth.utils.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

@Component
public class Request {
    @Autowired
    private HttpServletRequest request;

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    protected StringRedisTemplate stringRedisTemplate;

    public User getLoginUser() {
        String requestToken = request.getHeader("token");
        String userStr = stringRedisTemplate.opsForValue().get(AuthConst.getUserInfoKey(requestToken));
        logger.info("getLoginUser "+requestToken+" "+userStr);
        User user = JsonUtil.fromJson(userStr, User.class);
        return user;
    }

}
