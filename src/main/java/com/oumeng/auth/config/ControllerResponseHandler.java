package com.oumeng.auth.config;

import com.oumeng.auth.entity.Response;
import com.oumeng.auth.utils.JsonUtil;
import com.oumeng.auth.utils.ProcessResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import javax.servlet.http.HttpServletRequest;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

@RestControllerAdvice
public class ControllerResponseHandler implements ResponseBodyAdvice<Object> {

	private static Map<String,String[]> paramData = new HashMap<>();

	private static Set<String> paramUrls;

	public static String interceptorUrl = "/useradmin/exportUser";

	static {
		paramData.put("/user/login",new String[]{"DL","DL","T_001"});

		paramData.put("/useradmin/addUser",new String[]{"YHGL_HTZH","XZ","T_023"});
		//paramData.put("/useradmin/searchUser",new String[]{"YHGL_HTZH","CX","T_002"});
		paramData.put("/useradmin/exportUser",new String[]{"YHGL_HTZH","DC","T_003"});
		paramData.put("/useradmin/updateUser",new String[]{"YHGL_HTZH","BJ","T_019"});

		paramData.put("/useradmin/enableBeforeUser",new String[]{"YHGL_QDYH","QY","T_027"});
		paramData.put("/useradmin/enableAfterUser",new String[]{"YHGL_HTZH","QY","T_027"});
		paramData.put("/useradmin/disableBeforeUser",new String[]{"YHGL_QDYH","TY","T_028"});
		paramData.put("/useradmin/disableAfterUser",new String[]{"YHGL_HTZH","TY","T_028"});

		paramData.put("/useradmin/addRole",new String[]{"YHGL_HTJS","XZ","T_021"});
		paramData.put("/useradmin/deleteRole",new String[]{"YHGL_HTJS","SC","T_020"});
		paramData.put("/useradmin/updateRole",new String[]{"YHGL_HTJS","BJ","T_019"});
		paramData.put("/useradmin/updateRolePermission",new String[]{"YHGL_HTJS","QXPZ","T_022"});

		paramUrls = paramData.keySet();
	}

	@Override
	public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
		return true;
	}

	@Autowired
	private RestTemplate restTemplate;

	@Autowired
	private HttpServletRequest httpServletRequest;

	@Value("${logRequestUrl:}")
	private String logRequestUrl;

	private final Logger logger = LoggerFactory.getLogger(getClass());

	@Override
	public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
								  Class<? extends HttpMessageConverter<?>> selectedConverterType, ServerHttpRequest request,
								  ServerHttpResponse response) {
		try {
			String url = httpServletRequest.getRequestURI();
			url = url.replace(httpServletRequest.getContextPath(), "");
			sendLog(url,body);
		}catch (Exception e){
			e.printStackTrace();
			logger.error("", e);
		}
		return body;
	}

	public void sendLog(String url,Object body){
		try {
			if(logRequestUrl!=null && !logRequestUrl.equals("") && paramUrls.contains(url)){
				String token = httpServletRequest.getHeader("token");
				Map<String, Object> hashMap = new HashMap<String, Object>();
				if (body instanceof ProcessResult) {
					ProcessResult processResult = (ProcessResult) body;
					if (processResult.getResult() == 0) {
						if(token==null){
							Object responseInfo = processResult.getResponseInfo();
							if(responseInfo!=null){
								token = (String) ((Map)JsonUtil.fromJson(JsonUtil.toJson(responseInfo),Map.class)).get("token");
								String logParam = processResult.getSign();
								if(logParam!=null){
									hashMap.put("logParam",logParam);
								}
							}
						}
					}
				}
				String operationModule = paramData.get(url)[0];
				String operationType = paramData.get(url)[1];
				String descType = paramData.get(url)[2];
				HttpHeaders headers = new HttpHeaders();
				headers.add("Content-Type","application/json");
				headers.add("token",token);
				headers.add("version","V1.0.1");
				hashMap.put("operationModule",operationModule);
				hashMap.put("operationType",operationType);
				hashMap.put("descType",descType);
				logger.info("sendLog url:" + url);
				logger.info("sendLog data:" + JsonUtil.toJson(hashMap));
				HttpEntity requestEntity =new HttpEntity(hashMap,headers);
				Response sendLogResponse = restTemplate.postForObject(logRequestUrl,requestEntity, Response.class);
				logger.info("sendLog response:" + sendLogResponse.getMsg());
			}
		}catch (Exception e){
			e.printStackTrace();
			logger.error("", e);
		}
	}
}