package study.querydsl;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Commit;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.dto.MemberDto;
import study.querydsl.dto.QMemberDto;
import study.querydsl.dto.UserDto;
import study.querydsl.entity.Member;
import study.querydsl.entity.QMember;
import study.querydsl.entity.QTeam;
import study.querydsl.entity.Team;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static study.querydsl.entity.QMember.*;
import static study.querydsl.entity.QTeam.*;

@SpringBootTest
@Transactional
public class QuerydslBasicTest {
    @Autowired
    EntityManager em;
    @Autowired
    EntityManagerFactory emf;

    JPAQueryFactory queryFactory;

    @BeforeEach
    void before(){
        queryFactory = new JPAQueryFactory(em);
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
    }

    @Test
    void startJPQL(){
        String qlString = "select m from Member m where m.username=:username";
        Member findMember = em.createQuery(qlString, Member.class)
                .setParameter("username", "member1")
                .getSingleResult();
        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    void startQuerydsl(){
        Member findMember = queryFactory
                .select(member)
                .from(member)
                .where(member.username.eq("member1"))
                .fetchOne();
        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    void search(){
        List<Member> findMemeber = queryFactory
                .selectFrom(member)
                .where(member.username.eq("member1")
                        .and(member.age.loe(10))
                )
                .fetch();
        assertThat(findMemeber.size()).isEqualTo(1);
        assertThat(findMemeber.get(0).getUsername()).isEqualTo("member1");
    }
    //AND 생략
    @Test
    void searchAndParam(){
        List<Member> findMember = queryFactory
                .selectFrom(member)
                .where(
                        member.username.eq("member1"),
                        member.age.loe(10)
                )
                .fetch();
        assertThat(findMember.size()).isEqualTo(1);
        assertThat(findMember.get(0).getUsername()).isEqualTo("member1");
    }
    //type of Fetch
    @Test
    void resultFetch(){
        //List
        List<Member> fetch = queryFactory
                .selectFrom(member)
                .fetch();

        //Single result
//        Member findMember1 = queryFactory
//                .selectFrom(member)
//                .fetchOne();

        //first result
        Member findMember2 = queryFactory
                .selectFrom(member)
                .fetchFirst();
        //Paging
        QueryResults<Member> results = queryFactory
                .selectFrom(member)
                .fetchResults();
        //count query
        long fetchCount = queryFactory
                .selectFrom(member)
                .fetchCount();
    }
    /**
     *회원 정렬 순서
     * 1. 회원 나이 내림차순(desc)
     * 2. 회원 이름 올림차순(asc)
     * 단 2에서 회원 이름이 없으면 마지막에 출력(nulls last)
     * */
    @Test
    void sort(){
        em.persist(Member.builder().age(100).build());
        em.persist(Member.builder().username("member5").age(100).build());
        em.persist(Member.builder().username("member6").age(100).build());

        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.eq(100))
                .orderBy(member.age.desc(), member.username.asc().nullsLast())
                .fetch();

        Member member5 = result.get(0);
        Member member6 = result.get(1);
        Member memberNull = result.get(2);
        assertThat(member5.getUsername()).isEqualTo("member5");
        assertThat(member6.getUsername()).isEqualTo("member6");
        assertThat(memberNull.getUsername()).isNull();
    }
    //paging
    @Test
    void paging1(){
        List<Member> result = queryFactory
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1) //0부터 시작
                .limit(2) //2건씩 조회
                .fetch();
        assertThat(result.size()).isEqualTo(2);
    }

    @Test
    void paging2(){
        QueryResults<Member> queryResults = queryFactory
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1)
                .limit(2)
                .fetchResults();  //count 쿼리 실행
        assertThat(queryResults.getTotal()).isEqualTo(4);
        assertThat(queryResults.getOffset()).isEqualTo(1);
        assertThat(queryResults.getLimit()).isEqualTo(2);
        assertThat(queryResults.getResults().size()).isEqualTo(2);
    }

    /**
     * JPQL
     * select
     * COUNT(m), //회원수
     * SUM(m.age), //나이 합
     * AVG(m.age), //평균 나이
     * MAX(m.age), //최대 나이
     * MIN(m.age) //최소 나이 * from Member m
     * */
    @Test
    void aggregation(){
        List<Tuple> result = queryFactory
                .select(member.count(),
                        member.age.sum(),
                        member.age.avg(),
                        member.age.max(),
                        member.age.max()
                )
                .from(member)
                .fetch();

        Tuple tuple = result.get(0);
        assertThat(tuple.get(member.count())).isEqualTo(4);
        assertThat(tuple.get(member.age.sum())).isEqualTo(100);
        assertThat(tuple.get(member.age.avg())).isEqualTo(25);
        assertThat(tuple.get(member.age.max())).isEqualTo(40);
        assertThat(tuple.get(member.age.min())).isEqualTo(10);
    }
    /**
     * 팀의 이름과 각 팀의 평균 연령을 구해라.(groupBy)
     */
    @Test
    void group(){
        List<Tuple> result = queryFactory
                .select(team.name, member.age.avg())
                .from(member)
                .join(member.team, team)
                .groupBy(team.name)
                .fetch();
        Tuple teamA = result.get(0);
        Tuple teamB = result.get(1);

        assertThat(teamA.get(team.name)).isEqualTo("teamA");
        assertThat(teamA.get(member.age.avg())).isEqualTo(15);
        assertThat(teamB.get(team.name)).isEqualTo("teamB");
        assertThat(teamB.get(member.age.avg())).isEqualTo(35);
    }

    /**
     *팀A에 소속된 모든 회원
     */
    @Test
    void join(){
        List<Member> result = queryFactory
                .selectFrom(member)
                .join(member.team, team)
                .where(team.name.eq("teamA"))
                .fetch();
        assertThat(result).extracting("username").containsExactly("member1","member2");
    }

    /**
     * 세타 조인(연관관계가 없는 필드로 조인)
     * 회원의 이름이 팀 이름과 같은 회원 조회
     */
    @Test
    void theta_join(){
        em.persist(Member.builder().username("teamA").build());
        em.persist(Member.builder().username("teamB").build());
        List<Member> result = queryFactory
                .select(member)
                .from(member, team)
                .where(member.username.eq(team.name))
                .fetch();
        for (Member m : result) {
            System.out.println("m = "+m);
        }
        assertThat(result).extracting("username").containsExactly("teamA","teamB");
    }

    /**
     * 예) 회원과 팀을 조인하면서, 팀 이름이 teamA인 팀만 조인, 회원은 모두 조회
     * JPQL: SELECT m, t FROM Member m LEFT JOIN m.team t on t.name = 'teamA'
     * SQL: SELECT m.*, t.* FROM Member m LEFT JOIN Team t ON m.TEAM_ID=t.id and
     t.name='teamA'
     */
    @Test
    void join_on_filtering(){

        List<Tuple> result = queryFactory
                .select(member, team)
                .from(member)
                .leftJoin(member.team, team).on(team.name.eq("teamA"))
                .fetch();
        for (Tuple tuple : result) {
            System.out.println("t = "+tuple);
        }
    }

    /**
     *2. 연관관계 없는 엔티티 외부 조인
     *예)회원의 이름과 팀의 이름이 같은 대상 외부 조인
     * JPQL: SELECT m, t FROM Member m LEFT JOIN Team t on m.username = t.name
     * SQL: SELECT m.*, t.* FROM Member m LEFT JOIN Team t ON m.username = t.name */
    @Test
    void join_on_no_relation(){
        em.persist(Member.builder().username("teamA").build());
        em.persist(Member.builder().username("teamB").build());
        List<Tuple> result = queryFactory
                .select(member, team)
                .from(member)
                .leftJoin(team).on(member.username.eq(team.name))
                .fetch();
        for (Tuple tuple : result) {
            System.out.println("t = "+tuple);
        }
    }

    /**
     * fetch join 미적용
     */
    @Test
    void fetchJoinNo(){
        em.flush();
        em.clear();

        Member findMember = queryFactory
                .select(member)
                .from(member)
                .where(member.username.eq("member1"))
                .fetchOne();
        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());
        assertThat(loaded).as("Did not use fetch join").isFalse();
    }

    /**
     * fetch join 적용
     */
    @Test
    void fetchJoin(){
        em.flush();
        em.clear();

        Member findMember = queryFactory
                .select(member)
                .from(member)
                .join(member.team,team).fetchJoin()
                .where(member.username.eq("member1"))
                .fetchOne();
        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());
        assertThat(loaded).as("use fetch join").isTrue();
    }

    /**
     * 나이가 가장 많은 회원 조회
     */
    @Test
    public void subQuery(){
        QMember memberSub = new QMember("memberSub");

        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.eq(
                        JPAExpressions
                                .select(memberSub.age.max())
                                .from(memberSub)
                ))
                .fetch();
        assertThat(result).extracting("age").containsExactly(40);
    }
    /**
     *나이가 평균 나이 이상인 회원
     */
    @Test
    public void subQueryGoe(){
        QMember memberSub = new QMember("memberSub");
        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.goe(
                        JPAExpressions
                                .select(memberSub.age.avg())
                                .from(memberSub)
                ))
                .fetch();
        assertThat(result).extracting("age").containsExactly(30,40);
    }
    /**
     * 서브쿼리 여러 건 처리, in 사용
     */
    @Test
    public void subQueryIn(){
        QMember memberSub = new QMember("memberSub");
        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.in(
                        JPAExpressions
                                .select(memberSub.age)
                                .from(memberSub)
                                .where(memberSub.age.gt(10))
                ))
                .fetch();
        assertThat(result).extracting("age").containsExactly(20,30,40);
    }
    /**
     * select 절에 subquery
     */
    @Test
    void subquerySelect(){
        QMember memberSub = new QMember("memberSub");
        List<Tuple> fetch = queryFactory
                .select(member.username,
                        JPAExpressions  //이부분 import 가능
                                .select(memberSub.age.avg())
                                .from(memberSub)
                )
                .from(member)
                .fetch();
        for (Tuple tuple : fetch) {
            System.out.println("username = "+tuple.get(member.username));
            System.out.println("age = "+tuple.get(JPAExpressions.select(memberSub.age.avg()).from(memberSub)));
        }
    }
    /**
     * case
     */
    @Test
    void basicCase(){
        List<String> result = queryFactory
                .select(member.age
                        .when(10).then("10 years old")
                        .when(20).then("20 years old")
                        .otherwise("ect"))
                .from(member)
                .fetch();
        for (String s : result) {
            System.out.println("s = "+s);
        }
    }

    @Test
    void  complicateCase(){
        List<String> result = queryFactory
                .select(new CaseBuilder()
                        .when(member.age.between(0, 20)).then("0~20")
                        .when(member.age.between(21, 30)).then("21~30")
                        .otherwise("etc"))
                .from(member)
                .fetch();
        for (String s : result) {
            System.out.println("s = "+s);
        }
    }

    /**
     * 0~30살 아닌 회원을 가장 먼저 출력
     * 0~20살 회원 그다음 출력
     * 21~30살 회원 마지막으로 출력
     */
    @Test
    void rankCase(){
        NumberExpression<Integer> rankPath = new CaseBuilder()
                .when(member.age.between(0, 20)).then(2)
                .when(member.age.between(21, 30)).then(1)
                .otherwise(3);
        List<Tuple> result = queryFactory
                .select(member.username, member.age, rankPath)
                .from(member)
                .orderBy(rankPath.desc())
                .fetch();
        for (Tuple t : result) {
            String username = t.get(member.username);
            Integer age = t.get(member.age);
            Integer rank = t.get(rankPath);
            System.out.println("username = "+username+" age = "+age+" rank = "+rank);
        }
    }

    //concat
    @Test
    void concat(){
        List<Tuple> result = queryFactory
                .select(member.username, Expressions.constant("A"))
                .from(member)
                .fetch();
        for (Tuple tuple : result) {
            System.out.println("t = "+tuple);
        }
    }
    //add string concat
    @Test
    void concatAddString(){
        String result = queryFactory
                .select(member.username.concat("_").concat(member.age.stringValue()))
                .from(member)
                .where(member.username.eq("member1"))
                .fetchOne();
        System.out.println("result = "+result);

    }

    //DTO
    @Test
    void findDtoByJpql(){
        List<MemberDto> result = em.createQuery("select new study.querydsl.dto.MemberDto(m.username,m.age) from Member m", MemberDto.class)
                .getResultList();
        for (MemberDto memberDto : result) {
            System.out.println("memberDto = "+memberDto);
        }
    }
    @Test
    void findDtoByProperty(){
        List<MemberDto> results = queryFactory
                .select(Projections.bean(MemberDto.class, member.username, member.age))
                .from(member)
                .fetch();
        for (MemberDto result : results) {
            System.out.println("memberDto = "+result);
        }
    }
    @Test
    void findDtoByField(){
        List<MemberDto> results = queryFactory
                .select(Projections.fields(MemberDto.class, member.username, member.age))
                .from(member)
                .fetch();
        for (MemberDto result : results) {
            System.out.println("memberDto = "+result);
        }
    }
    @Test
    void findDtoByConstructor(){
        List<MemberDto> results = queryFactory
                .select(Projections.constructor(MemberDto.class, member.username, member.age))
                .from(member)
                .fetch();
        for (MemberDto result : results) {
            System.out.println("memberDto = "+result);
        }
    }
    //별칭 사용
    @Test
    void findUserDto() {
        QMember memberSub = new QMember("memberSub");
        List<UserDto> results = queryFactory
                .select(Projections.fields(UserDto.class, member.username.as("name"),
                        ExpressionUtils.as(
                                JPAExpressions
                                        .select(memberSub.age.max())
                                        .from(memberSub),"age"
                        )))
                .from(member)
                .fetch();
        for (UserDto result : results) {
            System.out.println("memberDto = " + result);
        }
    }
    @Test
    void findDtoByQueryProjection(){
        List<MemberDto> result = queryFactory
                .select(new QMemberDto(member.username, member.age))
                .from(member)
                .fetch();
        for (MemberDto memberDto : result) {
            System.out.println("memberDto = "+memberDto);
        }
    }

    //dynamic query
    @Test
    void dynamicQuery_BooleanBuilder(){
        String usernameParam = "member1";
        Integer ageParam = 10;
        List<Member> result = searchMember1(usernameParam, ageParam);
        assertThat(result.size()).isEqualTo(1);
    }
    private List<Member> searchMember1(String usernameParam, Integer ageParam){
        BooleanBuilder builder = new BooleanBuilder();
        if(usernameParam != null){
            builder.and(member.username.eq(usernameParam));
        }
        if(ageParam != null){
            builder.and(member.age.eq(ageParam));
        }
        return queryFactory
                .select(member)
                .from(member)
                .where(builder)
                .fetch();
    }

    @Test
    void dynamicQuery(){
        String usernameParam = "member1";
        Integer ageParam = 10;
        List<Member> result = searchMember1(usernameParam, ageParam);
        assertThat(result.size()).isEqualTo(1);
    }
    private List<Member> searchMember2(String usernameParam,Integer ageParam){
        return queryFactory
                .select(member)
                .from(member)
                //.where(usernameEq(usernameParam),ageEq(ageParam))
                .where(allEq(usernameParam,ageParam))
                .fetch();
    }
    private BooleanExpression ageEq(Integer ageParam) {
        return ageParam != null ? member.age.eq(ageParam) : null;
    }
    private BooleanExpression usernameEq(String usernameParam) {
        return usernameParam != null ? member.username.eq(usernameParam) : null;
    }
    private BooleanExpression allEq(String usernameParam, Integer ageParam){
        return usernameEq(usernameParam).and(ageEq(ageParam));
    }

    //bulk
    @Test
    //@Commit
    void bulkUpdate(){
        long count = queryFactory
                .update(member)
                .set(member.username, "unknown")
                .where(member.age.lt(28))
                .execute();
        //영속성 컨텍스트 값 초기화. 벌크 연산은 영속성 컨텍스 값은 안 바뀌고 db 값 만 바껴서
        em.flush();
        em.clear();

        List<Member> result = queryFactory
                .select(member)
                .from(member)
                .fetch();
        for (Member m : result) {
            System.out.println("member = "+m);
        }
    }
    @Test
    void bulkAdd(){
        long count = queryFactory
                .update(member)
                .set(member.age, member.age.add(1))
                .execute();
    }
    @Test
    void bulkDelete(){
        long count = queryFactory
                .delete(member)
                .where(member.age.gt(18))
                .execute();
    }

    //SQL function
    @Test
    void sqlFunction(){
        List<String> result = queryFactory
                .select(member.username)
                .from(member)
                //.where(member.username.eq(Expressions.stringTemplate("function('lower', {0})", member.username)))
                .where(member.username.eq(member.username.lower()))
                .fetch();
    }

}
