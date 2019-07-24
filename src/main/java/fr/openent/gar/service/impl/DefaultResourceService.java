package fr.openent.gar.service.impl;

import fr.openent.gar.Gar;
import fr.openent.gar.service.ResourceService;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.buffer.impl.BufferImpl;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.net.PemKeyCertOptions;
import io.vertx.core.net.ProxyOptions;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.neo4j.Neo4jResult;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Date;
import java.util.zip.GZIPInputStream;

public class DefaultResourceService implements ResourceService {

    private final Vertx vertx;
    private final String garHost;
    private final String idEnt;
    private final String certPath;
    private final String keyPath;
    private Logger log = LoggerFactory.getLogger(DefaultResourceService.class);
    private HttpClient httpClient;

    public DefaultResourceService(Vertx vertx, String garHost, String idEnt, String certPath, String keyPath) {
        this.vertx = vertx;
        this.garHost = garHost;
        this.idEnt = idEnt;
        this.certPath = certPath;
        this.keyPath = keyPath;

        try {
            this.httpClient = generateHttpClient(new URI(garHost));
        } catch (URISyntaxException e) {
            log.error("[DefaultResourceService@constructor] An error occurred when creating the URI", e);
        }
    }

    @Override
    public void get(String userId, String structure, String hostname, Handler<Either<String, JsonArray>> handler) {
        String uaiQuery = "MATCH (s:Structure {id: {structureId}}) return s.UAI as UAI";
        JsonObject params = new JsonObject().put("structureId", structure);

        Neo4j.getInstance().execute(uaiQuery, params, Neo4jResult.validResultHandler(event -> {
            if (event.isRight()) {
                JsonArray results = event.right().getValue();
                if (results.size() > 0) {
                    String uai = results.getJsonObject(0).getString("UAI");
                    String garHostNoProtocol = "";
                    int port = 443;
                    String host = garHost;
                    try {
                        URL url = new URL(garHost);
                        garHostNoProtocol = url.getHost();
                        port = url.getPort() != -1 ? url.getPort() : port;
                        host = url.getHost();
                    } catch (Exception e) {
                        handler.handle(new Either.Left<>("[DefaultResourceService@get] Bad gar host url : " + garHost));
                    }
                    String resourcesUri = Gar.demo
                            ? "/gar/public/ts/model/__mocks__/resources.json"
                            : "/ressources/" + idEnt + "/" + uai + "/" + userId;
                    final HttpClientRequest client = httpClient.get(port, host, resourcesUri, response -> {
                        if (response.statusCode() != 200) {
                            log.error("try to call " + resourcesUri);
                            log.error(response.statusCode() + " " + response.statusMessage());

                            response.bodyHandler(errBuff -> {
                                JsonObject error = new JsonObject(new String(errBuff.getBytes()));
                                if (error.containsKey("Erreur")) {
                                    handler.handle(new Either.Left<>(error.getJsonObject("Erreur").getString("Message")));
                                } else {
                                    handler.handle(new Either.Left<>("[DefaultResourceService@get] failed to connect to GAR servers: " + response.statusMessage()));
                                }
                            });
                        } else {
                            Buffer responseBuffer = new BufferImpl();
                            response.handler(responseBuffer::appendBuffer);
                            response.endHandler(aVoid -> {
                                JsonObject resources = new JsonObject(Gar.demo ? new String(responseBuffer.getBytes()) : decompress(responseBuffer));
                                handler.handle(new Either.Right<>(resources.getJsonObject("listeRessources").getJsonArray("ressource")));
                            });
                            response.exceptionHandler(throwable -> handler.handle(new Either.Left<>("[DefaultResourceService@get] failed to get GAR response: " + throwable.getMessage())));
                        }
                    }).putHeader("Accept", "application/json")
                            .putHeader("Accept-Encoding", "gzip, deflate")
                            .putHeader("Host", garHostNoProtocol)
                            .putHeader("Cache-Control", "no-cache")
                            .putHeader("Date", new Date().toString());

                    client.end();
                } else {
                    handler.handle(new Either.Right<>(new JsonArray()));
                }
            } else {
                String message = "[DefaultResourceService@get] An error occurred when fetching structure UAI for structure " + structure;
                log.error(message);
                handler.handle(new Either.Left<>(message));
            }
        }));
    }

    private String decompress(Buffer buffer) {
        StringBuilder output = new StringBuilder();
        try {
            GZIPInputStream gzipInputStream = new GZIPInputStream(new ByteArrayInputStream(buffer.getBytes()));
            BufferedReader bf = new BufferedReader(new InputStreamReader(gzipInputStream, "UTF-8"));
            String line;
            while ((line = bf.readLine()) != null) {
                output.append(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return output.toString();
    }

    private HttpClient generateHttpClient(URI uri) {
        HttpClientOptions options = new HttpClientOptions()
                .setDefaultHost(uri.getHost())
                .setDefaultPort("https".equals(uri.getScheme()) ? 443 : 80)
                .setVerifyHost(false)
                .setTrustAll(true)
                .setSsl("https".equals(uri.getScheme()))
                .setKeepAlive(true)
                .setPemKeyCertOptions(getPemKeyCertOptions(certPath, keyPath));
        return vertx.createHttpClient(options);
    }

    private PemKeyCertOptions getPemKeyCertOptions(String certPath, String keyPath) {
        return new PemKeyCertOptions()
                .setCertPath(certPath)
                .setKeyPath(keyPath);
    }
}
