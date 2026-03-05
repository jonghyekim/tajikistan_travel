package egovframework.example.domain;

import javax.persistence.*;
import lombok.Getter; 
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity
@Getter
@Setter
public class Member {
	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;
    
    @Column(nullable = false, unique = true)
    private String email;
    
    @Column(nullable = false)
    private String passwordHash;
 
    @Column(nullable = false, unique = true)
    private String nickname;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MemberStatus status; //ACTIVE, DELETED
    
    @Column
    private Instant deletedAt;
    
    @CreationTimestamp
    @Column
    private Instant createdAt;

    @UpdateTimestamp
    @Column
    private Instant updatedAt;
    
}


