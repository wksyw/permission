package com.oumeng.auth.entity;

import java.util.List;

public class AuthConst {
	
	public static String getUrlKey(String tokenId,String url)
	{
		return tokenId+"*" + url;
	}
	public static String getAdminRoleInnerName()
	{
		return "admin";
	}
	
	public static String getUserInfoKey(String tokenId)
	{
		return tokenId+"_user";
	}

	public static String getUserIdExpiredKey(String userId)
	{
		return userId+"_userId_expired";
	}

	public static String getUserPermissionKey(String tokenId)
	{
		return tokenId+"_permission";
	}
	
	public static String getAdminApplicationId()
	{
		return "system";
	}
	
	/**
	 * 
	 * @param tokenId
	 * @return
	 */
	public static String getErrorPasswordKey(String tokenId)
	{
		return tokenId+"_errorPassword";
	}
	/**
	 * 权限能够放行
	 * @return
	 */
	public static String permitCanAccess()
	{
		return String.valueOf(UserPermission.LEVEL_ENABLE);
	}
	
	public static boolean isRoleById(List<Role> roles,String roldId)
	{
		try {
			StringBuilder ls = new StringBuilder();
			ls.append(",");
			for(Role role:roles)
			{
				ls.append(role.getRoleId());
				ls.append(",");
			}
			return ls.toString().contains(","+roldId + ",");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return false;
	}
	
	public static boolean isRoleByInnerName(List<Role> roles,String roleName)
	{
		try {
			StringBuilder ls = new StringBuilder();
			ls.append(",");
			for(Role role:roles)
			{
				ls.append(role.getRole());
				ls.append(",");
			}
			return ls.toString().contains(","+roleName + ",");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return false;
	}
	
}
