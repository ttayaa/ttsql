package com.ttsql.helpers;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StringHelper {
    private static final Pattern BLANK_PATTERN = Pattern.compile("\\|\t|\r|\n");
    private static final String XML_EXT = ".xml";
    private static final String JAVA_EXT = ".java";
    private static final String CLASS_EXT = ".class";

    private StringHelper() {
    }

    public static String replaceBlank(String str) {
        Matcher m = BLANK_PATTERN.matcher(str);
        return m.replaceAll("").replaceAll("\\s{2,}", " ").trim();
    }

    public static boolean isBlank(String str) {
        int strLen;
        if (str != null && (strLen = str.length()) != 0) {
            for(int i = 0; i < strLen; ++i) {
                if (!Character.isWhitespace(str.charAt(i))) {
                    return false;
                }
            }

            return true;
        } else {
            return true;
        }
    }

    public static boolean isNotBlank(String str) {
        return !isBlank(str);
    }

    public static String concat(String... strs) {
        StringBuilder sb = new StringBuilder("");
        String[] var2 = strs;
        int var3 = strs.length;

        for(int var4 = 0; var4 < var3; ++var4) {
            String str = var2[var4];
            sb.append(str);
        }

        return sb.toString();
    }

    private static boolean isExtFile(String filePath, String ext) {
        return filePath != null && filePath.endsWith(ext);
    }

    public static boolean isXmlFile(String filePath) {
        return isExtFile(filePath, ".xml");
    }

    public static boolean isJavaFile(String filePath) {
        return isExtFile(filePath, ".java");
    }

    public static boolean isClassFile(String filePath) {
        return isExtFile(filePath, ".class");
    }


    private final static Pattern BIG_COMPILE = Pattern.compile("[A-Z]");

    //小驼峰转下划线
    public static String smallHumpToUnderline(String str) {

        if (str==null)
            return null;

        Matcher matcher = BIG_COMPILE.matcher(str);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(sb, "_" + matcher.group(0).toLowerCase());
        }
        matcher.appendTail(sb);

        return sb.toString();
    }

    //大驼峰转下划线
    public static String bigHumpToUnderline(String str) {
        if (str==null)
            return null;
        Matcher matcher = BIG_COMPILE.matcher(str);
        StringBuffer sb = new StringBuffer();
        int i =0;
        while (matcher.find()) {
            if (i==0){
                matcher.appendReplacement(sb,matcher.group(0).toLowerCase());
            }else {
                matcher.appendReplacement(sb, "_" + matcher.group(0).toLowerCase());
            }
            i++;
        }
        matcher.appendTail(sb);
        return sb.toString();
    }
    //下划线 转 大驼峰
    public static String putOffUnderline(String columnName) {
        if (columnName==null)
            return null;
        StringBuffer fieldNameBuffer = null;
        String tempNameArray[] = columnName.split("_");
        for (int i = 0; i < tempNameArray.length; i++) {
            if (i == 0) {
                fieldNameBuffer = new StringBuffer(tempNameArray[i]);
            } else {
                fieldNameBuffer.append(captureName(tempNameArray[i]));
            }
        }
        return fieldNameBuffer.toString();
    }
    //首字母大写
    public static String captureName(String name) {
        if (name==null)
            return null;
        name = name.substring(0, 1).toUpperCase() + name.substring(1);
        return name;
    }
}
