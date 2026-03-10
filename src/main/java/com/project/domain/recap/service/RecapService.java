package com.project.domain.recap.service;

import com.project.domain.recap.model.MonthlyRecap;

public interface RecapService {
    MonthlyRecap getMonthlyRecap(Long customerId, int year, int month);
}
