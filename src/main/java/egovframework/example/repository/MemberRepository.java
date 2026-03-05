package egovframework.example.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import egovframework.example.domain.Member;
import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long>{
	Optional<Member> findByUsername(String username);
	boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    boolean existsByNickname(String nickname);
}
