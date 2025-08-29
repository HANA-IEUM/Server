package com.hanaieum.server.domain.support.service;

import com.hanaieum.server.domain.member.entity.Member;
import com.hanaieum.server.domain.support.dto.SupportRequest;
import com.hanaieum.server.domain.support.dto.SupportResponse;

import java.util.List;

public interface SupportService {

    SupportResponse supportBucketList(Long bucketListId, SupportRequest request, Member supporter);

    List<SupportResponse> getBucketListSupports(Long bucketListId, Member member);
}