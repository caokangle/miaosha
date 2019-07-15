package com.imooc.miaosha.service;



import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.imooc.miaosha.dao.UserDao;
import com.imooc.miaosha.domain.User;


@Service
public class UserService {
	@Autowired
	public UserDao userDao;
	
	public User getById(int id ) {
		return userDao.getById(id);
	}
	
	
	//事务测试
	@Transactional
	public  boolean tx() {
		User u1 =new User();
		u1.setId(2);
		u1.setName("王海峰");
		userDao.insert(u1);
		

		User u2= new User();
		u2.setId(1);
		u2.setName("sb");
		userDao.insert(u2);
		return true;
	}

}
