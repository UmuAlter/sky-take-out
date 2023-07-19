package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.PasswordConstant;
import com.sky.constant.StatusConstant;
import com.sky.context.BaseContext;
import com.sky.dto.EmployeeDTO;
import com.sky.dto.EmployeeLoginDTO;
import com.sky.dto.EmployeePageQueryDTO;
import com.sky.entity.Employee;
import com.sky.exception.AccountLockedException;
import com.sky.exception.AccountNotFoundException;
import com.sky.exception.PasswordErrorException;
import com.sky.mapper.EmployeeMapper;
import com.sky.result.PageResult;
import com.sky.service.EmployeeService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class EmployeeServiceImpl implements EmployeeService {

    @Autowired
    private EmployeeMapper employeeMapper;

    /**
     * 员工登录
     *
     * @param employeeLoginDTO
     * @return
     */
    public Employee login(EmployeeLoginDTO employeeLoginDTO) {
        String username = employeeLoginDTO.getUsername();
        String password = employeeLoginDTO.getPassword();

        //1、根据用户名查询数据库中的数据
        Employee employee = employeeMapper.getByUsername(username);

        //2、处理各种异常情况（用户名不存在、密码不对、账号被锁定）
        if (employee == null) {
            //账号不存在
            throw new AccountNotFoundException(MessageConstant.ACCOUNT_NOT_FOUND);
        }

        //密码比对
        // MD5加密
        password = DigestUtils.md5DigestAsHex(password.getBytes());

        if (!password.equals(employee.getPassword())) {
            //密码错误
            throw new PasswordErrorException(MessageConstant.PASSWORD_ERROR);
        }

        if (employee.getStatus() == StatusConstant.DISABLE) {
            //账号被锁定
            throw new AccountLockedException(MessageConstant.ACCOUNT_LOCKED);
        }

        //3、返回实体对象
        return employee;
    }

    /**
     * 新增员工
     * 此时需要将实体DTO类转化为普通实体类方便存储
     * @param employeeDTO
     */
    @Override
    public void addEmp(EmployeeDTO employeeDTO) {
        Employee employee = new Employee();
        //属性对象拷贝  employeeDTO--》employee  属性名一致才会拷贝
        BeanUtils.copyProperties(employeeDTO,employee);

        //设置初始状态 1正常
        employee.setStatus(StatusConstant.ENABLE);
        //密码进行MD5加密后存储 默认密码为123456
        employee.setPassword(DigestUtils.md5DigestAsHex(PasswordConstant.DEFAULT_PASSWORD.getBytes()));

        employee.setCreateTime(LocalDateTime.now());
        employee.setUpdateTime(LocalDateTime.now());
        //获取存在局部变量中的id
        employee.setCreateUser(BaseContext.getCurrentId());
        employee.setUpdateUser(BaseContext.getCurrentId());

        employeeMapper.insert(employee);
    }

    /**
     * 分页查询
     * @param pageQueryDTO
     * @return
     */
    @Override
    public PageResult page(EmployeePageQueryDTO pageQueryDTO) {
        //通过PageHelper进行分页查询
        PageHelper.startPage(pageQueryDTO.getPage(),pageQueryDTO.getPageSize());

        Page<Employee> page =  employeeMapper.pageQurry(pageQueryDTO);

        long total = page.getTotal();//获得分页总数
        List<Employee> recodes = page.getResult();  //获得查询记录

        return new PageResult(total,recodes);
    }

    /**
     * 启用禁用员工权限
     * @param status
     * @param id
     */
    @Override
    public void startOrStop(Integer status, Long id) {
        //来源于@Builder方法
        //对employeeMapper传入 employee 实体类
        Employee employee = Employee.builder()
                .status(status).id(id)
                .build();
        //动态更新
        employeeMapper.update(employee);
    }

}
