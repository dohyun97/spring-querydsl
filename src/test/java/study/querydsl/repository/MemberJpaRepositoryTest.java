package study.querydsl.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.entity.Member;

import javax.persistence.EntityManager;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
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

}