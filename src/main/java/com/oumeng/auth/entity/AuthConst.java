package com.oumeng.auth.entity;

public class AuthConst {
	
	public static String getUrlKey(String userId,String url)
	{
		return userId+"*" + url;
	}
	
	public static String getUserInfoKey(String tokenId)
	{
		return tokenId+"_user";
	}

	
	public static String getAdminApplicationId()
	{
		return "system";
	}

	
}
