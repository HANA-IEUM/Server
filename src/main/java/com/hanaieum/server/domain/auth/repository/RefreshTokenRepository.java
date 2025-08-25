package com.hanaieum.server.domain.auth.repository;

import com.hanaieum.server.domain.auth.entity.RefreshToken;
import com.hanaieum.server.domain.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByMember(Member member);

    Optional<RefreshToken> findByToken(String token);

    @Modifying
    @Transactional
    void deleteByMember(Member member);

}
