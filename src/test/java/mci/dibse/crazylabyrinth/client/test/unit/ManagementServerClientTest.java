package mci.dibse.crazylabyrinth.client.test.unit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import mci.dibse.crazylabyrinth.client.infrastructure.exceptions.GameManagementServerException;
import mci.dibse.crazylabyrinth.client.infrastructure.management.ManagementServerClient;
import mci.dibse.crazylabyrinth.client.test.data.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/**
 *  @author Julian Janetschek
 */
@ExtendWith(MockitoExtension.class)
class ManagementServerClientTest {

    private static final String MANAGEMENT_SERVER_URL = "http://mgmt.example.com";
    private static final String MANAGEMENT_SERVER_PORT = "8080";
    private static final long MANAGEMENT_SERVER_REQUEST_TIMEOUT_MILLIS = 5000;
    private static final String PATH_GET_SERVERS = "/servers";

    private ManagementServerClient client;
    private @Mock HttpClient httpClientMock;
    private @Mock HttpResponse<String> httpResponseMock;

    @BeforeEach
    void setUp() {
        client =
                spy(
                        new ManagementServerClient(
                                MANAGEMENT_SERVER_URL,
                                MANAGEMENT_SERVER_PORT,
                                MANAGEMENT_SERVER_REQUEST_TIMEOUT_MILLIS));
        ReflectionTestUtils.setField(client, "managementServerUrl", MANAGEMENT_SERVER_URL);

        ReflectionTestUtils.setField(client, "httpClient", httpClientMock);
    }

    @Test
    void getGameServers_successReturnsBody() throws Exception {
        when(httpClientMock.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponseMock);
        when(httpResponseMock.statusCode()).thenReturn(200);
        when(httpResponseMock.body()).thenReturn(TestDataFactory.valid_getServers_ResponseBody());

        String result = client.getGameServers();

        assertEquals(TestDataFactory.valid_getServers_ResponseBody(), result);

        ArgumentCaptor<HttpRequest> reqCaptor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(httpClientMock).send(reqCaptor.capture(), any(HttpResponse.BodyHandler.class));
        HttpRequest sent = reqCaptor.getValue();
        assertEquals(createUri(), sent.uri());
        assertEquals("GET", sent.method());
        assertEquals(
                Duration.ofMillis(MANAGEMENT_SERVER_REQUEST_TIMEOUT_MILLIS),
                sent.timeout().orElseThrow());
    }

    @Test
    void getGameServers_non200Throws() throws Exception {
        when(httpClientMock.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponseMock);
        when(httpResponseMock.statusCode()).thenReturn(503);

        GameManagementServerException ex =
                assertThrows(GameManagementServerException.class, client::getGameServers);
        assertTrue(ex.getCause().toString().contains("503"));
    }

    @Test
    void getGameServers_ioExceptionWrapped() throws Exception {
        when(httpClientMock.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenThrow(new IOException());

        GameManagementServerException ex =
                assertThrows(GameManagementServerException.class, client::getGameServers);
        assertTrue(ex.getMessage().contains("I/O error"));
        assertTrue(ex.getCause() instanceof IOException);
    }

    @Test
    void getGameServers_interruptedExceptionWrappedAndThreadInterrupted() throws Exception {
        when(httpClientMock.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenAnswer(
                        inv -> {
                            throw new InterruptedException("stop");
                        });

        GameManagementServerException ex =
                assertThrows(GameManagementServerException.class, client::getGameServers);
        assertTrue(ex.getMessage().contains("Request interrupted"));
        assertTrue(ex.getCause() instanceof InterruptedException);
        assertTrue(Thread.currentThread().isInterrupted(), "Thread should be interrupted");
        Thread.interrupted();
    }

    private URI createUri() {
        return URI.create(MANAGEMENT_SERVER_URL + ":" + MANAGEMENT_SERVER_PORT + PATH_GET_SERVERS);
    }
}
