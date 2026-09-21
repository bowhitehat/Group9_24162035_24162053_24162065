package com.group9.topicmanagement.repository;

import com.group9.topicmanagement.domain.council.CouncilMember;
import com.group9.topicmanagement.domain.enums.CouncilMemberRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CouncilMemberRepository extends JpaRepository<CouncilMember, Long> {
    List<CouncilMember> findByCouncilId(Long councilId);
    boolean existsByCouncilIdAndMemberId(Long councilId, Long memberId);

    @Query("SELECT m FROM CouncilMember m JOIN FETCH m.member WHERE m.council.id = :councilId")
    List<CouncilMember> findByCouncilIdWithMember(@Param("councilId") Long councilId);

    Optional<CouncilMember> findByCouncilIdAndMemberId(Long councilId, Long memberId);

    @Query("SELECT m FROM CouncilMember m JOIN FETCH m.council WHERE m.member.id = :memberId AND m.role = :role")
    List<CouncilMember> findByMemberIdAndRole(@Param("memberId") Long memberId, @Param("role") CouncilMemberRole role);

    long countByCouncilIdAndRole(Long councilId, CouncilMemberRole role);
}
