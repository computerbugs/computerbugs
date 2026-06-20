package com.example.changtest;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

/**
 * EfggTure 单元测试类 (JUnit 5)
 *
 * 覆盖范围: 正常场景、边界场景、异常场景
 *
 * @author changjiasheng
 */
@DisplayName("EfggTure 单元测试")
public class EfggTureTest {

    // ======================== Time2GMT 测试 ========================

    @Test
    @DisplayName("Time2GMT - 正常日期转GMT格式")
    void testTime2GMT_Normal() throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date date = sdf.parse("2024-03-08 15:30:00");
        String result = EfggTure.Time2GMT(date);

        assertNotNull(result, "结果不应为null");
        assertTrue(result.endsWith("GMT"), "应以GMT结尾");
        assertEquals("Fri, 8 Mar 2024 07:30:00 GMT", result);
    }

    @Test
    @DisplayName("Time2GMT - epoch时间点(1970-01-01)")
    void testTime2GMT_Epoch() {
        Date epoch = new Date(0L);
        String result = EfggTure.Time2GMT(epoch);

        assertNotNull(result);
        // 时区差异可能导致不同输出，验证基本结构即可
        assertTrue(result.contains("1970"));
        assertTrue(result.endsWith("GMT"));
    }

    @Test
    @DisplayName("Time2GMT - 最大/极值日期")
    void testTime2GMT_ExtremeDates() throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date farFuture = sdf.parse("2099-12-31 23:59:59");
        String result = EfggTure.Time2GMT(farFuture);

        assertNotNull(result);
        assertTrue(result.endsWith("GMT"));
        assertTrue(result.contains("2099"));
    }

    // ======================== isDate 测试 ========================

    @Test
    @DisplayName("isDate - 正常场景: 有效日期格式返回 true")
    void testIsDate_NormalScenarios() {
        assertTrue(EfggTure.isDate("2024-03-08"));
        assertTrue(EfggTure.isDate("2024/03/08"));
        assertTrue(EfggTure.isDate("2024年03月08日"));
        assertTrue(EfggTure.isDate("2024-03-08 15:30:00"));
        assertTrue(EfggTure.isDate("2024/03/08 15:30:00"));
        assertTrue(EfggTure.isDate("2024年03月08日 15:30:00"));
        assertTrue(EfggTure.isDate("20240308"));
        assertTrue(EfggTure.isDate("20240308153000"));
    }

    @ParameterizedTest(name = "isDate(\"{0}\") => true (合法日期)")
    @ValueSource(strings = {
            "2024-02-29",  // 闰年2月29日 - 有效
            "2023-02-28",  // 非闰年2月28日 - 有效
            "2099-12-31"   // 极值日期
    })
    @DisplayName("isDate - 边界场景: 合法日期字符串")
    void testIsDate_ValidDates(String input) {
        assertTrue(EfggTure.isDate(input), "合法日期应返回 true: " + input);
    }

    @ParameterizedTest(name = "isDate(\"{0}\") => false (非法日期)")
    @ValueSource(strings = {
            "2023-13-45", "not-a-date", "abc123",
            "   ", "特殊字符!@#$%", "2023-02-29",  // 非闰年2月29日
            "2024-00-01", "", "2024-13-01"
    })
    @DisplayName("isDate - 边界场景: 非法日期字符串返回 false")
    void testIsDate_InvalidDates(String input) {
        assertFalse(EfggTure.isDate(input), "非法日期应返回 false: " + input);
    }

    @ParameterizedTest(name = "isDate(null) => NullPointerException")
    @NullSource
    @DisplayName("isDate - 异常场景: null")
    void testIsDate_Exception_Null(String input) {
        assertThrows(NullPointerException.class, () -> EfggTure.isDate(input),
                "null 输入应抛 NullPointerException");
    }

    // ======================== covertDateStrFormat 测试 ========================

    @Test
    @DisplayName("covertDateStrFormat - 正常转换 yyyy-MM-dd -> yyyy/MM/dd")
    void testCovertFormat_Normal1() throws Exception {
        String result = EfggTure.covertDateStrFormat("2024-03-08", "yyyy-MM-dd", "yyyy/MM/dd");

        assertEquals("2024/03/08", result);
    }

    @Test
    @DisplayName("covertDateStrFormat - 正常转换 含时间的完整格式")
    void testCovertFormat_Normal2() throws Exception {
        String result = EfggTure.covertDateStrFormat(
                "2024-03-08 14:30:00", "yyyy-MM-dd HH:mm:ss", "yyyyMMddHHmmss"
        );

        assertEquals("20240308143000", result);
    }

    @Test
    @DisplayName("covertDateStrFormat - 正常转换 中文日期格式")
    void testCovertFormat_Normal3() throws Exception {
        String result = EfggTure.covertDateStrFormat(
                "2024年03月08日", "yyyy年MM月dd日", "yyyyMMdd"
        );

        assertEquals("20240308", result);
    }

    @ParameterizedTest(name = "\"{0}\" 格式转换异常")
    @ValueSource(strings = {"invalid-date", "", "abc", "9999-99-99"})
    @DisplayName("covertDateStrFormat - 异常场景: 非法日期字符串抛异常")
    void testCovertFormat_Exception_InvalidDate(String invalidInput) {
        Exception exception = assertThrows(Exception.class,
                () -> EfggTure.covertDateStrFormat(invalidInput, "yyyy-MM-dd", "yyyy/MM/dd"),
                "非法日期字符串应抛出Exception");

        assertInstanceOf(ParseException.class, exception.getCause(),
                "根原因应为ParseException, 实际: " + exception.getCause().getClass().getSimpleName());
    }

    @Test
    @DisplayName("covertDateStrFormat - 边界场景: 极值日期(2099-12-31)")
    void testCovertFormat_Boundary_MaxDate() throws Exception {
        String result = EfggTure.covertDateStrFormat(
                "2099-12-31 23:59:59", "yyyy-MM-dd HH:mm:ss", "yyyyMMddHHmmss"
        );

        assertEquals("20991231235959", result);
    }

    @Test
    @DisplayName("covertDateStrFormat - 边界场景: 最小日期(0001-01-01)")
    void testCovertFormat_Boundary_MinDate() throws Exception {
        String result = EfggTure.covertDateStrFormat(
                "0001-01-01", "yyyy-MM-dd", "yyyyMMdd"
        );

        assertEquals("00010101", result);
    }

    // ======================== getWeekByDate 测试 ========================

    @Test
    @DisplayName("getWeekByDate - 正常场景: 周一")
    void testGetWeekByDate_Monday() throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Date monday = sdf.parse("2024-03-04"); // 周一

        assertEquals("周一", EfggTure.getWeekByDate(monday));
    }

    @Test
    @DisplayName("getWeekByDate - 正常场景: 周日")
    void testGetWeekByDate_Sunday() throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Date sunday = sdf.parse("2024-03-03"); // 周日

        assertEquals("周日", EfggTure.getWeekByDate(sunday));
    }

    @Test
    @DisplayName("getWeekByDate - 正常场景: 周五")
    void testGetWeekByDate_Friday() throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Date friday = sdf.parse("2024-03-08"); // 周五

        assertEquals("周五", EfggTure.getWeekByDate(friday));
    }

    @ParameterizedTest(name = "{0} => {1}")
    @CsvSource({
            "2024-01-01, 周一", "2024-01-02, 周二", "2024-01-03, 周三",
            "2024-01-04, 周四", "2024-01-05, 周五", "2024-01-06, 周六", "2024-01-07, 周日"
    })
    @DisplayName("getWeekByDate - 边景: 一周七天全覆盖")
    void testGetWeekByDate_AllDaysOfWeek(String dateStr, String expectedWeek) throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Date date = sdf.parse(dateStr);

        assertEquals(expectedWeek, EfggTure.getWeekByDate(date));
    }

    @Test
    @DisplayName("getWeekByDate - 边界场景: epoch时间点(周四)")
    void testGetWeekByDate_Epoch() {
        Date epoch = new Date(0L); // 1970-01-01 周四

        assertEquals("周四", EfggTure.getWeekByDate(epoch));
    }
}
