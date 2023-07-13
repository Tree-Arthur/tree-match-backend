package com.tree.treematch.once;

import com.alibaba.excel.EasyExcel;

import java.util.List;

/**
 * 导入Excel
 * @author tree
 */
public class ImportExcel {

    /**
     * 最简单的读
     * 1. 创建excel对应的实体对象 参照{@link DemoData}
     * 2. 由于默认一行行的读取excel，所以需要创建excel一行一行的回调监听器，参照{@link DemoDataListener}
     * 3. 直接读即可
     */
    public static void main(String[] args) {
        // 写法1：JDK8+ ,不用额外写一个DemoDataListener
        // since: 3.0.0-beta1
        String fileName = "D:\\Users\\Desktop\\Partner Matching System\\yupao-backend\\src\\main\\resources\\test.xlsx";
//        listenerRead(fileName);
        synchronousRead(fileName);
    }
    /**
     * 监听器读取
     * @param fileName
     */
    private static void listenerRead(String fileName) {
        // 这里默认每次会读取100条数据 然后返回过来 直接调用使用数据就行
        // 具体需要返回多少行可以在`PageReadListener`的构造函数设置
        EasyExcel.read(fileName, XingQiuTableUserInfo.class, new TableListener()).sheet().doRead();
    }
    /**
     * 同步读取
     * @param fileName
     */
    public static void synchronousRead(String fileName) {
        // 这里 需要指定读用哪个class去读，然后读取第一个sheet 同步读取会自动finish
        List<XingQiuTableUserInfo> list = EasyExcel.read(fileName).head(XingQiuTableUserInfo.class).sheet().doReadSync();
        for (XingQiuTableUserInfo data : list) {
            System.out.println(data);
        }
    }
}
