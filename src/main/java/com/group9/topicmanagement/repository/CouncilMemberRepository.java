package com.group9.topicmanagement.repository;

import com.group9.topicmanagement.model.council.CouncilMember;
import com.group9.topicmanagement.model.enums.CouncilMemberRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CouncilMemberRepository extends JpaRepository<CouncilMember, Long> {
    @EntityGraph(attributePaths = {"member"})
    List<CouncilMember> findByCouncilId(Long councilId);
    boolean existsByCouncilIdAndMemberId(Long councilId, Long memberId);

    Optional<CouncilMember> findByCouncilIdAndMemberId(Long councilId, Long memberId);

    @EntityGraph(attributePaths = {"council"})
    List<CouncilMember> findByMemberIdAndRole(Long memberId, CouncilMemberRole role);

    long countByCouncilIdAndRole(Long councilId, CouncilMemberRole role);
}
