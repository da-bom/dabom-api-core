package com.project.domain.recap.service;

import com.project.domain.recap.dto.response.MonthlyRecapResponse;

public interface RecapService {
    MonthlyRecapResponse getMonthlyRecap(Long customerId, int year, int month);
}
