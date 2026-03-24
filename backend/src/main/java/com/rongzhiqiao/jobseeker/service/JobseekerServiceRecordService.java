package com.rongzhiqiao.jobseeker.service;

import com.rongzhiqiao.common.exception.BusinessException;
import com.rongzhiqiao.common.security.SecurityUtils;
import com.rongzhiqiao.serviceorg.repository.ServiceOrgRepository;
import com.rongzhiqiao.serviceorg.repository.ServiceOrgRepository.ServiceCaseRecord;
import com.rongzhiqiao.serviceorg.service.ServiceOrgService;
import com.rongzhiqiao.serviceorg.vo.ServiceCaseDetailResponse;
import com.rongzhiqiao.serviceorg.vo.ServiceCaseSummaryResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class JobseekerServiceRecordService {

    private final ServiceOrgRepository serviceOrgRepository;
    private final ServiceOrgService serviceOrgService;

    public List<ServiceCaseSummaryResponse> listCurrentUserCases() {
        Long userId = SecurityUtils.getCurrentUserId();
        return serviceOrgRepository.listCasesByUserId(userId);
    }

    public ServiceCaseDetailResponse getCurrentUserCaseDetail(String caseId) {
        Long userId = SecurityUtils.getCurrentUserId();
        ServiceCaseRecord record = serviceOrgRepository.findCaseByIdAndUserId(caseId, userId);
        if (record == null) {
            throw new BusinessException(4004, "service case not found");
        }
        return serviceOrgService.getCaseDetail(record.id());
    }
}
