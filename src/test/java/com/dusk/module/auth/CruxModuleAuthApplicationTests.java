package com.dusk.module.auth;

import com.hankcs.hanlp.HanLP;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.QBean;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.Getter;
import lombok.Setter;
import com.dusk.common.framework.dto.PagedAndSortedInputDto;
import com.dusk.common.framework.jpa.querydsl.QBeanBuilder;
import com.dusk.common.framework.tenant.TenantContextHolder;
import com.dusk.module.auth.entity.QRole;
import com.dusk.module.auth.entity.QUser;
import com.dusk.module.auth.entity.User;
import com.dusk.module.auth.service.impl.UserServiceImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Iterator;
import java.util.List;

@SpringBootTest
class CruxModuleAuthApplicationTests {

    @Test
    void contextLoads() {
        //test
    }

    //多音字组件测试用例
    @Test
    void duoyinziTest(){
        String result = HanLP.convertToPinyinString("调试员", "", false);
        Assertions.assertEquals(result,"tiaoshiyuan");

        result=HanLP.convertToPinyinString("银行","",false);
        Assertions.assertEquals(result,"yinhang");
    }

    @Autowired
    UserServiceImpl userService;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
        //@Transactional
    void resetUserPassword(){
        long tenantId = Long.valueOf("1278479482871754754");
        TenantContextHolder.setTenantId(tenantId);
        try{
            List<User> userList =  userService.getAllUsers();
            Iterator<User> it = userList.iterator();
            while (it.hasNext()){
                User user = it.next();
                if(user.getUserName().equals("admin")){
                    it.remove();
                    continue;
                }
                String userName = user.getUserName();
                char[] chars = userName.toCharArray();
                chars[0] -=32;
                userName = String.valueOf(chars)+".654123";
                String password = passwordEncoder.encode(userName);
                System.out.println(userName+"\t"+password);
                user.setPassword(password);
                user.setShouldChangePasswordOnNextLogin(true);
                user.setAccessFailedCount(0);
            }
            userService.saveAll(userList);
        }finally {
            TenantContextHolder.clear();
        }
    }

    @Autowired
    JPAQueryFactory queryFactory;


    @Test
    void JpaQueryFactoryUtilTest(){
        PagedAndSortedInputDto pageDto = new PagedAndSortedInputDto();
        pageDto.setSorting("id");
        QUser user = QUser.user;
        QRole role = QRole.role;
        QBean<UserVo> userVoQBean = QBeanBuilder.create(UserVo.class).appendQEntity(user).build();
        JPAQuery<UserVo> query =  queryFactory.select(userVoQBean).from(user);
        userService.page(query,pageDto.getPageable());
        System.out.println();
        JPAQuery<User> query1 =  queryFactory.selectFrom(user);
        userService.page(query1,pageDto.getPageable());
        JPAQuery<Long> query2 =  queryFactory.select(user.id).from(user);
        userService.page(query2,pageDto.getPageable());
        JPAQuery<Tuple> query3 =  queryFactory.select(user.id,user.admin).from(user);
        userService.page(query3,pageDto.getPageable());

        JPAQuery<Tuple> query4 =  queryFactory.select(user,role.id).from(user).innerJoin(user.userRoles,role);
        userService.page(query4,pageDto.getPageable());
    }
    @Setter
    @Getter
    public static class UserVo{
        //private Long id;
        //private Boolean admin;
        private String name;
        private String userName;
    }
}
