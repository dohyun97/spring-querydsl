package study.querydsl.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.dto.MemberSearchCondition;
import study.querydsl.dto.MemberTeamDto;
import study.querydsl.entity.Member;
import study.querydsl.entity.Team;

import javax.persistence.EntityManager;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Transactional
class MemberJpaRepositoryTest {
    @Autowired
    EntityManager em;
    @Autowired
    MemberJpaRepository repository;

    @Test
    void basicTest(){
        Member member = Member.builder().username("member1").age(10).build();
        repository.save(member);

        List<Member> findAll = repository.findAll();
        assertThat(findAll).containsExactly(member);
        List<Member> allQuerydsl = repository.findAll_querydsl();
        assertThat(allQuerydsl).containsExactly(member);

        Member findMember1 = repository.findById(member.getId()).get();
        assertThat(findMember1).isEqualTo(member);

        List<Member> findMember2 = repository.findByUsername("member1");
        assertThat(findMember2).containsExactly(member);
        List<Member> findMemberQuerydsl = repository.findByUsername_querydsl("member1");
        assertThat(findMemberQuerydsl).containsExactly(member);
    }

    @Test
    void searchTest(){
        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");
        em.persist(teamA);
        em.persist(teamB);

        Member member1 = Member.builder().age(10).username("member1").team(teamA).build();
        Member member2 = Member.builder().age(20).username("member2").team(teamA).build();
        Member member3 = Member.builder().age(30).username("member3").team(teamB).build();
        Member member4 = Member.builder().age(40).username("member4").team(teamB).build();
        em.persist(member1);
        em.persist(member2);
        em.persist(member3);
        em.persist(member4);

        MemberSearchCondition condition = new MemberSearchCondition();
        condition.setAgeLoe(40);
        condition.setAgeGoe(35);
        condition.setTeamName("teamB");

        //List<MemberTeamDto> result = repository.searchByBuilder(condition);
        List<MemberTeamDto> result = repository.search(condition);
        assertThat(result).extracting("username").containsExactly("member4");
    }

}