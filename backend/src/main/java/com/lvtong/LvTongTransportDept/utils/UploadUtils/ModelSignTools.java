package com.lvtong.LvTongTransportDept.utils.UploadUtils;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 签名工具类
 * 将 JSON 对象转换为待签名字符串：key=value&key=value（按字典序排列）
 */
public class ModelSignTools {

    /**
     * 生成待签名字符串
     *
     * @param modelJsonStr JSON 格式字符串
     * @param filterFields 排除的字段名（可选）
     * @return key=value&key=value 格式字符串
     */
    public static String generateSignContent(String modelJsonStr, String... filterFields) {
        Map<String, Object> map = JSON.parseObject(modelJsonStr, new TypeReference<Map<String, Object>>() {});

        Set<Map.Entry<String, Object>> entrySet = new TreeMap<>(map).entrySet();
        List<String> filterFieldList = Arrays.asList(filterFields);

        return entrySet.stream()
                .filter(e -> {
                    if (e.getValue() instanceof Collection) {
                        return !((Collection<?>) e.getValue()).isEmpty();
                    }
                    return e.getValue() != null;
                })
                .filter(e -> !filterFieldList.contains(e.getKey()))
                .map(ModelSignTools::keyValue)
                .collect(Collectors.joining("&"));
    }

    private static String keyValue(Map.Entry<String, Object> e) {
        Object value = e.getValue();

        if (value instanceof LocalDateTime) {
            LocalDateTime ldt = (LocalDateTime) value;
            value = ldt.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));

        } else if (value instanceof List) {
            List<?> list = (List<?>) value;
            String inner = list.stream().map(item -> {
                if (item instanceof Map) {
                    return "{" + generateSignContent(toJsonStr((Map<?, ?>) item)) + "}";
                }
                return "{" + Objects.toString(item, "") + "}";
            }).collect(Collectors.joining(","));
            value = "[" + inner + "]";

        } else if (value instanceof Map) {
            try {
                return e.getKey() + "={" + generateSignContent(toJsonStr((Map<?, ?>) value)) + "}";
            } catch (Exception ex) {
                return null;
            }
        }

        return e.getKey() + "=" + value;
    }

    private static String toJsonStr(Map<?, ?> map) {
        return JSON.toJSONString(map);
    }
}
