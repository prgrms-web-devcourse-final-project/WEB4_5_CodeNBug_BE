package org.codeNbug.mainserver.domain.manager.controller;

import static org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.codeNbug.mainserver.domain.event.entity.EventCategoryEnum;
import org.codeNbug.mainserver.domain.manager.dto.EventRegisterRequest;
import org.codeNbug.mainserver.domain.manager.dto.layout.LayoutDto;
import org.codeNbug.mainserver.domain.manager.dto.layout.PriceDto;
import org.codeNbug.mainserver.domain.manager.dto.layout.SeatInfoDto;
import org.codeNbug.mainserver.util.TestUtil;
import org.codenbug.user.domain.user.constant.UserRole;
import org.codenbug.user.domain.user.entity.User;
import org.codenbug.user.domain.user.repository.UserRepository;
import org.codenbug.user.security.service.CustomUserDetails;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = NONE)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Testcontainers
class ManagerControllerTest {

    @Container
    @ServiceConnection
    static MySQLContainer<?> mysql =
        new MySQLContainer<>("mysql:8.0.34")
            .withDatabaseName("ticketoneTest")
            .withUsername("test")
            .withPassword("test");
    @Container
    @ServiceConnection
    static GenericContainer<?> redis =
        new GenericContainer<>("redis:alpine")
            .withExposedPorts(6379)
            .waitingFor(Wait.forListeningPort());
    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 2) 스프링 프로퍼티에 컨테이너 URL/계정 주입
    // @DynamicPropertySource
    // static void overrideProps(DynamicPropertyRegistry registry) {
    //
    //     registry.add("spring.datasource.url", mysql::getJdbcUrl);
    //     registry.add("spring.datasource.username", mysql::getUsername);
    //     registry.add("spring.datasource.password", mysql::getPassword);
    //     registry.add("spring.redis.host", () -> redis.getHost());
    //     registry.add("spring.redis.port", () -> redis.getMappedPort(6379));
    //     registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    //
    // }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    private Long managerId;

    @AfterAll
    void tearDown() {
        TestUtil.truncateAllTables(jdbcTemplate);
    }

    @BeforeEach
    void setupTestManager() {
        User manager = User.builder()
                .email("manager@test.com")
                .password("encrypted")
                .name("테스트 매니저")
                .sex("M")
                .age(30)
                .phoneNum("010-1234-5678")
                .role(UserRole.MANAGER.getAuthority()) // "ROLE_MANAGER"
                .build();

        userRepository.save(manager);

        // 👇 CustomUserDetails 생성
        CustomUserDetails userDetails = new CustomUserDetails(manager);

        // 👇 인증 토큰 생성 및 SecurityContext에 등록
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities()
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }



    @Test
    @WithMockUser(username = "manager@test.com", roles = "MANAGER")
    void 이벤트_등록_성공() throws Exception {
        LayoutDto layoutDto = LayoutDto.builder()
                .layout(List.of(List.of("A1", "A2"), List.of("B1", "B2")))
                .seat(Map.of(
                        "A1", new SeatInfoDto("S"),
                        "A2", new SeatInfoDto("S"),
                        "B1", new SeatInfoDto("A"),
                        "B2", new SeatInfoDto("A")
                ))
                .build();

        EventRegisterRequest request = EventRegisterRequest.builder()
                .title("테스트 이벤트")
                .category(EventCategoryEnum.CONCERT)
                .description("설명")
                .restriction("없음")
                .thumbnailUrl("https://example.com/image.jpg")
                .startDate(LocalDateTime.now().plusDays(5))
                .endDate(LocalDateTime.now().plusDays(7))
                .location("서울시 강남구")
                .hallName("1관")
                .seatCount(4)
                .layout(layoutDto)
                .price(List.of(
                        new PriceDto("S", 100000),
                        new PriceDto("A", 80000)
                ))
                .bookingStart(LocalDateTime.now().plusDays(1))
                .bookingEnd(LocalDateTime.now().plusDays(4))
                .agelimit(12)
                .build();

        mockMvc.perform(post("/api/v1/manager/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.msg").value("이벤트 등록 성공"))
                .andExpect(jsonPath("$.data.eventId").exists());

    }


    @Test
    @DisplayName("이벤트 수정 성공")
    void 이벤트_수정_성공() throws Exception {
        // Step 1: 이벤트 등록
        LayoutDto layoutDto = LayoutDto.builder()
                .layout(List.of(List.of("A1", "A2"), List.of("B1", "B2")))
                .seat(Map.of(
                        "A1", new SeatInfoDto("S"),
                        "A2", new SeatInfoDto("S"),
                        "B1", new SeatInfoDto("A"),
                        "B2", new SeatInfoDto("A")
                ))
                .build();

        EventRegisterRequest original = EventRegisterRequest.builder()
                .title("테스트 이벤트")
                .category(EventCategoryEnum.CONCERT)
                .description("설명")
                .restriction("없음")
                .thumbnailUrl("https://example.com/image.jpg")
                .startDate(LocalDateTime.now().plusDays(1))
                .endDate(LocalDateTime.now().plusDays(3))
                .location("서울시 강남구")
                .hallName("1관")
                .seatCount(4)
                .layout(layoutDto)
                .price(List.of(
                        new PriceDto("S", 100000),
                        new PriceDto("A", 80000)
                ))
                .bookingStart(LocalDateTime.now())
                .bookingEnd(LocalDateTime.now().plusDays(1))
                .agelimit(12)
                .build();

        MvcResult result = mockMvc.perform(post("/api/v1/manager/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(original)))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        JsonNode jsonNode = objectMapper.readTree(responseBody);
        Long eventId = jsonNode.path("data").path("eventId").asLong();

        // Step 2: 수정 요청
        EventRegisterRequest updated = EventRegisterRequest.builder()
                .title("수정된 이벤트")
                .category(EventCategoryEnum.MUSICAL)
                .description("수정된 설명")
                .restriction("연령제한")
                .thumbnailUrl("https://example.com/updated.jpg")
                .startDate(LocalDateTime.now().plusDays(2))
                .endDate(LocalDateTime.now().plusDays(4))
                .location("부산시 해운대구")
                .hallName("2관")
                .seatCount(4)
                .layout(layoutDto)
                .price(List.of(
                        new PriceDto("S", 100000),
                        new PriceDto("A", 80000)
                ))
                .bookingStart(LocalDateTime.now().plusDays(1))
                .bookingEnd(LocalDateTime.now().plusDays(2))
                .agelimit(15)
                .build();

        // Step 3: 수정 요청 실행 및 검증
        mockMvc.perform(put("/api/v1/manager/events/{eventId}", eventId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updated)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.msg").value("이벤트 수정 성공"))
                .andExpect(jsonPath("$.data.eventId").value(eventId));
    }

    @Test
    @DisplayName("이벤트 삭제 성공")
    void 이벤트_삭제_성공() throws Exception {
        // Step 1: 이벤트 등록
        LayoutDto layoutDto = LayoutDto.builder()
                .layout(List.of(List.of("A1", "A2"), List.of("B1", "B2")))
                .seat(Map.of(
                        "A1", new SeatInfoDto("S"),
                        "A2", new SeatInfoDto("S"),
                        "B1", new SeatInfoDto("A"),
                        "B2", new SeatInfoDto("A")
                ))
                .build();

        EventRegisterRequest request = EventRegisterRequest.builder()
                .title("삭제할 이벤트")
                .category(EventCategoryEnum.CONCERT)
                .description("설명")
                .restriction("없음")
                .thumbnailUrl("https://example.com/image.jpg")
                .startDate(LocalDateTime.now().plusDays(1))
                .endDate(LocalDateTime.now().plusDays(3))
                .location("서울시 강남구")
                .hallName("1관")
                .seatCount(4)
                .layout(layoutDto)
                .price(List.of(
                        new PriceDto("S", 100000),
                        new PriceDto("A", 80000)
                ))
                .bookingStart(LocalDateTime.now())
                .bookingEnd(LocalDateTime.now().plusDays(1))
                .agelimit(12)
                .build();

        MvcResult result = mockMvc.perform(post("/api/v1/manager/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        JsonNode jsonNode = objectMapper.readTree(responseBody);
        Long eventId = jsonNode.path("data").path("eventId").asLong();

        // Step 2: 삭제 요청
        mockMvc.perform(patch("/api/v1/manager/events/{eventId}", eventId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.msg").value("이벤트 삭제 성공"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    @DisplayName("매니저 이벤트 목록 조회 성공")
    void 매니저_이벤트_목록_조회() throws Exception {
        // Step 1: 이벤트 하나 등록
        LayoutDto layoutDto = LayoutDto.builder()
                .layout(List.of(List.of("A1", "A2"), List.of("B1", "B2")))
                .seat(Map.of(
                        "A1", new SeatInfoDto("S"),
                        "A2", new SeatInfoDto("S"),
                        "B1", new SeatInfoDto("A"),
                        "B2", new SeatInfoDto("A")
                ))
                .build();

        EventRegisterRequest request = EventRegisterRequest.builder()
                .title("목록 조회용 이벤트")
                .category(EventCategoryEnum.CONCERT)
                .description("설명")
                .restriction("없음")
                .thumbnailUrl("https://example.com/image.jpg")
                .startDate(LocalDateTime.now().plusDays(1))
                .endDate(LocalDateTime.now().plusDays(3))
                .location("서울시 강남구")
                .hallName("1관")
                .seatCount(4)
                .layout(layoutDto)
                .price(List.of(
                        new PriceDto("S", 100000),
                        new PriceDto("A", 80000)
                ))
                .bookingStart(LocalDateTime.now())
                .bookingEnd(LocalDateTime.now().plusDays(1))
                .agelimit(12)
                .build();

        mockMvc.perform(post("/api/v1/manager/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // Step 2: 목록 조회 요청
        mockMvc.perform(get("/api/v1/manager/events/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.msg").value("매니저 이벤트 목록 조회 성공"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].title").value("목록 조회용 이벤트"));
    }


}
