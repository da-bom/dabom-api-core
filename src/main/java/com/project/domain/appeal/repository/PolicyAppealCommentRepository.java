package com.project.domain.appeal.repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.project.domain.appeal.entity.PolicyAppealComment;

/** 이의제기 상세 조회(댓글 조회) 저장소 */
public interface PolicyAppealCommentRepository extends JpaRepository<PolicyAppealComment, Long> {

    /** 이의제기 상세 조회(댓글 커서) 조회 */
    @Query(
            """
            select pac
            from PolicyAppealComment pac
            where pac.appealId = :appealId
              and (:cursorId is null or pac.id < :cursorId)
              and pac.deletedAt is null
            order by pac.id desc
            """)
    List<PolicyAppealComment> findByAppealIdAndIdLessThanOrderByIdDesc(
            @Param("appealId") Long appealId, @Param("cursorId") Long cursorId, Pageable pageable);
}
