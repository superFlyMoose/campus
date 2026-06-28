package com.campus.management.dto;

import java.util.List;
import lombok.Data;

@Data
public class DashboardChartCacheData {
    private List<String> labels;
    private List<Long> activityCounts;
    private List<Long> registrationCounts;
}
