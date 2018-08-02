package org.cboard.config;

import com.alibaba.druid.pool.DruidDataSource;
import org.cboard.security.handler.LoginHandler;
import org.cboard.security.handler.LogoutHandler;
import org.cboard.security.service.DbUserDetailService;
import org.cboard.security.service.DefaultAuthenticationService;
import org.cboard.security.service.ShareAuthenticationProviderDecorator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.authentication.encoding.Md5PasswordEncoder;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

/**
 * @author WangKun
 * @create 2018-07-25
 * @desc
 **/
@EnableWebSecurity
@Order(2)
@Import(DataSourceConfig.class)
public class SecurityJDBCConfig extends WebSecurityConfigurerAdapter {

    private static final Logger LOGGER = LoggerFactory.getLogger(SecurityJDBCConfig.class);

    @Autowired
    private DruidDataSource druidDataSource;

    @Bean
    public Md5PasswordEncoder md5PasswordEncoder() {
        return new Md5PasswordEncoder();
    }

    @Bean
    public DefaultAuthenticationService defaultAuthenticationService() {
        return new DefaultAuthenticationService();
    }

    @Bean
    public DbUserDetailService dbUserDetailService() {
        DbUserDetailService dbUserDetailService = new DbUserDetailService();
        dbUserDetailService.setDataSource(druidDataSource);
        dbUserDetailService.setAuthoritiesByUsernameQuery("SELECT login_name username, 'admin' AS authority FROM dashboard_user WHERE login_name = ?");
        dbUserDetailService.setUsersByUsernameQuery("SELECT user_id,user_name,login_name, user_password, 1 AS enabled FROM dashboard_user WHERE login_name = ? ");
        return dbUserDetailService;
    }

    @Bean
    public DaoAuthenticationProvider daoAuthenticationProvider() {
        DaoAuthenticationProvider daoAuthenticationProvider = new DaoAuthenticationProvider();
        daoAuthenticationProvider.setUserDetailsService(dbUserDetailService());
        daoAuthenticationProvider.setPasswordEncoder(md5PasswordEncoder());
        return daoAuthenticationProvider;
    }

    @Bean
    public ShareAuthenticationProviderDecorator shareAuthenticationProviderDecorator() {
        ShareAuthenticationProviderDecorator share = new ShareAuthenticationProviderDecorator();
        share.setAuthenticationProvider(daoAuthenticationProvider());
        return share;
    }

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.authenticationProvider(shareAuthenticationProviderDecorator());
    }

    @Override
    public void configure(WebSecurity web) throws Exception {
        web.ignoring().antMatchers("/lib/**", "/dist/**", "/bootstrap/**", "/plugins/**", "/css/**");
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.authorizeRequests()
                .anyRequest().authenticated();
        http.sessionManagement()
                .sessionFixation().migrateSession()
                .maximumSessions(1)
                .maxSessionsPreventsLogin(true)
                .expiredUrl("/login.do?expired-session")
                .and()
                .invalidSessionUrl("/login.do?invalid-session");
        http.formLogin()
                .loginPage("/login.do")
                .failureUrl("/login.do?error")
                .usernameParameter("username")
                .passwordParameter("password")
                .successHandler(new LoginHandler())
                .permitAll();
        http.logout()
                .deleteCookies("JSESSIONID")
                .invalidateHttpSession(true)
                .logoutUrl("/j_spring_cas_security_logout")
                .logoutSuccessHandler(new LogoutHandler())
                .permitAll();
        http.httpBasic();
        http.rememberMe().rememberMeParameter("remember_me");
        http.csrf().disable();
    }

}
