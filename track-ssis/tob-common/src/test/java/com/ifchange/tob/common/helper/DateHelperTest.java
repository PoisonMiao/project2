package com.ifchange.tob.common.helper;

import com.ifchange.tob.common.support.CalendarInfo;
import com.ifchange.tob.common.support.DateFormat;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Ignore
public class DateHelperTest {

    @Test
    public void formatDate() {
        Assert.assertEquals(DateHelper.now(DateFormat.StrikeDateTime), DateHelper.format(new Date(), DateFormat.StrikeDateTime));
    }

    @Test
    public void formatLocalDate() {
        Assert.assertEquals(DateHelper.now(DateFormat.StrikeDate), DateHelper.format(LocalDate.now(), DateFormat.StrikeDate));
    }

    @Test
    public void formatLocalDateTime() {
        Assert.assertEquals(DateHelper.now(DateFormat.StrikeDateTime), DateHelper.format(LocalDateTime.now(), DateFormat.StrikeDateTime));
    }

    @Test
    public void formatLong() {
        Assert.assertEquals(DateHelper.now(DateFormat.StrikeDateTime), DateHelper.format(new Date().getTime(), DateFormat.StrikeDateTime));
    }

    @Test
    public void ofDateString() {
        Date date = DateHelper.ofDate("2018-03-27", DateFormat.StrikeDate);
        Assert.assertEquals("180327", DateHelper.format(date, DateFormat.ShortNumDate));
    }

    @Test
    public void ofDateLong() {
        Date date = DateHelper.ofDate(1522142502000L);
        Assert.assertEquals("2018-03-27", DateHelper.format(date, DateFormat.StrikeDate));
    }

    @Test
    public void ofLocalDateString() {
        LocalDate date = DateHelper.ofLocalDate("2018-03-27", DateFormat.StrikeDate);
        Assert.assertEquals("20180327", DateHelper.format(date, DateFormat.NumDate));
    }

    @Test
    public void ofLocalDateLong() {
        LocalDate date = DateHelper.ofLocalDate(1522142502000L);
        Assert.assertEquals("2018-03-27", DateHelper.format(date, DateFormat.StrikeDate));
    }

    @Test
    public void ofLocalDateTimeString() {
        LocalDateTime time = DateHelper.ofLocalDateTime("2018-03-27 17:30:20", DateFormat.StrikeDateTime);
        Assert.assertEquals("20180327173020", DateHelper.format(time, DateFormat.NumDateTime));
    }

    @Test
    public void ofLocalDateTimeLong() {
        LocalDateTime time = DateHelper.ofLocalDateTime(1522143020000L);
        Assert.assertEquals("2018-03-27 17:30:20", DateHelper.format(time, DateFormat.StrikeDateTime));
    }

    @Test
    public void date2Local() {
        LocalDate date = DateHelper.date2Local(1522143020000L);
        Assert.assertEquals("2018-03-27", DateHelper.format(date, DateFormat.StrikeDate));
    }

    @Test
    public void time2Local() {
        LocalDateTime time = DateHelper.time2Local(1522143020000L);
        Assert.assertEquals("2018-03-27 17:30:20", DateHelper.format(time, DateFormat.StrikeDateTime));
    }

    @Test
    public void nowDate() {
        Assert.assertEquals(new Date().getTime() / DateHelper.SECOND_TIME, DateHelper.now().getTime() / DateHelper.SECOND_TIME);
    }

    @Test
    public void nowString() {
        Assert.assertEquals(DateHelper.format(new Date(), DateFormat.StrikeDateTime), DateHelper.now(DateFormat.StrikeDateTime));
    }

    @Test
    public void secondNow() {
        long nowS = new Date().getTime() / DateHelper.SECOND_TIME;
        Assert.assertEquals(nowS, DateHelper.second());
    }

    @Test
    public void secondDate() {
        Date date = DateHelper.ofDate("2018-03-27 17:30:20", DateFormat.StrikeDateTime);
        Assert.assertEquals(1522143020L, DateHelper.second(date));
    }

    @Test
    public void secondLocalDate() {
        LocalDate date = DateHelper.ofLocalDate("2018-03-27", DateFormat.StrikeDate);
        Assert.assertEquals(1522080000L, DateHelper.second(date));
    }

    @Test
    public void secondLocalDateTime() {
        LocalDateTime time = DateHelper.ofLocalDateTime("2018-03-27 17:30:20", DateFormat.StrikeDateTime);
        Assert.assertEquals(1522143020L, DateHelper.second(time));
    }

    @Test
    public void timeNow() {
        Assert.assertEquals(new Date().getTime() / DateHelper.SECOND_TIME, DateHelper.time() / DateHelper.SECOND_TIME);
    }

    @Test
    public void timeDate() {
        Assert.assertEquals(new Date().getTime() / DateHelper.SECOND_TIME, DateHelper.time(DateHelper.now()) / DateHelper.SECOND_TIME);
    }

    @Test
    public void timeLocalDate() {
        long nowS = DateHelper.ofDate(DateHelper.now(DateFormat.StrikeDate), DateFormat.StrikeDate).getTime() / DateHelper.SECOND_TIME;
        Assert.assertEquals(nowS, DateHelper.time(LocalDate.now()) / DateHelper.SECOND_TIME);
    }

    @Test
    public void timeLocalDateTime() {
        Assert.assertEquals(new Date().getTime() / DateHelper.SECOND_TIME, DateHelper.time(LocalDateTime.now()) / DateHelper.SECOND_TIME);
    }

    @Test
    public void hourAt() {
        long dsTime = DateHelper.ofDayStart(new Date().getTime());
        Assert.assertEquals(DateHelper.ofDate(dsTime), DateHelper.hourAt(0));
    }

    @Test
    public void minuteAt() {
        long dsTime = DateHelper.ofDayStart(new Date().getTime());
        Assert.assertEquals(DateHelper.ofDate(dsTime), DateHelper.minuteAt(0, 0));
    }

    @Test
    public void secondAt() {
        long dsTime = DateHelper.ofDayStart(new Date().getTime());
        Assert.assertEquals(DateHelper.ofDate(dsTime), DateHelper.secondAt(0, 0, 0));
    }

    @Test
    public void timeSlot() {
        Date time = DateHelper.hourAt(0);
        long slot = DateHelper.DAY_TIME - (DateHelper.time() - time.getTime());
        Assert.assertTrue(slot >= DateHelper.timeSlot(time, DateHelper.DAY_TIME));
    }

    @Test
    public void ofDayStart() {
        Assert.assertEquals(DateHelper.secondAt(0, 0, 0).getTime(), DateHelper.ofDayStart(new Date().getTime()));
    }

    @Test
    public void addDays() {
        Date now = DateHelper.now();
        Date date = DateHelper.addDays(now, 3);
        Assert.assertEquals(3, (date.getTime() - now.getTime()) / DateHelper.DAY_TIME);
    }

    @Test
    public void dayOfWeekDate() {
        Assert.assertEquals(1, DateHelper.dayOfWeek(DateHelper.ofDate("2018-01-01", DateFormat.StrikeDate)));
    }

    @Test
    public void dayOfWeekLocalDate() {
        Assert.assertEquals(1, DateHelper.dayOfWeek(DateHelper.ofLocalDate("2018-01-01", DateFormat.StrikeDate)));
    }

    @Test
    public void dayOfWeekNow() {
        Assert.assertEquals(DateHelper.dayOfWeek(new Date()), DateHelper.dayOfWeek());
    }

    @Test
    public void daySize() {
        Date sDate = DateHelper.ofDate("2018-02-27 01:02:00", DateFormat.StrikeDateTime);
        Date eDate = DateHelper.ofDate("2018-03-01 10:02:00", DateFormat.StrikeDateTime);
        Assert.assertEquals(2, DateHelper.daySize(sDate, eDate).intValue());
        Assert.assertTrue(DateHelper.daySize(sDate, eDate).doubleValue() > 2.0);
    }

    @Test
    public void monthDays() {
        Assert.assertEquals(28, DateHelper.monthDays(2017, 2));
        Assert.assertEquals(29, DateHelper.monthDays(2000, 2));
        Assert.assertEquals(31, DateHelper.monthDays(2018, 3));
        Assert.assertEquals(30, DateHelper.monthDays(2017, 9));
    }

    @Test
    public void firstDayOfMonth() {
        Date date = DateHelper.ofDate("2018-02-01", DateFormat.StrikeDate);
        Assert.assertEquals(date, DateHelper.firstDayOfMonth(DateHelper.ofDate("2018-02-27", DateFormat.StrikeDate)));
    }

    @Test
    public void lastDayOfMonth() {
        Date date = DateHelper.ofDate("2018-02-28", DateFormat.StrikeDate);
        Assert.assertEquals(date, DateHelper.lastDayOfMonth(DateHelper.ofDate("2018-02-13", DateFormat.StrikeDate)));
    }

    @Test
    public void isToday() {
        Assert.assertTrue(DateHelper.isToday(LocalDate.now()));
        Assert.assertFalse(DateHelper.isToday(DateHelper.ofLocalDate("2018-02-13", DateFormat.StrikeDate)));
    }

    @Test
    public void firstDayOfWeek() {
        Date date = DateHelper.ofDate("2018-01-01", DateFormat.StrikeDate);
        Assert.assertEquals(date, DateHelper.firstDayOfMonth(DateHelper.ofDate("2018-01-05", DateFormat.StrikeDate)));
    }

    @Test
    public void lastDayOfWeek() {
        Date date = DateHelper.ofDate("2018-01-07", DateFormat.StrikeDate);
        Assert.assertEquals(date, DateHelper.lastDayOfWeek(DateHelper.ofDate("2018-01-03", DateFormat.StrikeDate)));
    }

    @Test
    public void calender() {
        List<CalendarInfo<String>> infoList = DateHelper.calender(2018, 1, date -> Optional.of(DateHelper.format(date, DateFormat.StrikeDate)));
        Assert.assertEquals(31, infoList.size());
        for(int i=0; i < 31; i++) {
            String day = StringHelper.leftPad(String.valueOf(i + 1), 2, '0');
            Assert.assertEquals("2018-01-" + day, infoList.get(i).info);
        }
    }
}