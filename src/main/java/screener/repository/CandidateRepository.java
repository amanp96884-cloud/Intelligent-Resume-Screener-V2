package screener.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import screener.models.CandidateResult;

@Repository
public interface CandidateRepository extends JpaRepository<CandidateResult, Long> {
}
