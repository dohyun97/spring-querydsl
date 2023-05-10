package study.querydsl.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.entity.Member;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest
@Transactional
class MemberRepositoryTest {
    @Autowired
    MemberRepository repository;
    @Test
    void basicTest(){
        Member member = Member.builder().username("member1").age(10).build();
        repository.save(member);

        List<Member> findAll = repository.findAll();
        assertThat(findAll).containsExactly(member);


        Member findMember1 = repository.findById(member.getId()).get();
        assertThat(findMember1).isEqualTo(member);

        List<Member> findMember2 = repository.findByUsername("member1");
        assertThat(findMember2).containsExactly(member);

    }
}