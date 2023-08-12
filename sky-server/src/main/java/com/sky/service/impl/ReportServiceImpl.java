package com.sky.service.impl;

import com.sky.dto.GoodsSalesDTO;
import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.UserMapper;
import com.sky.result.Result;
import com.sky.service.ReportService;
import com.sky.service.WorkspaceService;
import com.sky.vo.*;
import io.swagger.models.auth.In;
import org.apache.commons.lang.StringUtils;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ReportServiceImpl implements ReportService {
    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private WorkspaceService workspaceService;
    /**
     * 获取营业额数据统计
     * //营业额为订单状态为已完成的订单
     * @param begin
     * @param end
     * @return
     */
    public TurnoverReportVO getTurnoverStatistics(LocalDate begin, LocalDate end) {
        //存放从begin到end每天的日期
        List<LocalDate> dateList = new ArrayList<>();
        dateList.add(begin);

        while (!begin.equals(end)){
            begin = begin.plusDays(1);  //向后推移一天
            dateList.add(begin);
        }

        List<Double> turnoverList = new ArrayList<>();

        //查看当天营业额
        for (LocalDate date : dateList) {
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);//00:00
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);  //23:59:999
            Map map = new HashMap();
            map.put("begin",beginTime);
            map.put("end",endTime);
            map.put("status", Orders.COMPLETED);
            //计算当天营业额
            Double turnOver = orderMapper.sumByMap(map);
            if(turnOver == null){
                turnOver = 0.0;
            }
            turnoverList.add(turnOver);
        }
        //List 转 String 阿帕奇下的包
        return TurnoverReportVO.builder()
                .dateList(StringUtils.join(dateList,","))
                .turnoverList(StringUtils.join(turnoverList,","))
                .build();
    }

    /**
     * 用户统计
     * @param begin
     * @param end
     * @return
     */
    public UserReportVO getUserStatistics(LocalDate begin, LocalDate end) {
        List<LocalDate> dateList = new ArrayList<>();
        dateList.add(begin);
        while (begin != end){
            begin = begin.plusDays(1);
            dateList.add(begin);
        }
        //每天新增用户数
        List<Integer> addUsers  = new ArrayList<>();
        //每天总用户数
        List<Integer> totalUsers = new ArrayList<>();
        for (LocalDate date : dateList) {
            LocalDateTime beginTime = LocalDateTime.of(date,LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(end,LocalTime.MAX);

            Map map = new HashMap();
            map.put("end",endTime);
            //查截至当天总的用户数
            Integer addUser =  userMapper.countByMap(map);
            map.put("begin",beginTime);
            //查当天新增用户数
            Integer totalUser = userMapper.countByMap(map);

            addUsers.add(addUser);
            totalUsers.add(totalUser);
        }
        return UserReportVO.builder()
                .dateList(StringUtils.join(dateList,","))
                .newUserList(StringUtils.join(addUsers,","))
                .totalUserList(StringUtils.join(totalUsers,","))
                .build();
    }

    /**
     * 统计指定时间内的订单数据
     * @param begin
     * @param end
     * @return
     */
    public OrderReportVO getOrderStatistics(LocalDate begin, LocalDate end) {
        List<LocalDate> dateList = new ArrayList<>();
        dateList.add(begin);
        while (begin != end){
            begin = begin.plusDays(1);
            dateList.add(begin);
        }

        List<Integer> totalOrderList = new ArrayList<>();   //总订单
        List<Integer> usefullList = new ArrayList<>();      //有效订单

        for (LocalDate date : dateList) {
            LocalDateTime beginTime = LocalDateTime.of(date,LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date,LocalTime.MAX);

            //查询每天订单总数
            totalOrderList.add(countOrders(beginTime,endTime,null));
            //查询每天有效订单数
            usefullList.add(countOrders(beginTime,endTime,Orders.COMPLETED));
        }
        //对List中的数据求和
        Integer totalOrderCount = totalOrderList.stream().reduce(Integer::sum).get();
        Integer validOrderCount = usefullList.stream().reduce(Integer::sum).get();
        //计算订单完成率
        Double orderCompleteRate =0.0;
        if(totalOrderCount != 0){
            orderCompleteRate = validOrderCount.doubleValue() / totalOrderCount;
        }
        return OrderReportVO.builder()
                .dateList(StringUtils.join(dateList,","))
                .orderCountList(StringUtils.join(totalOrderList,","))
                .validOrderCountList(StringUtils.join(usefullList,","))
                .totalOrderCount(totalOrderCount)
                .validOrderCount(validOrderCount)
                .orderCompletionRate(orderCompleteRate)
                .build();
    }

    /**
     * 统计销量排名前十
     * statys == 5 才是销售出的商品
     * @param begin
     * @param end
     * @return
     */
    public SalesTop10ReportVO getSalesTop10(LocalDate begin, LocalDate end) {
        LocalDateTime beginTime = LocalDateTime.of(begin,LocalTime.MIN);
        LocalDateTime endTime = LocalDateTime.of(end,LocalTime.MAX);

        List<GoodsSalesDTO> top10 = orderMapper.getTop10(beginTime, endTime);
        List<String> names = top10.stream().map(GoodsSalesDTO::getName).collect(Collectors.toList());
        List<Integer> numbers = top10.stream().map(GoodsSalesDTO::getNumber).collect(Collectors.toList());
        String nameList = StringUtils.join(names,",");
        String numberList = StringUtils.join(numbers,",");
        return SalesTop10ReportVO.builder()
                .nameList(nameList)
                .numberList(numberList)
                .build();
    }

    /**
     * 导出运营数据表
     * @param response
     */
    public void exportBusinessData(HttpServletResponse response) {
        //查询数据库，获得运营数据
        //距离今天最近的30天
        LocalDate dateBegin = LocalDate.now().minusDays(30);
        //昨天
        LocalDate dateEnd = LocalDate.now().minusDays(1);
        //获得近30天运营数据
        BusinessDataVO businessDataVO = workspaceService.getBusinessData(
                LocalDateTime.of(dateBegin, LocalTime.MIN),
                LocalDateTime.of(dateEnd, LocalTime.MAX)
        );
        //将数据写入excel文件
        InputStream in = this.getClass().getClassLoader().getResourceAsStream("template/modu.xlsx");
        try {
            //基于模板创建一个新的excel文件
            XSSFWorkbook excel = new XSSFWorkbook(in);
            //获取标签页
            XSSFSheet sheet = excel.getSheetAt(0);
            //获取行-获取单元格-填充数据( time )
            sheet.getRow(1)
                    .getCell(1)
                    .setCellValue("时间 从" + dateBegin + "到" + dateEnd);
            //第四行
            XSSFRow row = sheet.getRow(3);
            row.getCell(2).setCellValue(businessDataVO.getTurnover());
            row.getCell(4).setCellValue(businessDataVO.getOrderCompletionRate());
            row.getCell(6).setCellValue(businessDataVO.getNewUsers());
            //第五行
            row = sheet.getRow(4);
            row.getCell(2).setCellValue(businessDataVO.getValidOrderCount());//有效订单
            row.getCell(4).setCellValue(businessDataVO.getUnitPrice());     //平均客单价

            //填充明细数据
            for(int i=0;i<30;i++){
                LocalDate date = dateBegin.plusDays(i);
                //查询某一天营业额
                BusinessDataVO businessData = workspaceService.getBusinessData(
                        LocalDateTime.of(dateBegin, LocalTime.MIN),
                        LocalDateTime.of(dateEnd, LocalTime.MAX)
                );
                row = sheet.getRow(7+i);
                row.getCell(1).setCellValue(date.toString());
                row.getCell(2).setCellValue(businessDataVO.getTurnover());
                row.getCell(3).setCellValue(businessDataVO.getValidOrderCount());
                row.getCell(4).setCellValue(businessDataVO.getOrderCompletionRate());
                row.getCell(5).setCellValue(businessDataVO.getUnitPrice());
                row.getCell(6).setCellValue(businessDataVO.getNewUsers());
            }
        //通过输出流将excel文件下载到客户端浏览器
            ServletOutputStream outputStream = response.getOutputStream();
            excel.write(outputStream);//效果为弹出保存对话框，可以下载文件

            outputStream.close();
            excel.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 根据条件查询订单数
     * （用不到某一个参数可以直接传一个null）
     * @param begin
     * @param end
     * @param status
     * @return
     */
    private Integer countOrders(LocalDateTime begin,LocalDateTime end,Integer status){
        Map map = new HashMap();
        map.put("begin",begin);
        map.put("end",end);
        map.put("status",status);
        return orderMapper.countByMap(map);
    }

}
