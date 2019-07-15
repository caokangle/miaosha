package com.imooc.miaosha.controller;

import java.util.List;
import java.util.concurrent.locks.ReentrantLock;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.websocket.server.PathParam;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.thymeleaf.spring4.context.SpringWebContext;
import org.thymeleaf.spring4.view.ThymeleafViewResolver;

import com.alibaba.druid.util.StringUtils;
import com.imooc.miaosha.domain.MiaoshaUser;
import com.imooc.miaosha.redis.GoodsKey;
import com.imooc.miaosha.redis.RedisService;
import com.imooc.miaosha.result.Result;
import com.imooc.miaosha.service.GoodsService;
import com.imooc.miaosha.service.MiaoshaUserService;
import com.imooc.miaosha.vo.GoodsDetailVo;
import com.imooc.miaosha.vo.GoodsVo;


@Controller
@RequestMapping("/goods")
public class GoodsController {

	
	@Autowired
	MiaoshaUserService userService;
	
	@Autowired
	RedisService redisService;
	
	@Autowired
	GoodsService goodsService;
	
	@Autowired
	ThymeleafViewResolver thymeleafViewResolver;
	
	@Autowired
	ApplicationContext applicationContext;
	
	@RequestMapping(value="/to_list",produces="text/html")
	@ResponseBody
	public String list(HttpServletRequest request,HttpServletResponse response,Model model,MiaoshaUser user) {
		model.addAttribute("user", user);
	//取缓存
	String html=redisService.get(GoodsKey.getGoodsList,"", String.class);
	if(!StringUtils.isEmpty(html)) {
		return html;
	}
	
		//查询商品列表
		List<GoodsVo> goodsList=goodsService.listGoodsVo();
		model.addAttribute("goodsList",goodsList);
		//手动渲染
		SpringWebContext ctx=new SpringWebContext(request,response,request.getServletContext(),request.getLocale(),model.asMap(),applicationContext);
		html=thymeleafViewResolver.getTemplateEngine().process("goods_list", ctx);
		if(!StringUtils.isEmpty(html)) {
			//存储到redis
			redisService.set(GoodsKey.getGoodsList, "", html);
		}
		return html;
	}
	
	@RequestMapping(value="/to_detail/{goodsId}",produces="text/html")
	@ResponseBody
	public String detail(HttpServletRequest request,HttpServletResponse response,Model model,MiaoshaUser user,@PathVariable("goodsId")long goodsId) {
		model.addAttribute("user", user);
		GoodsVo goods=goodsService.getGoodsVoByGoodsId(goodsId);
		model.addAttribute("goods", goods);
		//取缓存
    	String html = redisService.get(GoodsKey.getGoodsDetail, ""+goodsId, String.class);
    	if(!StringUtils.isEmpty(html)) {
    		return html;
    	}
    	
		long startAt = goods.getStartDate().getTime();
    	long endAt = goods.getEndDate().getTime();
    	long now = System.currentTimeMillis();
    	
    	int miaoshaStatus = 0;
    	int remainSeconds = 0;
    	
    	if(now<startAt) {
    		//还没开始秒杀活动
    		miaoshaStatus = 0;
    		remainSeconds = (int) ((startAt-now)/1000);
    	}else if(now>endAt){
    		//秒杀已经结束
    		miaoshaStatus = 2;
    		remainSeconds = -1;
		}else {
			miaoshaStatus = 1;
			remainSeconds = 0;
		}
    	
    	model.addAttribute("miaoshaStatus", miaoshaStatus);
    	model.addAttribute("remainSeconds", remainSeconds);
    	
    	SpringWebContext ctx = new SpringWebContext(request,response,
    			request.getServletContext(),request.getLocale(), model.asMap(), applicationContext );
    	html = thymeleafViewResolver.getTemplateEngine().process("goods_detail", ctx);
    	if(!StringUtils.isEmpty(html)) {
    		redisService.set(GoodsKey.getGoodsDetail, ""+goodsId, html);
    	}
    	return html;
	}
	
	@RequestMapping(value="/detail/{goodsId}")
	@ResponseBody
	public Result<GoodsDetailVo> detail2(HttpServletRequest request,HttpServletResponse response,Model model,MiaoshaUser user,@PathVariable("goodsId")long goodsId) {
		GoodsVo goods=goodsService.getGoodsVoByGoodsId(goodsId);
	
		long startAt = goods.getStartDate().getTime();
    	long endAt = goods.getEndDate().getTime();
    	long now = System.currentTimeMillis();
    	
    	int miaoshaStatus = 0;
    	int remainSeconds = 0;
    	
    	if(now<startAt) {
    		//还没开始秒杀活动
    		miaoshaStatus = 0;
    		remainSeconds = (int) ((startAt-now)/1000);
    	}else if(now>endAt){
    		//秒杀已经结束
    		miaoshaStatus = 2;
    		remainSeconds = -1;
		}else {
			miaoshaStatus = 1;
			remainSeconds = 0;
		}
    	
    	GoodsDetailVo vo=new GoodsDetailVo();
    	vo.setGoods(goods);
    	vo.setMiaoshaStatus(miaoshaStatus);
    	vo.setRemainSeconds(remainSeconds);
    	vo.setUser(user);
    
    	return Result.success(vo);
	}
}
