package com.lucky.luckyojbackenduserservice.utils;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Component
public class MailUtils {

    @Autowired
    JavaMailSender javaMailSender;

    // 获取六位随机验证码
    public static String getCode() {
        // 由于数字 1 、 0 和字母 O 、l 有时分不清楚，所以，没有数字 1 、 0
        String[] beforeShuffle = {"2", "3", "4", "5", "6", "7", "8", "9", "A", "B", "C", "D", "E", "F",
                "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z", "a",
                "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m", "n", "o", "p", "q", "r", "s", "t", "u", "v",
                "w", "x", "y", "z"};
        // 将数组转换成集合
        List<String> list = Arrays.asList(beforeShuffle);
        // 打乱集合顺序，以达到随机的效果
        Collections.shuffle(list);
        // 创建StringBuilder，不是线程安全的
        StringBuilder sb = new StringBuilder();
        // 将集合转变成StringBuilder字符串
        for (String s : list) {
            sb.append(s);
        }
        // 返回sb字符串中第10~17位的5位验证码，这个区间其实随便设的
        return sb.substring(10, 16);
    }
}
