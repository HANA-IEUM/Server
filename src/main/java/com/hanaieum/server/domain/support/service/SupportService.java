package com.hanaieum.server.domain.support.service;

import com.hanaieum.server.domain.member.entity.Member;
import com.hanaieum.server.domain.support.dto.SupportMessageUpdateRequest;
import com.hanaieum.server.domain.support.dto.SupportRequest;
import com.hanaieum.server.domain.support.dto.SupportResponse;

import java.util.List;

public interface SupportService {

    SupportResponse supportBucketList(Long bucketListId, SupportRequest request, Member supporter);

    List<SupportResponse> getBucketListSupports(Long bucketListId, Member member);

    SupportResponse getSupportRecord(Long supportId, Member member);

    SupportResponse updateSupportMessage(Long supportId, SupportMessageUpdateRequest request, Member member);

    void deleteSupportRecord(Long supportId, Member member);
}