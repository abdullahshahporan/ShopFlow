package com.shahporan.demo.service;

import com.shahporan.demo.dto.UserResponseDto;
import com.shahporan.demo.entity.Admin;
import com.shahporan.demo.entity.Seller;
import com.shahporan.demo.entity.User;
import com.shahporan.demo.exception.ResourceNotFoundException;
import com.shahporan.demo.repository.AdminRepository;
import com.shahporan.demo.repository.SellerRepository;
import com.shahporan.demo.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock
    private AdminRepository adminRepository;

    @Mock
    private SellerRepository sellerRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AdminService adminService;

    private Seller enabledSeller;
    private Seller disabledSeller;

    @BeforeEach
    void setUp() {
        enabledSeller = Seller.builder()
                .id(1L).name("Active Seller").email("active@test.com")
                .passwordHash("hash").enabled(true).build();

        disabledSeller = Seller.builder()
                .id(2L).name("Pending Seller").email("pending@test.com")
                .passwordHash("hash").enabled(false).build();
    }

    // -----------------------------------------------------------------------
    // toggleUser
    // -----------------------------------------------------------------------

    @Test
    void toggleUser_whenSellerIsEnabled_thenDisablesSeller() {
        when(sellerRepository.findById(1L)).thenReturn(Optional.of(enabledSeller));
        when(sellerRepository.save(enabledSeller)).thenReturn(enabledSeller);

        UserResponseDto result = adminService.toggleUser(1L, 99L);

        assertThat(result.getEnabled()).isFalse();
    }

    @Test
    void toggleUser_whenSellerIsDisabled_thenEnablesSeller() {
        when(sellerRepository.findById(2L)).thenReturn(Optional.of(disabledSeller));
        when(sellerRepository.save(disabledSeller)).thenReturn(disabledSeller);

        UserResponseDto result = adminService.toggleUser(2L, 99L);

        assertThat(result.getEnabled()).isTrue();
    }

    @Test
    void toggleUser_whenSellerNotFound_thenThrowsResourceNotFound() {
        when(sellerRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> adminService.toggleUser(99L, 1L));
    }

    // -----------------------------------------------------------------------
    // countPendingSellers / counts
    // -----------------------------------------------------------------------

    @Test
    void countPendingSellers_thenReturnsRepositoryValue() {
        when(sellerRepository.countByEnabled(false)).thenReturn(3L);

        long count = adminService.countPendingSellers();

        assertThat(count).isEqualTo(3L);
    }

    @Test
    void countBuyers_thenReturnsRepositoryValue() {
        when(userRepository.count()).thenReturn(7L);

        assertThat(adminService.countBuyers()).isEqualTo(7L);
    }

    @Test
    void countSellers_thenReturnsRepositoryValue() {
        when(sellerRepository.count()).thenReturn(4L);

        assertThat(adminService.countSellers()).isEqualTo(4L);
    }

    // -----------------------------------------------------------------------
    // getAllSellers
    // -----------------------------------------------------------------------

    @Test
    void getAllSellers_thenReturnsMappedList() {
        when(sellerRepository.findAllByOrderByCreatedAtDesc())
                .thenReturn(List.of(enabledSeller, disabledSeller));

        List<UserResponseDto> result = adminService.getAllSellers();

        assertThat(result).hasSize(2);
        assertThat(result).allMatch(dto -> "ROLE_SELLER".equals(dto.getRole()));
    }
}
