package org.codeNbug.mainserver.domain.user.business;

import org.codeNbug.mainserver.domain.purchase.dto.PurchaseHistoryDetailResponse;
import org.codeNbug.mainserver.domain.purchase.dto.PurchaseHistoryListResponse;
import org.codeNbug.mainserver.domain.purchase.entity.PaymentMethodEnum;
import org.codeNbug.mainserver.domain.purchase.entity.PaymentStatusEnum;
import org.codeNbug.mainserver.domain.purchase.entity.Purchase;
import org.codeNbug.mainserver.domain.purchase.repository.PurchaseRepository;
import org.codeNbug.mainserver.domain.purchase.service.PurchaseService;
import org.codeNbug.mainserver.domain.ticket.entity.Ticket;
import org.codenbug.user.domain.user.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PurchaseServiceTest {

    @Mock
    private PurchaseRepository purchaseRepository;

    @InjectMocks
    private PurchaseService purchaseService;

    @Nested
    @DisplayName("구매 이력 조회 테스트")
    class PurchaseHistoryTest {
        @Test
        @DisplayName("사용자의 구매 이력 조회시 성공")
        void 구매_이력_조회_성공() {
            // given
            Long userId = 1L;
            List<PaymentStatusEnum> statuses = Arrays.asList(PaymentStatusEnum.DONE, PaymentStatusEnum.EXPIRED);
            List<Purchase> purchases = createTestPurchases();

            when(purchaseRepository.findByUserUserIdAndPaymentStatusInOrderByPurchaseDateDesc(userId, statuses))
                .thenReturn(purchases);

            // when
            PurchaseHistoryListResponse response = purchaseService.getPurchaseHistoryList(userId);

            // then
            assertNotNull(response);
            assertEquals(purchases.size(), response.getPurchases().size());
        }

        @Test
        @DisplayName("구매 이력이 없는 사용자 조회시 빈 리스트 반환")
        void 구매_이력_없음() {
            // given
            Long userId = 1L;
            List<PaymentStatusEnum> statuses = Arrays.asList(PaymentStatusEnum.DONE, PaymentStatusEnum.EXPIRED);

            when(purchaseRepository.findByUserUserIdAndPaymentStatusInOrderByPurchaseDateDesc(userId, statuses))
                .thenReturn(List.of());

            // when
            PurchaseHistoryListResponse response = purchaseService.getPurchaseHistoryList(userId);

            // then
            assertNotNull(response);
            assertTrue(response.getPurchases().isEmpty());
        }
    }

    @Nested
    @DisplayName("구매 상세 조회 테스트")
    class PurchaseDetailTest {
        @Test
        @DisplayName("구매 상세 조회 성공")
        void 구매_상세_조회_성공() {
            // given
            Long userId = 1L;
            Long purchaseId = 1L;
            Purchase purchase = Purchase.builder()
                    .id(purchaseId)
                    .user(createTestUser())
                    .paymentStatus(PaymentStatusEnum.DONE)
                    .paymentMethod(PaymentMethodEnum.카드)
                    .amount(10000)
                    .orderName("테스트 주문")
                    .orderId("ORDER_123")
                    .purchaseDate(LocalDateTime.now())
                    .tickets(new ArrayList<>())
                    .build();

            when(purchaseRepository.findById(purchaseId)).thenReturn(Optional.of(purchase));

            // when
            PurchaseHistoryDetailResponse response = purchaseService.getPurchaseHistoryDetail(userId, purchaseId);

            // then
            assertNotNull(response);
            assertNotNull(response.getPurchases());
            assertFalse(response.getPurchases().isEmpty());
            assertTrue(response.getPurchases().get(0).getTickets().isEmpty());
        }

        @Test
        @DisplayName("존재하지 않는 구매 내역 조회시 실패")
        void 구매_상세_조회_실패() {
            // given
            Long userId = 1L;
            Long purchaseId = 999L;

            when(purchaseRepository.findById(purchaseId)).thenReturn(Optional.empty());

            // when & then
            assertThrows(RuntimeException.class, 
                () -> purchaseService.getPurchaseHistoryDetail(userId, purchaseId));
        }

        @Test
        @DisplayName("다른 사용자의 구매 내역 조회시 실패")
        void 구매_상세_조회_권한_없음() {
            // given
            Long userId = 1L;
            Long purchaseId = 1L;
            Purchase otherUserPurchase = createTestPurchase();
            User otherUser = User.builder()
                    .userId(2L)
                    .email("other@example.com")
                    .build();
            otherUserPurchase = Purchase.builder()
                    .id(purchaseId)
                    .user(otherUser)
                    .paymentStatus(PaymentStatusEnum.DONE)
                    .purchaseDate(LocalDateTime.now())
                    .build();

            when(purchaseRepository.findById(purchaseId)).thenReturn(Optional.of(otherUserPurchase));

            // when & then
            assertThrows(RuntimeException.class, 
                () -> purchaseService.getPurchaseHistoryDetail(userId, purchaseId));
        }
    }

    // Helper methods
    private User createTestUser() {
        return User.builder()
                .userId(1L)
                .email("test@example.com")
                .password("encodedPassword")
                .name("테스트")
                .age(25)
                .sex("남성")
                .phoneNum("010-1234-5678")
                .location("서울시 강남구")
                .role("ROLE_USER")
                .build();
    }

    private List<Purchase> createTestPurchases() {
        return Arrays.asList(createTestPurchase());
    }

    private Purchase createTestPurchase() {
        return Purchase.builder()
                .id(1L)
                .user(createTestUser())
                .paymentStatus(PaymentStatusEnum.DONE)
                .paymentMethod(PaymentMethodEnum.카드)
                .amount(10000)
                .orderName("테스트 주문")
                .orderId("ORDER_123")
                .purchaseDate(LocalDateTime.now())
                .tickets(new ArrayList<>())
                .build();
    }
} 