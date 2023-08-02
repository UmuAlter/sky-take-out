package com.sky.controller.admin;

import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Category;
import com.sky.entity.Dish;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.annotations.Delete;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
@Slf4j
@Api(tags = "菜品管理")
@RequestMapping("/admin/dish")
public class DishController {
    @Autowired
    private DishService dishService;
    @Autowired
    private RedisTemplate redisTemplate;
    /**
     * 新增菜品
     * @param dishDTO
     * @return
     */
    @PostMapping
    @ApiOperation("新增菜品")
    public Result save(@RequestBody DishDTO dishDTO){
        log.info("新增菜品:{}",dishDTO);

        dishService.saveWithFlavor(dishDTO);
        //清理Redis缓存
        String key = "dish_" + dishDTO.getCategoryId();
        redisTemplate.delete(key);

        return Result.success();
    }

    /**
     * 菜品分页查询
     * @param dishPageQueryDTO
     * @return
     */
    @Transactional //涉及多个表的操作，保证事物一致性
    @GetMapping("/page")
    @ApiOperation("菜品分页查询")
    public Result<PageResult> page(DishPageQueryDTO dishPageQueryDTO){
        log.info("菜品分页查询:{}",dishPageQueryDTO);

        PageResult result = dishService.pageQuery(dishPageQueryDTO);

        return Result.success(result);
    }

    /*
    前端传来的参数为String类型的id集合 1，2，3
    此处的@RequestParam注解可以让springmvc将String的参数变成 Long型 的集合
     */

    /**
     * 菜品批量删除
     * 把对应菜品关联的口味也删除
     * @param ids
     * @return
     */
    @DeleteMapping
    @ApiOperation("菜品批量删除")
    public Result delete(@RequestParam List<Long> ids){
        log.info("菜品批量删除: {}",ids);
        dishService.deleteBatch(ids);
        //可能会影响多个Key
        //删除所有以dish_开头的
        cleanRedis();

        return Result.success();
    }
  /*
  修改菜品
  */
    /**
     * 1.查询回显
     * 查询菜品和对应口味
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    @ApiOperation("根据Id查询菜品")
    public Result<DishVO> getById(@PathVariable Long id){
        log.info("根据Id查询菜品 :{}",id);
        DishVO dishVO = dishService.getByIdWithFlavor(id);
        return Result.success(dishVO);
    }

    /**
     * 2.更新菜品
     * @param dishDTO
     * @return
     */
    @PutMapping
    @ApiOperation("更新菜品")
    public Result update(@RequestBody DishDTO dishDTO){
        log.info("更新菜品");
        dishService.updateWithFlavor(dishDTO);

        cleanRedis();

        return Result.success();
    }

    /**
     * 菜品的起售停售
     * @param id
     * @param status
     * @return
     */
    @PostMapping("/status/{status}")
    @ApiOperation("菜品的起售停售")
    public Result startOrStop(Long id ,@PathVariable Integer status ){
        log.info("菜品{}的起售停售：{}",id,status);
        dishService.startOrStop(id,status);

        cleanRedis();

        return Result.success();
    }

    /**
     * 根据分类Id查询菜品
     * @param categoryId
     * @return
     */
    @GetMapping("/list")
    @ApiOperation("根据分类Id查询菜品")
    public Result<List<Dish>> selectCategoryById(Long categoryId){
        log.info("根据分类Id查询菜品:{}",categoryId);
        List<Dish> dishes = dishService.list(categoryId);
        return Result.success(dishes);
    }

    //清空所有Redis缓存
    private void cleanRedis() {

        Set keys = redisTemplate.keys("dish_*");
        redisTemplate.delete(keys);

    }
}
