package study.querydsl.repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;
import study.querydsl.dto.MemberSearchCondition;
import study.querydsl.dto.MemberTeamDto;
import study.querydsl.dto.QMemberTeamDto;
import study.querydsl.entity.Member;

import javax.persistence.EntityManager;
import java.util.List;
import java.util.Optional;

import static study.querydsl.entity.QMember.member;
import static study.querydsl.entity.QTeam.team;

@Repository
public class MemberJpaRepository {
    private final EntityManager em;
    private final JPAQueryFactory queryFactory;

    public MemberJpaRepository(EntityManager em) {
        this.em = em;
        this.queryFactory = new JPAQueryFactory(em);
    }

    public void save(Member member){
        em.persist(member);
    }

    public Optional<Member> findById(Long id){
        Member member = em.find(Member.class, id);
        return Optional.ofNullable(member);
    }

    public List<Member> findAll(){
        return em.createQuery("select m from Member m", Member.class).getResultList();
    }

    public List<Member> findAll_querydsl(){
        return queryFactory
                .selectFrom(member)
                .fetch();
    }

    public List<Member> findByUsername(String username){
        return em.createQuery("select m from Member m where m.username=:username", Member.class)
                .setParameter("username",username)
                .getResultList();
    }

    public List<Member> findByUsername_querydsl(String username){
        return queryFactory
                .selectFrom(member)
                .where(member.username.eq(username))
                .fetch();
    }

    //search with builder
    public List<MemberTeamDto> searchByBuilder(MemberSearchCondition condition){
        BooleanBuilder builder = new BooleanBuilder();
        if(StringUtils.hasText(condition.getUsername())){
            builder.and(member.username.eq(condition.getUsername()));
        }
        if(StringUtils.hasText(condition.getTeamName())){
            builder.and(team.name.eq(condition.getTeamName()));
        }
        if(condition.getAgeGoe()!=null){
            builder.and(member.age.goe(condition.getAgeGoe()));
        }
        if (condition.getAgeLoe()!=null){
            builder.and(member.age.loe(condition.getAgeLoe()));
        }

        return queryFactory
                .select(new QMemberTeamDto(
                        member.id,
                        member.username,
                        member.age,
                        team.id,
                        team.name
                ))
                .from(member)
                .leftJoin(member.team, team)
                .where(builder)
                .fetch();
    }

    //search in where
    public List<MemberTeamDto> search(MemberSearchCondition condition){
        return queryFactory
                .select(new QMemberTeamDto(
                        member.id,
                        member.username,
                        member.age,
                        team.id,
                        team.name
                ))
                .from(member)
                .leftJoin(member.team,team)
                .where(usernameEq(condition.getUsername()),teanNameEq(condition.getTeamName()),ageGoe(condition.getAgeGoe()),ageLoe(condition.getAgeLoe()))
                //.where(usernameEq(condition.getUsername()),teanNameEq(condition.getTeamName()),ageBetween(condition.getAgeLoe(),condition.getAgeGoe()))
                .fetch();
    }
   private  BooleanExpression ageBetween(int ageLoe, int ageGoe){
        return ageGoe(ageGoe).and(ageLoe(ageLoe));
   }
    private BooleanExpression ageLoe(Integer ageLoe) {
        return ageLoe != null ? member.age.loe(ageLoe) : null;
    }

    private BooleanExpression ageGoe(Integer ageGoe) {
        return ageGoe != null ? member.age.goe(ageGoe) : null;
    }

    private BooleanExpression teanNameEq(String teamName) {
        return StringUtils.isEmpty(teamName) ? null : team.name.eq(teamName);
    }

    private BooleanExpression usernameEq(String username) {
        return StringUtils.hasText(username) ? member.username.eq(username) : null;
    }

}
