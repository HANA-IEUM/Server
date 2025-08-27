package com.hanaieum.server.domain.group.entity;

import com.hanaieum.server.common.entity.BaseEntity;
import com.hanaieum.server.domain.member.entity.Member;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "groups_table")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Group extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // PK

    @Column(nullable = false, length = 20)
    private String groupName;

    @Column(nullable = false, unique = true)
    private String inviteCode;

    @Column(nullable = false)
    private boolean isActive = true;

    @OneToMany(mappedBy = "group")
    @Builder.Default
    private List<Member> members = new ArrayList<>();
}
