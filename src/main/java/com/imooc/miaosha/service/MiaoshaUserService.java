package com.imooc.miaosha.service;

import static org.assertj.core.api.Assertions.useRepresentation;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alibaba.druid.util.StringUtils;
import com.imooc.miaosha.dao.MiaoshaUserDao;
import com.imooc.miaosha.domain.MiaoshaUser;
import com.imooc.miaosha.exception.GlobalException;
import com.imooc.miaosha.redis.MiaoshaUserKey;
import com.imooc.miaosha.redis.RedisService;
import com.imooc.miaosha.result.CodeMsg;
import com.imooc.miaosha.util.MD5Util;
import com.imooc.miaosha.util.UUIDUtil;
import com.imooc.miaosha.vo.LoginVo;

@Service
public class MiaoshaUserService {
	
	public static final String COOKI_NAME_TOKEN = "token";
	
	@Autowired
	MiaoshaUserDao miaoshaUserDao;
	
	@Autowired
	RedisService redisService;
	
	public MiaoshaUser getById(long id) {
		//取缓存
		MiaoshaUser user=redisService.get(MiaoshaUserKey.getById, ""+id, MiaoshaUser.class);
		if(user!=null) {
			return user;
		}
		//取数据库
		user=miaoshaUserDao.getById(id);
		if(user!=null) {
			redisService.set(MiaoshaUserKey.getById, ""+id, user);
		}
		
		return user;
	}
	
	/*
	 * 对象级缓存
	 */
	public boolean updatepassword(String token,long id,String formPass) {
		//取缓存
		MiaoshaUser user=getById(id);
		if(user==null) {
			throw new GlobalException(CodeMsg.MOBILE_NOT_EXIST);
		}
		//更新数据库
		MiaoshaUser toBeupdate=new MiaoshaUser();
		toBeupdate.setId(id);
		toBeupdate.setPassword(MD5Util.formPassToDBPass(formPass, user.getSalt()));
		miaoshaUserDao.update(toBeupdate);
		//处理缓存
		redisService.delete(MiaoshaUserKey.getById, ""+id);
		user.setPassword(toBeupdate.getPassword());
		redisService.set(MiaoshaUserKey.token, token, user);
		return true;
	}
	
	public MiaoshaUser getByToken(HttpServletResponse response,String token) {
		if(StringUtils.isEmpty(token)) {
			return null;
		}
		MiaoshaUser user=redisService.get(MiaoshaUserKey.token, token, MiaoshaUser.class);
		if(user!=null) {
			addCookie(response, token, user);
		}
		return user;
	}
	
	public boolean login(HttpServletResponse response, LoginVo loginVo) {
		if(loginVo == null) {
			throw new GlobalException(CodeMsg.SERVER_ERROR);
		}
		String mobile = loginVo.getMobile();
		String formPass = loginVo.getPassword();
		//判断手机号是否存在
		MiaoshaUser user = getById(Long.parseLong(mobile));
		if(user == null) {
			throw new GlobalException(CodeMsg.MOBILE_NOT_EXIST);
		}
		//验证密码
		String dbPass = user.getPassword();
		String saltDB = user.getSalt();
		String calcPass = MD5Util.formPassToDBPass(formPass, saltDB);
		if(!calcPass.equals(dbPass)) {
			throw new GlobalException(CodeMsg.PASSWORD_ERROR);
		}
		//生成cookie
		String token	 = UUIDUtil.uuid();
		addCookie(response, token, user);
		return true;
	}
	
	private void addCookie(HttpServletResponse response,String token,MiaoshaUser user) {
		redisService.set(MiaoshaUserKey.token, token, user);
		Cookie cookie=new Cookie(COOKI_NAME_TOKEN,token );
		cookie.setMaxAge(MiaoshaUserKey.TOKEN_EXPIRE);
		cookie.setPath("/");
		response.addCookie(cookie);
		
		
	}

}
