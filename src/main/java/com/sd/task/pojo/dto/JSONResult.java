package com.sd.task.pojo.dto;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;

public class JSONResult {
    public static String fillResultString(Integer status, String message, Object result) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("code", status);
        jsonObject.put("msg", message);
        jsonObject.put("data", result);
        return JSONObject.toJSONString(jsonObject, SerializerFeature.WriteMapNullValue);
    }
}