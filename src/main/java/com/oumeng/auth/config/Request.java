package com.oumeng.auth.config;

import com.oumeng.auth.entity.AuthConst;
import com.oumeng.auth.entity.User;
import com.oumeng.auth.utils.JsonUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

@Component
public class Request {
    @Autowired
    private HttpServletRequest request;

    public User getLoginUser() {
        String requestToken = request.getHeader("token");
        String userStr = (String) request.getSession().getAttribute(AuthConst.getUserInfoKey(requestToken));
        User user = JsonUtil.fromJson(userStr, User.class);
        return user;
    }

}
