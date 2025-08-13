package com.github.eacryo.mongoflex.util;

import org.springframework.stereotype.Component;

/**
 * 字符串工具类
 */
//TODO:未经过测试
public class StringUtil {

    /**
     * 小驼峰转下划线
     * 例如：userName -> user_name
     * 
     * @param camelCase 小驼峰字符串
     * @return 下划线字符串
     */
    public static String camelToUnderscore(String camelCase) {
        if (camelCase == null || camelCase.isEmpty()) {
            return camelCase;
        }
        
        StringBuilder result = new StringBuilder();
        //标准的小驼峰中不存在连续的大写字母
        for (int i = 0; i < camelCase.length(); i++) {
            char c = camelCase.charAt(i);
            if (Character.isUpperCase(c)) {
                if (i > 0) {
                    result.append('_');
                }
                result.append(Character.toLowerCase(c));
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }

    /**
     * 下划线转小驼峰
     * 例如：user_name -> userName
     * 
     * @param underscore 下划线字符串
     * @return 小驼峰字符串
     */
    public static String underscoreToCamel(String underscore) {
        if (underscore == null || underscore.isEmpty()) {
            return underscore;
        }
        
        StringBuilder result = new StringBuilder();
        boolean nextUpper = false;
        
        for (int i = 0; i < underscore.length(); i++) {
            char c = underscore.charAt(i);
            if (c == '_') {
                nextUpper = true;
            } else {
                if (nextUpper) {
                    result.append(Character.toUpperCase(c));
                    nextUpper = false;
                } else {
                    result.append(c);
                }
            }
        }
        return result.toString();
    }

    /**
     * 判断字符串是否为空或null
     * 
     * @param str 待检查的字符串
     * @return 如果为空或null返回true，否则返回false
     */
    public static boolean isEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }

    /**
     * 判断字符串是否不为空且不为null
     * 
     * @param str 待检查的字符串
     * @return 如果不为空且不为null返回true，否则返回false
     */
    public static boolean isNotEmpty(String str) {
        return !isEmpty(str);
    }
}
