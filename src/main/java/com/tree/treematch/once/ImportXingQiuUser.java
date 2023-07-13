package com.tree.treematch.once;

import com.alibaba.excel.EasyExcel;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 导入星球用户到数据库
 */
public class ImportXingQiuUser {
    public static void main(String[] args) {
        String fileName = "D:\\Users\\Desktop\\Partner Matching System\\yupao-backend\\src\\main\\resources\\test.xlsx";
            // 这里 需要指定读用哪个class去读，然后读取第一个sheet 同步读取会自动finish
            List<XingQiuTableUserInfo> userInfolist =
                    EasyExcel.read(fileName).head(XingQiuTableUserInfo.class).sheet().doReadSync();
        System.out.println("总数 = "+userInfolist.size());
        //把名称相同的数据放到同一组下
        Map<String, List<XingQiuTableUserInfo>> listMap =
                userInfolist.stream()
                        .filter(userInfo-> StringUtils.isNotEmpty(userInfo.getUserName()))
                        .collect(Collectors.groupingBy(XingQiuTableUserInfo::getUserName));

        //查看重复名
        for (Map.Entry<String, List<XingQiuTableUserInfo>> stringListEntry : listMap.entrySet()) {
            if (stringListEntry.getValue().size() >1){
                System.out.println("userName = "+stringListEntry.getKey());

            }
        }

        System.out.println("不重复昵称数 = "+listMap.keySet().size());
    }
}
