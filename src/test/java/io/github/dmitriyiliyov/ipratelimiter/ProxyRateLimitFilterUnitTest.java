package io.github.dmitriyiliyov.ipratelimiter;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProxyRateLimitFilterUnitTest {

    @Mock
    ObjectMapper mapper;

    @Mock
    RateLimitRepository repository;

    @Mock
    HttpServletRequest request;

    ProxyRateLimitFilter tested;

    @BeforeEach
    void setUp() {
        lenient().when(repository.getTargetUrl()).thenReturn("/api/ip");
        tested = new ProxyRateLimitFilter(mapper, List.of(repository));
    }

    @Test
    @DisplayName("UT extractIp() when X-Forwarded-For is null, should return remoteAddr")
    void extractIp_whenXffHeaderIsNull_shouldReturnRemoteAddr() {
        // given
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("192.168.0.1");

        // when
        String ip = tested.extractIp(request);

        // then
        assertThat(ip).isEqualTo("192.168.0.1");
    }

    @Test
    @DisplayName("UT extractIp() when X-Forwarded-For is blank, should return remoteAddr")
    void extractIp_whenXffHeaderIsBlank_shouldReturnRemoteAddr() {
        // given
        when(request.getHeader("X-Forwarded-For")).thenReturn("   ");
        when(request.getRemoteAddr()).thenReturn("192.168.0.1");

        // when
        String ip = tested.extractIp(request);

        // then
        assertThat(ip).isEqualTo("192.168.0.1");
    }

    @Test
    @DisplayName("UT extractIp() when X-Forwarded-For has single IP, should return that IP")
    void extractIp_whenXffHeaderHasSingleIp_shouldReturnThatIp() {
        // given
        when(request.getHeader("X-Forwarded-For")).thenReturn("10.0.0.1");

        // when
        String ip = tested.extractIp(request);

        // then
        assertThat(ip).isEqualTo("10.0.0.1");
    }

    @Test
    @DisplayName("UT extractIp() when X-Forwarded-For has multiple IPs, should return first IP")
    void extractIp_whenXffHeaderHasMultipleIps_shouldReturnFirstIp() {
        // given
        when(request.getHeader("X-Forwarded-For")).thenReturn("10.0.0.1, 172.16.0.5, 203.0.113.9");

        // when
        String ip = tested.extractIp(request);

        // then
        assertThat(ip).isEqualTo("10.0.0.1");
    }

    @Test
    @DisplayName("UT extractIp() when X-Forwarded-For first IP has surrounding whitespace, should return trimmed IP")
    void extractIp_whenXffFirstIpHasSurroundingWhitespace_shouldReturnTrimmedIp() {
        // given
        when(request.getHeader("X-Forwarded-For")).thenReturn("  10.0.0.1  , 172.16.0.5");

        // when
        String ip = tested.extractIp(request);

        // then
        assertThat(ip).isEqualTo("10.0.0.1");
    }
}