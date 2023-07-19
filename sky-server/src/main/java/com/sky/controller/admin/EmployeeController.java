package com.sky.controller.admin;

import com.sky.constant.JwtClaimsConstant;
import com.sky.dto.EmployeeDTO;
import com.sky.dto.EmployeeLoginDTO;
import com.sky.dto.EmployeePageQueryDTO;
import com.sky.entity.Employee;
import com.sky.properties.JwtProperties;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.EmployeeService;
import com.sky.utils.JwtUtil;
import com.sky.vo.EmployeeLoginVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 员工管理
 */
@RestController
@RequestMapping("/admin/employee")
@Slf4j
@Api(tags = "员工相关的接口")  //会体现在由knife4j生成的接口文档上
public class EmployeeController {

    @Autowired
    private EmployeeService employeeService;
    @Autowired
    private JwtProperties jwtProperties;

    /**
     * 登录
     *
     * @param employeeLoginDTO
     * @return
     */
    //DTO_做数据传输的实体
    @ApiOperation(value = "登录")
    @PostMapping("/login")
    public Result<EmployeeLoginVO> login(@RequestBody EmployeeLoginDTO employeeLoginDTO) {
        log.info("员工登录：{}", employeeLoginDTO);

        Employee employee = employeeService.login(employeeLoginDTO);

        //登录成功后，生成jwt令牌
        Map<String, Object> claims = new HashMap<>();
        claims.put(JwtClaimsConstant.EMP_ID, employee.getId());
        String token = JwtUtil.createJWT(
                jwtProperties.getAdminSecretKey(),
                jwtProperties.getAdminTtl(),
                claims);

        EmployeeLoginVO employeeLoginVO = EmployeeLoginVO.builder()
                .id(employee.getId())
                .userName(employee.getUsername())
                .name(employee.getName())
                .token(token)
                .build();

        return Result.success(employeeLoginVO);
    }

    /**
     * 退出
     *
     * @return
     */
    @ApiOperation("退出")
    @PostMapping("/logout")
    public Result<String> logout() {
        return Result.success();
    }

    /**
     * 新增员工
     * @param employeeDTO
     * @return
     */
    @ApiOperation("新增员工")
    @PostMapping
    public Result addEmp(@RequestBody EmployeeDTO employeeDTO){
        log.info("新增员工{}",employeeDTO);

        employeeService.addEmp(employeeDTO);

        return Result.success();
    }

    /**
     * 分页查询
     * @param pageQueryDTO
     * @return
     */
    @ApiOperation("员工分页查询")
    @GetMapping("/page")
    public Result page(EmployeePageQueryDTO pageQueryDTO){
        log.info("分页查询，参数为：#{}",pageQueryDTO);

        PageResult result = employeeService.page(pageQueryDTO);

        return Result.success(result);
    }

    //查询类建议加Result泛型，因为有类型为T的data，非查询类一般不需要加
    /**
     * 启用禁用员工权限
     * @param status
     * @param id
     * @return
     */
    @PostMapping("/status/{status}")
    @ApiOperation("启用禁用员工权限")
    public Result startOrStop(@PathVariable Integer status,Long id){
        log.info("启用禁用员工账户");
        employeeService.startOrStop(status,id);
        return Result.success();
    }
}
