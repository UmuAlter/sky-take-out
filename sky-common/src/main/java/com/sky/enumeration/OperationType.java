package com.sky.enumeration;

/**
 * 数据库操作类型
 */
public enum OperationType {

    /**
     * 更新操作
     */
    UPDATE,

    /**
     * 插入操作
     */
    INSERT

}
/*
默认情况下，枚举常量会被分配一个从 0 开始递增的整数值作为内部值。因此，在这个例子中，UPDATE 的内部值为 0，INSERT 的内部值为 1
 */