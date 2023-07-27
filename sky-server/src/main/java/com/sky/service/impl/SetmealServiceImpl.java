package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.Setmeal;
import com.sky.entity.SetmealDish;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.exception.SetmealEnableFailedException;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealDishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.result.PageResult;
import com.sky.service.SetmealService;
import com.sky.vo.SetmealVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SetmealServiceImpl implements SetmealService {
    @Autowired
    private SetmealMapper setmealMapper;
    @Autowired
    private SetmealDishMapper setmealDishMapper;
    @Autowired
    private DishMapper dishMapper;

    /**
     * 新增套餐,同时需要保存套餐和菜品的关联关系
     * @param setmealDTO
     */
    @Override
    @Transactional
    public void save(SetmealDTO setmealDTO) {
        Setmeal setmeal = new Setmeal();
        BeanUtils.copyProperties(setmealDTO,setmeal);
        //新增套餐
        //新增后才有套餐id
        setmealMapper.save(setmeal);

        //新增关联关系
        //获得套餐id
        Long setmealId = setmeal.getId();
        //获得套餐与菜品联系对象
        List<SetmealDish> setmealDishes = setmealDTO.getSetmealDishes();
        //新增联系，即对联系对象操作 设置套餐Id
        setmealDishes.forEach(setmealDish -> setmealDish.setSetmealId(setmealId));

        //保存套餐和菜品的联系
        setmealDishMapper.insertBatch(setmealDishes);

    }

    /**
     * 套餐分页查询
     * @param setmealPageQueryDTO
     * @return
     */
    @Override
    public PageResult page(SetmealPageQueryDTO setmealPageQueryDTO) {

        PageHelper.startPage(setmealPageQueryDTO.getPage(),setmealPageQueryDTO.getPageSize());
        Page<SetmealVO> page = setmealMapper.page(setmealPageQueryDTO);

        return new PageResult(page.getTotal(),page.getResult());
    }

    /**
     * 删除套餐
     * @param ids
     */
    @Override
    public void deleteBatch(List<Long> ids) {
        //判断套餐是否可以删除——起售中的套餐不能删除
        ids.forEach(id->{
            //根据Id查找套餐
            Setmeal setmeal = setmealMapper.getById(id);
            if(setmeal.getStatus() == StatusConstant.ENABLE){
                throw new DeletionNotAllowedException(MessageConstant.SETMEAL_ON_SALE);
            }
        });

        //删除——套餐表，套餐菜品关系表
        ids.forEach(id ->{
            //删除套餐
            setmealMapper.deleteById(id);
            //删除套餐菜品关系
            setmealDishMapper.deleteBySetmealId(id);
        });
    }

    /**
     * 根据Id查询套餐
     * @param id
     * @return
     */
    @Override
    public SetmealVO getByIdWithDishes(Long id) {
        //根据Id查询套餐
        Setmeal setmeal = setmealMapper.getById(id);
        //根据根据套餐Id查询 套餐——菜品 关系
        List<SetmealDish> setmealDishes = setmealDishMapper.getBySetmealId(id);

        SetmealVO setmealVO = new SetmealVO();
        BeanUtils.copyProperties(setmeal,setmealVO);
        setmealVO.setSetmealDishes(setmealDishes);

        return setmealVO;
    }

    /**
     * 修改套餐
     * @param setmealDTO
     */
    @Override
    @Transactional
    public void update(SetmealDTO setmealDTO) {
        //创建一条新的纪录
        Setmeal setmeal = new Setmeal();
        BeanUtils.copyProperties(setmealDTO,setmeal);

        System.out.println("OK");

        //更新套餐
        setmealMapper.update(setmeal);

        System.out.println("OK");

        //获得套餐Id
        Long setmaelId = setmealDTO.getId();
        //删除 套餐菜品表中当前套餐的关系
        setmealDishMapper.deleteBySetmealId(setmaelId);
        //获取当前 套餐——菜品关系
        List<SetmealDish> setmealDishes = setmealDTO.getSetmealDishes();
        setmealDishes.forEach(setmealDish -> {
            //设置菜品的套餐Id
            setmealDish.setSetmealId(setmaelId);
        });
        //添加新的关系
        setmealDishMapper.insertBatch(setmealDishes);
    }

    /**
     * 设置套餐起售停售
     * @param id
     * @param status
     */
    @Override
    public void startOrStop(Long id, Integer status) {
        //起售时不能有停售菜品
        if(status == StatusConstant.ENABLE){
            //根据套餐Id获得对应菜品
            List<Dish> dishes =  dishMapper.getBySetmealId(id);
            if(dishes != null && dishes.size()>0){
                dishes.forEach(dish -> {
                    if(dish.getStatus() == StatusConstant.DISABLE){
                        throw new SetmealEnableFailedException(MessageConstant.SETMEAL_ENABLE_FAILED);
                    }
                });
            }
        }
        Setmeal setmeal = Setmeal.builder().id(id).status(status).build();
        //更新
        setmealMapper.update(setmeal);
    }
}
