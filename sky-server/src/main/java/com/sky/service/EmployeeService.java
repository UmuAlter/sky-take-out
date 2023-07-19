package com.sky.service;

import com.sky.dto.EmployeeDTO;
import com.sky.dto.EmployeeLoginDTO;
import com.sky.dto.EmployeePageQueryDTO;
import com.sky.entity.Employee;
import com.sky.result.PageResult;

public interface EmployeeService {

    /**
     * 员工登录
     * @param employeeLoginDTO
     * @return
     */
    Employee login(EmployeeLoginDTO employeeLoginDTO);

    /**
     * 添加员工
     * @param employeeDTO
     */
    void addEmp(EmployeeDTO employeeDTO);

    /**
     * 分页查询
     * @param pageQueryDTO
     * @return
     */
    PageResult page(EmployeePageQueryDTO pageQueryDTO);

    /**
     * 启用禁用员工权限
     * @param status
     * @param id
     */
    void startOrStop(Integer status, Long id);

    /**
     * 编辑员工——查询
     * 根据ID查询
     * @param id
     * @return
     */
    Employee getById(Long id);

    /**
     * 编辑员工
     * @param employeeDTO
     */
    void update(EmployeeDTO employeeDTO);
}
