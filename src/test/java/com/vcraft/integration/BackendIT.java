package com.vcraft.integration;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import com.vcraft.domain.service.PasswordResetDelivery;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class BackendIT {
    @Container static final PostgreSQLContainer<?> db = new PostgreSQLContainer<>("postgres:15-alpine");
    @DynamicPropertySource static void config(DynamicPropertyRegistry p) {
        p.add("spring.datasource.url", db::getJdbcUrl); p.add("spring.datasource.username", db::getUsername);
        p.add("spring.datasource.password", db::getPassword);
        p.add("jwt.secret", () -> "integration-tests-only-not-a-production-secret-12345");
    }
    @Autowired TestRestTemplate http;
    @Autowired JdbcTemplate sql;
    @MockitoBean PasswordResetDelivery delivery;
    Map<String,String> mailed = new ConcurrentHashMap<>();
    String admin;
    @BeforeEach void setup() {
        when(delivery.available()).thenReturn(true);
        doAnswer(i -> { mailed.put(i.getArgument(0), i.getArgument(1)); return null; }).when(delivery).send(anyString(), anyString());
        admin = token(map(call("POST", "/auth/login", Map.of("email","admin@vcraft.vn","password","uet123456"), null, 200)));
    }
    @SuppressWarnings("unchecked") static Map<String,Object> map(Object v) { return (Map<String,Object>) v; }
    @SuppressWarnings("unchecked") static List<Map<String,Object>> list(Object v) { return (List<Map<String,Object>>) v; }
    static String token(Map<String,Object> v) { return (String) v.get("accessToken"); }
    ResponseEntity<Object> exchange(String method, String path, Object body, String token) {
        var headers = new HttpHeaders(); headers.setContentType(MediaType.APPLICATION_JSON);
        if (token != null) headers.setBearerAuth(token);
        return http.exchange("/api/v1"+path, HttpMethod.valueOf(method), new HttpEntity<>(body, headers), Object.class);
    }
    Object call(String method, String path, Object body, String token, int expected) {
        var r = exchange(method,path,body,token);
        assertThat(r.getStatusCode().value()).as(method+" "+path+" "+r.getBody()).isEqualTo(expected);
        return r.getBody();
    }
    Map<String,Object> buyer() {
        String email = UUID.randomUUID()+"@example.com";
        return map(call("POST","/auth/register/buyer",Map.of("name","Buyer","email",email,"password","Original123!"),null,201));
    }
    record Workshop(String id, String token) {}
    Workshop workshop() {
        String email=UUID.randomUUID()+"@example.com";
        var a=map(call("POST","/auth/register/artisan",Map.of("workshopName","Xuong gom","ownerName","Artisan","email",email,
                "password","Original123!","address","Bat Trang","lat",20.97,"lng",105.91),null,201));
        call("PATCH","/admin/artisans/"+a.get("id")+"/approve",null,admin,200);
        return new Workshop((String)a.get("id"),token(map(call("POST","/auth/login",Map.of("email",email,"password","Original123!"),null,200))));
    }
    String quote(String buyer, Workshop... workshops) {
        var q=map(call("POST","/quotes",Map.of("title","100 cups","description","Logo cups","quantity",100,
                "targetArtisanIds",Arrays.stream(workshops).map(Workshop::id).toList()),buyer,201));
        String id=(String)q.get("id");call("PATCH","/quotes/"+id+"/submit",null,buyer,200);return id;
    }
    String bid(String q, Workshop a, int price) {
        return (String)map(call("POST","/quotes/"+q+"/bids",Map.of("pricePerUnit",price,"estimatedDays",5),a.token(),201)).get("id");
    }
    @Test void migrationsAndRealJsonbRoundTrip() {
        assertThat(sql.queryForObject("SELECT COUNT(*) FROM flyway_schema_history WHERE success",Long.class)).isEqualTo(7);
        var a=workshop();
        call("PUT","/artisans/me",Map.of("familyHistory","Ba doi lam gom","certificates",List.of("OCOP"),"images",List.of("https://example.com/workshop.jpg")),a.token(),200);
        var profile=map(call("GET","/artisans/"+a.id(),null,null,200));
        assertThat(profile.get("certificates")).isEqualTo(List.of("OCOP"));
        call("GET","/products?limit=101",null,null,400);
        var users=map(call("GET","/admin/users",null,admin,200));
        for(var u:list(users.get("data"))) assertThat(u).doesNotContainKeys("passwordHash","securityVersion");
    }
    @Test void multiWorkshopAwardsPreserveTotalsAndRollUpDelivery() {
        var buyer=token(buyer());var a=workshop();var b=workshop();var stranger=token(buyer());
        String q=quote(buyer,a,b), one=bid(q,a,100), two=bid(q,b,200);
        var parts=List.of(Map.of("bidId",one,"quantity",60),Map.of("bidId",two,"quantity",40));
        call("POST","/quotes/"+q+"/accept-allocations",parts,stranger,403);
        var children=list(call("POST","/quotes/"+q+"/accept-allocations",parts,buyer,201));
        call("POST","/quotes/"+q+"/accept-allocations",parts,buyer,409);
        var progress=map(call("GET","/quotes/"+q+"/allocation-progress",null,buyer,200));
        assertThat(progress.get("overallStatus")).isEqualTo("ACCEPTED");
        assertThat(((Number)progress.get("acceptedValue")).longValue()).isEqualTo(14000);
        long confirmed=0;
        for(var child:children) {
            String id=(String)child.get("id");
            String workshop=((List<?>)child.get("targetArtisanIds")).contains(a.id())?a.token():b.token();
            call("PATCH","/quotes/"+id+"/start-production",null,workshop,200);
            call("PATCH","/quotes/"+id+"/complete",Map.of("deliveryNote","Done"),workshop,200);
            call("PATCH","/quotes/"+id+"/confirm-delivery",null,buyer,200);
            confirmed+=((Number)child.get("quantity")).longValue();
            var p=map(call("GET","/quotes/"+q+"/allocation-progress",null,buyer,200));
            assertThat(((Number)p.get("confirmedQuantity")).longValue()).isEqualTo(confirmed);
            if(confirmed<100)assertThat(p.get("overallStatus")).isEqualTo("IN_PRODUCTION");
        }
        assertThat(map(call("GET","/quotes/"+q+"/allocation-progress",null,buyer,200)).get("overallStatus")).isEqualTo("CLOSED");
        call("GET","/quotes/"+q+"/allocation-progress",null,a.token(),403);
    }
    @Test void concurrentAcceptanceHasExactlyOneWinner() throws Exception {
        String buyer=token(buyer());var a=workshop();var b=workshop();String q=quote(buyer,a,b);
        List<String> bids=List.of(bid(q,a,100),bid(q,b,200));
        try(var pool=Executors.newFixedThreadPool(2)) {
            var tasks=bids.stream().<Callable<Integer>>map(id -> () -> exchange("PATCH","/quotes/"+q+"/bids/"+id+"/accept",null,buyer).getStatusCode().value()).toList();
            List<Integer> statuses=new ArrayList<>();for(var result:pool.invokeAll(tasks))statuses.add(result.get());
            assertThat(statuses).containsExactlyInAnyOrder(200,409);
        }
        assertThat(sql.queryForObject("SELECT COUNT(*) FROM bids WHERE quote_request_id = ?::uuid AND status='ACCEPTED'",Long.class,q)).isEqualTo(1);
    }
    @Test void resetTokensAreHashedSingleUseAndRevokeExistingSessions() throws Exception {
        var auth=buyer();String old=token(auth), email=(String)map(auth.get("user")).get("email");
        var request=Map.of("email",email);
        Object reply=call("POST","/auth/forgot-password",request,null,202);
        assertThat(reply).isEqualTo(call("POST","/auth/forgot-password",Map.of("email","unknown@example.com"),null,202));
        String raw=mailed.get(email);assertThat(raw).hasSize(43);
        String stored=sql.queryForObject("SELECT token_hash FROM password_reset_tokens WHERE user_id = ?::uuid",String.class,map(auth.get("user")).get("id"));
        assertThat(stored).hasSize(64).isNotEqualTo(raw);
        call("POST","/auth/forgot-password",request,null,202);
        verify(delivery,times(1)).send(eq(email),anyString());
        var body=Map.of("token",raw,"newPassword","Replacement123!");
        try(var pool=Executors.newFixedThreadPool(2)) {
            Callable<Integer> task=() -> exchange("POST","/auth/reset-password",body,null).getStatusCode().value();
            var outcomes=pool.invokeAll(List.of(task,task));
            assertThat(List.of(outcomes.get(0).get(),outcomes.get(1).get())).containsExactlyInAnyOrder(200,400);
        }
        call("GET","/auth/me",null,old,401);
        call("POST","/auth/refresh-token",Map.of("refreshToken",auth.get("refreshToken")),null,401);
        call("POST","/auth/login",Map.of("email",email,"password","Original123!"),null,401);
        String fresh=token(map(call("POST","/auth/login",Map.of("email",email,"password","Replacement123!"),null,200)));
        call("GET","/auth/me",null,fresh,200);
        call("POST","/auth/reset-password",body,null,400);
    }
    @Test void disabledMailReturns503ForBothKnownAndUnknownAccounts() {
        var auth=buyer();when(delivery.available()).thenReturn(false);
        call("POST","/auth/forgot-password",Map.of("email",map(auth.get("user")).get("email")),null,503);
        call("POST","/auth/forgot-password",Map.of("email","unknown@example.com"),null,503);
    }
    @Test void partialCheckoutFailureRollsBackStockAndPreservesCart() {
        String buyer=token(buyer());var a=workshop();List<Map<String,Object>> products=new ArrayList<>();
        for(int i=0;i<2;i++) products.add(map(call("POST","/products",Map.of("name","Cup","price",100,"stock",10),a.token(),201)));
        products.sort(Comparator.comparing(p -> p.get("id").toString()));
        for(var p:products) call("POST","/cart/items",Map.of("productId",p.get("id"),"quantity",1),buyer,200);
        call("PUT","/products/"+products.get(1).get("id"),Map.of("stock",0),a.token(),200);
        call("POST","/orders",Map.of("shippingAddress","Bat Trang"),buyer,409);
        assertThat(map(call("GET","/products/"+products.get(0).get("id"),null,null,200)).get("stock")).isEqualTo(10);
        assertThat(list(map(call("GET","/cart",null,buyer,200)).get("items"))).hasSize(2);
    }

    @Test void invalidAwardPlanLeavesOriginalBidsUntouched() {
        String buyer=token(buyer());var a=workshop();var b=workshop();String q=quote(buyer,a,b);
        String first=bid(q,a,100),second=bid(q,b,200);
        call("POST","/quotes/"+q+"/accept-allocations",List.of(Map.of("bidId",first,"quantity",61),Map.of("bidId",second,"quantity",40)),buyer,400);
        assertThat(sql.queryForObject("SELECT COUNT(*) FROM quote_allocations WHERE parent_id = ?::uuid",Long.class,q)).isZero();
        assertThat(sql.queryForObject("SELECT COUNT(*) FROM bids WHERE quote_request_id = ?::uuid AND status='PENDING'",Long.class,q)).isEqualTo(2);
        assertThat(map(map(call("GET","/quotes/"+q,null,buyer,200)).get("quote")).get("status")).isEqualTo("BIDDING");
    }
    @Test void partialBidUpdateRetainsDaysAndMoneyCannotBeSilentlyRounded() {
        String buyer=token(buyer());var a=workshop();String q=quote(buyer,a),id=bid(q,a,100);
        var changed=map(call("PUT","/quotes/"+q+"/bids/"+id,Map.of("note","Updated note"),a.token(),200));
        assertThat(changed.get("estimatedDays")).isEqualTo(5);
        call("PUT","/quotes/"+q+"/bids/"+id,Map.of("pricePerUnit",1.001),a.token(),400);
        call("POST","/products",Map.of("name","Cup","price",1.001,"stock",10),a.token(),400);
    }
    @Test void changingPasswordImmediatelyRevokesOldAccessToken() {
        var auth=buyer();String old=token(auth),email=(String)map(auth.get("user")).get("email");
        call("PUT","/auth/change-password",Map.of("oldPassword","Original123!","newPassword","ắ".repeat(30)),old,400);
        call("PUT","/auth/change-password",Map.of("oldPassword","Original123!","newPassword","Replacement123!"),old,200);
        call("GET","/auth/me",null,old,401);
        String fresh=token(map(call("POST","/auth/login",Map.of("email",email,"password","Replacement123!"),null,200)));
        call("GET","/auth/me",null,fresh,200);
    }
    @Test void expiredResetTokenFailsAndDeliveryFailureRollsBackIssuedToken() {
        var auth=buyer();String email=(String)map(auth.get("user")).get("email"),id=(String)map(auth.get("user")).get("id");
        doThrow(new com.vcraft.domain.exception.ServiceUnavailableException("SMTP unavailable")).when(delivery).send(eq(email),anyString());
        call("POST","/auth/forgot-password",Map.of("email",email),null,503);
        assertThat(sql.queryForObject("SELECT COUNT(*) FROM password_reset_tokens WHERE user_id=?::uuid",Long.class,id)).isZero();
        doAnswer(i -> {mailed.put(email,i.getArgument(1));return null;}).when(delivery).send(eq(email),anyString());
        call("POST","/auth/forgot-password",Map.of("email",email),null,202);
        sql.update("UPDATE password_reset_tokens SET created_at=now()-interval '16 minutes', expires_at=now()-interval '1 minute' WHERE user_id=?::uuid",id);
        call("POST","/auth/reset-password",Map.of("token",mailed.get(email),"newPassword","Replacement123!"),null,400);
    }
}
