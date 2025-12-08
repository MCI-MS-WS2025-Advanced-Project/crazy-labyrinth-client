package mci.dibse.crazylabyrinth.client.infrastructure.management;

import java.io.IOException;
import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import mci.dibse.crazylabyrinth.client.infrastructure.exceptions.GameManagementServerException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpServerErrorException;

/**
 * Client component for communicating with the external game management server.
 * <p>
 * It performs HTTP requests to retrieve information about available game servers.
 * </p>
 *
 * <h2>Configuration properties</h2>
 * <ul>
 *   <li>management.server.url — base URL of the management server</li>
 *   <li>management.server.port — TCP port of the management server </li>
 *   <li>management.server.request.timeout.milliseconds — request timeout in milliseconds (default: 5000)</li>
 * </ul>
 *
 * <h2>Threading</h2>
 * The client uses Java's synchronous HttpClient. Calls block the invoking thread.
 * @author Julian Janetschek
 */
@Slf4j
@Component
public class ManagementServerClient {

    private static final int HTTP_OK = 200;
    private static final String PATH_GET_SERVERS = "/servers";
    private final String managementServerUrl;
    private final String port;
    private final HttpClient httpClient;
    private final Duration requestTimeout;

    public ManagementServerClient(
            @Value("${management.server.url:}") String managementServerUrl,
            @Value("${management.server.port:}") String port,
            @Value("${management.server.request.timeout.milliseconds:5000}") long timeoutMillis) {
        this.managementServerUrl = managementServerUrl;
        this.requestTimeout = Duration.ofMillis(timeoutMillis);
        this.port = port;
        this.httpClient =
                HttpClient.newBuilder()
                        .connectTimeout(requestTimeout)
                        .followRedirects(HttpClient.Redirect.NORMAL)
                        .build();
    }

    // TODO impl ErrorResponse Object?
    public String getGameServers() throws GameManagementServerException {
        try {
            HttpResponse<String> response = sendGetServerRequest();
            return response.body();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Request interrupted", e);
            throw new GameManagementServerException("Request interrupted", e);
        } catch (ConnectException e) {
            log.error("Failed to connect to management server", e);
            throw new GameManagementServerException("Failed to connect to management server", e);
        } catch (SocketTimeoutException e) {
            log.error("Request timed out", e);
            throw new GameManagementServerException("Request timed out", e);
        } catch (NoRouteToHostException e) {
            log.error("No route to host", e);
            throw new GameManagementServerException("No route to host", e);
        } catch (UnknownHostException e) {
            log.error("Unknown host", e);
            throw new GameManagementServerException("Unknown host", e);
        } catch (HttpServerErrorException e) {
            log.error("HttpServerError error", e);
            throw new GameManagementServerException("HttpServerError: " + e.getStatusCode(), e);
        } catch (IOException e) {
            log.error("I/O error during request", e);
            throw new GameManagementServerException("I/O error during request", e);
        }
    }

    private HttpResponse<String> sendGetServerRequest()
            throws IOException, InterruptedException, HttpServerErrorException {
        HttpRequest request =
                HttpRequest.newBuilder()
                        .uri(buildServerUri())
                        .timeout(requestTimeout)
                        .GET()
                        .build();
        HttpResponse<String> response =
                httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != HTTP_OK) {
            System.out.println("response.statusCode: " + response.statusCode());
            throw new HttpServerErrorException(HttpStatus.valueOf(response.statusCode()));
        }
        return response;
    }

    private URI buildServerUri() {
        String base = managementServerUrl == null ? "" : managementServerUrl.trim();
        String p = (port == null || port.isBlank()) ? "" : ":" + port.trim();
        return URI.create(base + p + PATH_GET_SERVERS);
    }
}
