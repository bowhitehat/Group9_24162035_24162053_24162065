package com.group9.topicmanagement.repository;
import com.group9.topicmanagement.domain.council.CouncilMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CouncilMemberRepository extends JpaRepository<CouncilMember, Long> {
    List<CouncilMember> findByCouncilId(Long councilId);
    boolean existsByCouncilIdAndMemberId(Long councilId, Long memberId);
}