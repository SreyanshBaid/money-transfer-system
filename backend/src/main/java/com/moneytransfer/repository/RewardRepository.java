package com.moneytransfer.repository;

import com.moneytransfer.domain.entity.Reward;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RewardRepository extends JpaRepository<Reward, Long> {

    List<Reward> findByUserIdOrderByCreatedAtDesc(Long userId);

    @Query("SELECT COALESCE(SUM(r.points), 0) FROM Reward r WHERE r.userId = :userId")
    int getTotalPointsByUserId(@Param("userId") Long userId);

    long countByUserId(Long userId);

    @Query("SELECT r FROM Reward r WHERE r.userId = :userId ORDER BY r.createdAt DESC")
    List<Reward> findRecentByUserId(@Param("userId") Long userId, org.springframework.data.domain.Pageable pageable);

    List<Reward> findByUserIdAndEntryTypeOrderByCreatedAtDesc(Long userId, String entryType);

    @Query("SELECT COALESCE(SUM(r.points), 0) FROM Reward r WHERE r.userId = :userId AND r.entryType = 'GRANT'")
    int getTotalEarnedByUserId(@Param("userId") Long userId);

    @Query("SELECT COALESCE(SUM(r.points), 0) FROM Reward r WHERE r.userId = :userId AND r.entryType = 'REDEEM'")
    int getTotalRedeemedByUserId(@Param("userId") Long userId);
}
