package com.tree.treematch.service;

import com.tree.treematch.utils.AlgorithmUtils;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;


/**
 * 算法工具类测试
 */
public class AlgorithmUtilsTest {

   @Test
    void test(){
       String str1 = "达菲鸭是达菲";
       String str2 = "达菲鸭不是鸭";
       String str3 = "达菲鸭是鸭";
       int score1 = AlgorithmUtils.minDistance(str1, str2);//增删改操作次数
       int score2 = AlgorithmUtils.minDistance(str1, str3);//增删改操作次数
       System.out.println("str1, str2: " + score1);
       System.out.println("str1, str3: " + score2);
   }

   @Test
   void testCompareTags(){
      List<String> tagList1 = Arrays.asList("Java", "大一", "男");
      List<String> tagList2 = Arrays.asList("Java", "大一", "女");
      List<String> tagList3 = Arrays.asList("Python", "大二", "女");
      int score1 = AlgorithmUtils.minDistance(tagList1, tagList2);//增删改操作次数
      int score2 = AlgorithmUtils.minDistance(tagList1, tagList3);//增删改操作次数
      System.out.println("tagList1, tagList2: " + score1);//1
      System.out.println("tagList1, tagList3: " + score2);//3
   }

}
