package com.example.english_app.service.speaking;

import com.example.english_app.entity.speaking.*;
import com.example.english_app.repository.speaking.*;
import com.example.english_app.repository.user.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.context.annotation.*;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.orm.jpa.*;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;

/** Opt-in: runs only against the disposable localhost test cluster, never application credentials. */
@EnabledIfEnvironmentVariable(named="SPEAKING_TEST_JDBC_URL",matches="jdbc:postgresql://127\\.0\\.0\\.1:55439/postgres")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SpeakingStorePostgresTest {
    static DataSource source;
    static String schema;
    AnnotationConfigApplicationContext context;
    SpeakingStore store;
    JdbcTemplate jdbc;
    Short scenarioId;
    @Configuration @EnableTransactionManagement
    @EnableJpaRepositories(basePackages={"com.example.english_app.repository.speaking","com.example.english_app.repository.user"})
    static class Config {
        @Bean DataSource dataSource() { return source; }
        @Bean LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource ds) {
            var factory=new LocalContainerEntityManagerFactoryBean();
            factory.setDataSource(ds);
            factory.setPackagesToScan("com.example.english_app.entity");
            factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
            factory.setJpaPropertyMap(Map.of("hibernate.hbm2ddl.auto","none","hibernate.show_sql","false"));
            return factory;
        }
        @Bean PlatformTransactionManager transactionManager(jakarta.persistence.EntityManagerFactory emf) { return new JpaTransactionManager(emf); }
        @Bean JdbcTemplate jdbcTemplate(DataSource ds) { return new JdbcTemplate(ds); }
        @Bean SpeakingJson speakingJson() { return new SpeakingJson(new ObjectMapper()); }
        @Bean SpeakingStore store(SpeakingSessionRepository sessions, SpeakingScenarioRepository scenarios, SpeakingTurnRepository turns, UserRepository users, JdbcTemplate jdbc, SpeakingJson json) {
            return new SpeakingStore(sessions,scenarios,turns,users,jdbc,json);
        }
    }
    @BeforeAll void initialize() throws Exception {
        String url=System.getenv("SPEAKING_TEST_JDBC_URL");
        schema="speaking_test_"+UUID.randomUUID().toString().replace("-","");
        var admin=new DriverManagerDataSource(url,"phase123","");
        new JdbcTemplate(admin).execute("create schema "+schema);
        source=new DriverManagerDataSource(url+"?currentSchema="+schema,"phase123","");
        try(var connection=source.getConnection()) {
            for(String file:List.of("V1__init.sql","V33__durable_speaking_sessions.sql","V34__speaking_scenario_library.sql","V35__speaking_job_audit_and_invariants.sql")) {
                String sql=Files.readString(Path.of("src/main/resources/db/migration",file),StandardCharsets.UTF_8)
                    .replace("public.",schema+".").replace("SELECT pg_catalog.set_config('search_path', '', false);","SET search_path TO "+schema+";");
                ScriptUtils.executeSqlScript(connection,new ByteArrayResource(sql.getBytes(StandardCharsets.UTF_8)));
            }
        }
        context=new AnnotationConfigApplicationContext(Config.class);
        store=context.getBean(SpeakingStore.class);
        jdbc=context.getBean(JdbcTemplate.class);
        jdbc.update("insert into users (id,email,full_name,is_active,locale,password_hash,role,provider,created_at,updated_at) values (1,'one@test.invalid','One',true,'en','unused','STUDENT','LOCAL',now(),now()),(2,'two@test.invalid','Two',true,'en','unused','STUDENT','LOCAL',now(),now())");
        scenarioId=jdbc.queryForObject("select min(id) from speaking_scenarios",Short.class);
    }
    @AfterAll void close() {
        if(context!=null) context.close();
        if(source!=null && schema!=null && schema.matches("speaking_test_[a-f0-9]{32}"))
            new JdbcTemplate(source).execute("drop schema "+schema+" cascade");
    }
    @BeforeEach void reset() {
        jdbc.execute("truncate speaking_job_attempts,speaking_jobs,speaking_turns,speaking_sessions,student_stats restart identity");
    }
    private SpeakingSession readySession() {
        var session=store.start(1L,scenarioId);
        var greeting=store.claim();
        store.responseDone(greeting,"Welcome!",new byte[]{1,2,3});
        return session;
    }
    @Test void seedsThirtyCompleteScenariosWithC1() {
        assertThat(jdbc.queryForObject("select count(*) from speaking_scenarios where cefr_level in ('A2','B1','B2','C1') and jsonb_array_length(hint_phrases_json) between 3 and 5",Integer.class)).isEqualTo(30);
        assertThat(jdbc.queryForObject("select count(*) from speaking_scenarios where cefr_level='C1'",Integer.class)).isEqualTo(5);
        assertThat(jdbc.queryForObject("select title_vi from speaking_scenarios where title_en='Restaurant dinner'",String.class)).isEqualTo("Gọi món nhà hàng");
    }
    @Test void rejectsCrossAccountAndInactiveScenario() {
        var s=readySession();
        assertThatThrownBy(()->store.owned(s.getId(),2L)).isInstanceOf(com.example.english_app.exception.AppException.class);
        assertThatThrownBy(()->store.submit(s.getId(),2L,"request01","a",null,null,"hello")).isInstanceOf(com.example.english_app.exception.AppException.class);
        assertThatThrownBy(()->store.hint(s.getId(),2L,"hintkey01")).isInstanceOf(com.example.english_app.exception.AppException.class);
        assertThatThrownBy(()->store.end(s.getId(),2L)).isInstanceOf(com.example.english_app.exception.AppException.class);
        jdbc.update("update speaking_scenarios set is_active=false where id=?",scenarioId);
        try { assertThatThrownBy(()->store.start(1L,scenarioId)).isInstanceOf(com.example.english_app.exception.AppException.class); }
        finally { jdbc.update("update speaking_scenarios set is_active=true where id=?",scenarioId); }
    }
    @Test void concurrentDuplicateInputCreatesOneTurnAndOneJob() throws Exception {
        var s=readySession();
        try(var pool=Executors.newFixedThreadPool(2)) {
            var gate=new CountDownLatch(1);
            Callable<Long> submit=()->{ gate.await(); return store.submit(s.getId(),1L,"request01","same",null,null,"hello").getId(); };
            var a=pool.submit(submit); var b=pool.submit(submit); gate.countDown();
            assertThat(a.get(10,TimeUnit.SECONDS)).isEqualTo(b.get(10,TimeUnit.SECONDS));
        }
        assertThat(jdbc.queryForObject("select count(*) from speaking_turns where speaker='STUDENT'",Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("select count(*) from speaking_jobs where kind='INPUT'",Integer.class)).isEqualTo(1);
        assertThatThrownBy(()->store.submit(s.getId(),1L,"request01","changed",null,null,"different")).isInstanceOf(com.example.english_app.exception.AppException.class);
    }
    @Test void hintsAndCompletionAwardOnlyOnce() throws Exception {
        var s=readySession();
        store.hint(s.getId(),1L,"hintkey01"); store.hint(s.getId(),1L,"hintkey01");
        assertThat(store.session(s.getId()).getHintUsedCount()).isEqualTo((short)1);
        var input=store.submit(s.getId(),1L,"request01","same",null,null,"I would like fish.");
        store.inputDone(store.claim(),"I would like fish.","{\"wordCount\":5}");
        store.responseDone(store.claim(),"Of course.",new byte[]{1});
        store.evaluationDone(store.claim(),new ObjectMapper().readTree("{\"grammar_errors\":[],\"vocabulary_suggestions\":[]}"));
        store.end(s.getId(),1L); store.end(s.getId(),1L);
        assertThat(jdbc.queryForObject("select count(*) from speaking_jobs where kind='SESSION_EVALUATION'",Integer.class)).isEqualTo(1);
        var job=store.claim();
        var report=new ObjectMapper().readTree("{\"task_completion_score\":100}");
        store.reportDone(job,report,null,null); store.reportDone(job,report,null,null);
        assertThat(jdbc.queryForObject("select total_xp from student_stats where student_id=1",Integer.class)).isEqualTo(28);
        assertThat(store.end(s.getId(),1L).getStatus()).isEqualTo("COMPLETED");
        assertThat(store.history(s.getId()).stream().filter(t->t.getId().equals(input.getId())).findFirst().orElseThrow().getGrammarErrorsJson()).isEqualTo("[]");
        assertThatThrownBy(()->store.submit(s.getId(),1L,"request02","new",null,null,"hello again")).isInstanceOf(com.example.english_app.exception.AppException.class);
    }
    @Test void staleWorkerCannotOverwriteReclaimedJob() {
        var s=store.start(1L,scenarioId);
        var stale=store.claim();
        jdbc.update("update speaking_jobs set lease_expires_at=now()-interval '1 second' where id=?",stale.id());
        var current=store.claim();
        store.responseDone(stale,"obsolete",new byte[]{1});
        assertThat(store.turn(stale.turnId()).getStatus()).isEqualTo("PENDING");
        store.responseDone(current,"current",new byte[]{2});
        assertThat(store.turn(current.turnId()).getTranscriptText()).isEqualTo("current");
        assertThat(store.history(s.getId())).hasSize(1);
    }
}
