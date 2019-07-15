package com.imooc.miaosha.controller;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.imooc.miaosha.dao.OrderDao;
import com.imooc.miaosha.domain.MiaoshaOrder;
import com.imooc.miaosha.domain.MiaoshaUser;
import com.imooc.miaosha.domain.OrderInfo;
import com.imooc.miaosha.redis.RedisService;
import com.imooc.miaosha.result.CodeMsg;
import com.imooc.miaosha.result.Result;
import com.imooc.miaosha.service.GoodsService;
import com.imooc.miaosha.service.MiaoshaService;
import com.imooc.miaosha.service.MiaoshaUserService;
import com.imooc.miaosha.service.OrderService;
import com.imooc.miaosha.vo.GoodsVo;
import com.imooc.miaosha.vo.OrderDetailVo;

@Controller
@RequestMapping("/order")
public class OrderController {
	
	@Autowired
	MiaoshaUserService userService;
	
	@Autowired
	RedisService redisService;
	
	@Autowired
	GoodsService goodsService;
	
	@Autowired
	OrderService orderService;

	
	@RequestMapping("/detail")
	@ResponseBody
	public Result<OrderDetailVo> list(MiaoshaUser user,@RequestParam("orderId") long orderId) {
		if(user==null) {
			return Result.error(CodeMsg.SESSION_ERROR);
		}
		
		OrderInfo orderInfo=orderService.getOrderById(orderId);
		if(orderInfo==null) {
			return Result.error(CodeMsg.ORDER_NOT_EXIST);
		}
		long goodsId=orderInfo.getGoodsId();
		GoodsVo goodsVo=goodsService.getGoodsVoByGoodsId(goodsId);
		
		
		
		OrderDetailVo vo=new OrderDetailVo();
		vo.setGoods(goodsVo);
		vo.setOrder(orderInfo);
		
		
		return Result.success(vo);
	
	}
	
	

}
