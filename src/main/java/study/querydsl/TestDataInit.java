package study.querydsl;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.entity.Member;
import study.querydsl.entity.Team;
import study.querydsl.repository.MemberJpaRepository;

import javax.persistence.EntityManager;

@Profile("local")
@RequiredArgsConstructor
@Component
public class TestDataInit {
    private final MemberJpaRepository memberRepository;
    private final EntityManager em;

    /**
     * 확인용 초기 데이터 추가
     */
    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void initData() {
        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");
        em.persist(teamB);
        em.persist(teamA);

        for (int i = 0; i < 100; i++){
            Team selectedTeam = i % 2 == 0 ? teamA : teamB;
            memberRepository.save(Member.builder().username("member"+i).team(selectedTeam).age(i).build());
        }
    }
}
