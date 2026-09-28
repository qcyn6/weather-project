package com.weather.service.dashboard;

import com.weather.dto.dashboard.DashboardSummaryVO;
import com.weather.entity.rain.RainRanking;
import com.weather.entity.rain.RainTrend;
import com.weather.entity.temp.TempTrend;
import com.weather.service.hum.HumRankingService;
import com.weather.service.rain.RainRankingService;
import com.weather.service.rain.RainTrendService;
import com.weather.service.sun.SunRankingService;
import com.weather.service.temp.TempRankingService;
import com.weather.service.temp.TempTrendService;
import com.weather.service.wind.WindRankingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private RainRankingService rainRankingService;
    @Mock
    private TempRankingService tempRankingService;
    @Mock
    private SunRankingService sunRankingService;
    @Mock
    private HumRankingService humRankingService;
    @Mock
    private WindRankingService windRankingService;
    @Mock
    private RainTrendService rainTrendService;
    @Mock
    private TempTrendService tempTrendService;

    @InjectMocks
    private DashboardService dashboardService;

    @Test
    void summaryShouldAssembleTrendByYear() {
        when(rainRankingService.latestYear()).thenReturn(2020);
        when(rainRankingService.minYear()).thenReturn(2000);
        when(rainRankingService.countCities()).thenReturn(2L);
        RainRanking row = new RainRanking();
        row.setYear(2020);
        row.setRankNum(1);
        row.setCity("福州市");
        row.setTotalRain(1500.0);
        row.setRainyDays(120);
        when(rainRankingService.listByYear(2020)).thenReturn(List.of(row));
        when(tempRankingService.listByYear(2020)).thenReturn(List.of());
        when(sunRankingService.listByYear(2020)).thenReturn(List.of());
        when(humRankingService.listByYear(2020)).thenReturn(List.of());
        when(windRankingService.listByYear(2020)).thenReturn(List.of());
        when(rainTrendService.listAll()).thenReturn(List.of(
                trend("福州市", 2000, 1200.0),
                trend("泉州市", 2000, 1000.0),
                trend("福州市", 2001, 1300.0),
                trend("泉州市", 2001, 1100.0)));
        when(tempTrendService.listAll()).thenReturn(List.of());

        DashboardSummaryVO vo = dashboardService.getSummary();

        assertEquals(2020, vo.getLatestYear());
        assertEquals(2, vo.getCityCount());
        assertEquals("2000-2020", vo.getYearRange());
        assertEquals(1, vo.getRain().size());
        assertEquals(2, vo.getRainTrend().size());
        assertEquals(2000, vo.getRainTrend().get(0).get("year"));
        assertEquals(1000.0, vo.getRainTrend().get(0).get("泉州市"));
        assertEquals(1300.0, vo.getRainTrend().get(1).get("福州市"));
    }

    @Test
    void summaryShouldBeEmptyWhenNoData() {
        when(rainRankingService.latestYear()).thenReturn(null);

        DashboardSummaryVO vo = dashboardService.getSummary();

        assertNull(vo.getLatestYear());
        assertEquals(0, vo.getCityCount());
        assertTrue(vo.getRain().isEmpty());
        assertTrue(vo.getRainTrend().isEmpty());
        assertTrue(vo.getTempTrend().isEmpty());
    }

    private RainTrend trend(String city, int year, double totalRain) {
        RainTrend t = new RainTrend();
        t.setCity(city);
        t.setYear(year);
        t.setTotalRain(totalRain);
        return t;
    }
}
