package com.group9.topicmanagement.repository;

import com.group9.topicmanagement.model.registration.TopicRegistration;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

/** Scalar dashboard queries: do not hydrate every registration, ballot and score. */
public interface EvaluationStatisticsRepository extends Repository<TopicRegistration, Long> {
    // The aliases r/e denote the registration and ballot in the enclosing query.
    // Keep nullable ballot associations inside EXISTS: implicit inner joins outside
    // the OR would incorrectly discard reviewer or council ballots.
    String CURRENT_BALLOT = """
            and (
                (e.evaluationType = 'REVIEWER' and exists (
                    select ra.id from ReviewerAssignment ra
                    where ra = e.reviewerAssignment and ra.topic = r.topic
                      and ra.reviewer = e.evaluator and ra.status <> 'CANCELLED'
                ))
                or (e.evaluationType = 'COUNCIL_MEMBER' and exists (
                    select m.id from CouncilMember m
                    where m = e.councilMember and m.member = e.evaluator and exists (
                        select ca.id from CouncilAssignment ca
                        where ca.topic = r.topic and ca.council = m.council
                          and ca.council.status in ('ACTIVE', 'COMPLETED')
                          and not exists (
                              select better.id from CouncilAssignment better
                              where better.topic = ca.topic
                                and better.council.status in ('ACTIVE', 'COMPLETED')
                                and (
                                    (better.council.status = 'ACTIVE' and ca.council.status = 'COMPLETED')
                                    or (better.council.status = ca.council.status and better.id > ca.id)
                                )
                          )
                    )
                ))
            )
            """;

    // Same meaning as hasMissingMandatoryScores: no current submitted ballot,
    // or any current submitted/locked ballot missing an active mandatory criterion.
    @Query("""
            select count(r) from TopicRegistration r
            where r.status = 'APPROVED' and (
                not exists (
                    select e.id from Evaluation e
                    where e.topic = r.topic and e.status in ('SUBMITTED', 'LOCKED')
            """ + CURRENT_BALLOT + """
                )
                or exists (
                    select e.id from Evaluation e
                    where e.topic = r.topic and e.status in ('SUBMITTED', 'LOCKED')
            """ + CURRENT_BALLOT + """
                      and exists (
                          select criterion.id from EvaluationCriterion criterion
                          where criterion.registrationPeriod = r.topic.registrationPeriod
                            and criterion.isActive = true and criterion.isMandatory = true
                            and not exists (
                                select score.id from EvaluationScore score
                                where score.evaluation = e and score.criterion = criterion
                            )
                      )
                )
            )
            """)
    long countApprovedWithMissingMandatoryScores();
}
