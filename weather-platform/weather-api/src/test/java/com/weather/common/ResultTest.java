package com.weather.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ResultTest {

    @Test
    void successShouldCarryCode200AndData() {
        Result<String> result = ResultUtils.success("hello");

        assertEquals(200, result.getCode());
        assertEquals("成功", result.getMsg());
        assertEquals("hello", result.getData());
    }

    @Test
    void failShouldCarryCustomCodeAndMsg() {
        Result<Void> result = ResultUtils.fail(ResultCode.BAD_REQUEST.getCode(), "用户名已存在");

        assertEquals(400, result.getCode());
        assertEquals("用户名已存在", result.getMsg());
        assertEquals(null, result.getData());
    }
}
