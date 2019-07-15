package com.imooc.miaosha.controller;


import java.util.List;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.imooc.miaosha.domain.MiaoshaOrder;
import com.imooc.miaosha.domain.MiaoshaUser;
import com.imooc.miaosha.domain.OrderInfo;
import com.imooc.miaosha.rabbitmq.MQSender;
import com.imooc.miaosha.rabbitmq.MiaoshaMessage;
import com.imooc.miaosha.redis.GoodsKey;
import com.imooc.miaosha.redis.RedisService;
import com.imooc.miaosha.result.CodeMsg;
import com.imooc.miaosha.result.Result;
import com.imooc.miaosha.service.GoodsService;
import com.imooc.miaosha.service.MiaoshaService;
import com.imooc.miaosha.service.MiaoshaUserService;
import com.imooc.miaosha.service.OrderService;
import com.imooc.miaosha.vo.GoodsVo;



@Controller
@RequestMapping("/miaosha")
public class MiaoshaController implements InitializingBean{
	
	@Autowired
	MiaoshaUserService userService;
	
	@Autowired
	RedisService redisService;
	
	@Autowired
	GoodsService goodsService;
	
	@Autowired
	OrderService orderService;
	
	@Autowired
	MiaoshaService miaoshaService;
	
	@Autowired
	MQSender sender;
	
	/*
	 * 
	 * 系统初始化
	 */
	@Override
	public void afterPropertiesSet() throws Exception {
		List<GoodsVo> goodsList=goodsService.listGoodsVo();
		if(goodsList==null) {
			return;
		}
		for(GoodsVo goods: goodsList) {
			redisService.set(GoodsKey.getMiaoshaGoodsStock, ""+goods.getId(), goods.getStockCount());
		}
		
	}
	
	@RequestMapping(value="/do_miaosha",method=RequestMethod.POST)
	@ResponseBody
	public Result<Integer> list(Model model,MiaoshaUser user,@RequestParam("goodsId") long goodsId) {
		model.addAttribute("user",user);
		if(user==null) {
			return Result.error(CodeMsg.SESSION_ERROR);
		}
		
		//redis预减库存
		long stock =redisService.decr(GoodsKey.getMiaoshaGoodsStock, ""+goodsId);
		if(stock<0) {
			return Result.error(CodeMsg.MIAO_SHA_OVER);
		}
		//判断之前有没有秒杀成功过
		MiaoshaOrder order= orderService.getMiaoshaOrderByUserIdGoodsId(user.getId(),goodsId);
		if(order!=null) {
			return Result.error(CodeMsg.REPEATE_MIAOSHA);
		}
		//入队
		MiaoshaMessage mm=new MiaoshaMessage();
		mm.setGoodsId(goodsId);
		mm.setUser(user);
		sender.sendMiaoshaMessage(mm);
		return Result.success(0);
		
		
//		//判断库存
//		GoodsVo goods=goodsService.getGoodsVoByGoodsId(goodsId);
//		int stock=goods.getStockCount();
//		if(stock<=0) {
//			//库存不足
//			return Result.error(CodeMsg.MIAO_SHA_OVER);
//		}
//		
//		//判断之前有没有秒杀成功过
//		MiaoshaOrder order= orderService.getMiaoshaOrderByUserIdGoodsId(user.getId(),goodsId);
//		if(order!=null) {
//			return Result.error(CodeMsg.REPEATE_MIAOSHA);
//		}
//		
//		//减库存 生成订单
//		OrderInfo orderInfo=miaoshaService.miaosha(user,goods);
//		return Result.success(orderInfo);
	
	}

	
	
	

}
