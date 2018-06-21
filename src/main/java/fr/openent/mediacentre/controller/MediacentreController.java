package fr.openent.mediacentre.controller;

import fr.openent.mediacentre.Mediacentre;
import fr.openent.mediacentre.export.ExportService;
import fr.openent.mediacentre.export.impl.ExportServiceImpl;
import fr.wseduc.bus.BusAddress;
import fr.wseduc.rs.Get;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.security.SecuredAction;
import org.entcore.common.controller.ControllerHelper;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.eventbus.impl.JsonObjectMessage;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.RouteMatcher;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Container;
import org.vertx.java.core.logging.Logger;

import java.util.Map;

import static fr.wseduc.webutils.http.response.DefaultResponseHandler.defaultResponseHandler;

public class MediacentreController extends ControllerHelper {

    private ExportService exportService;
    private Logger log = null;
    private EventBus eb = null;

    public MediacentreController() {
        super();
    }

    @Override
    public void init(Vertx vertx, Container container, RouteMatcher rm, Map<String, SecuredAction> securedActions) {
        super.init(vertx, container, rm, securedActions);
        this.exportService = new ExportServiceImpl(container);
        this.log = container.logger();
        this.eb = vertx.eventBus();
    }

    @Get("testexport")
    public void testExport(HttpServerRequest request) {
        exportService.launchExport(defaultResponseHandler(request));
        //defaultResponseHandler(request).handle(new Either.Left<String, JsonObject>("Toto"));
    }

    @Get("testsftp")
    public void testsftp(final HttpServerRequest request) {
        JsonObject test = new JsonObject().putString("action","send")
                .putString("known-hosts","C:\\Users\\colenot\\.ssh\\known_hosts")
                .putString("hostname","213.32.49.235")
                .putNumber("port",24242)
                .putString("username","sftpODE")
                .putString("sshkey","C:\\CGI\\DATA\\ARANGER\\DATA\\Support\\Alimentation\\priv_key.ppk")
                .putString("local-file","C:\\Users\\colenot\\.ssh\\known_hosts")
                .putString("dist-file","/testtcol.txt");
        eb.send("sftp", test, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {
                defaultResponseHandler(request).handle(new Either.Left<String, JsonObject>("Toto"));
            }
        });
        //exportService.launchExport(defaultResponseHandler(request));
        //defaultResponseHandler(request).handle(new Either.Left<String, JsonObject>("Toto"));
    }

    @BusAddress(Mediacentre.MEDIACENTRE_ADDRESS)
    public void addressHandler(Message<JsonObject> message) {
        String action = message.body().getString("action", "");
        switch (action) {
            case "export" : exportService.launchExport(message);
                break;
            default:
                log.error("Mediacentre invalid.action " + action);
                JsonObject json = (new JsonObject())
                        .putString("status", "error")
                        .putString("message", "invalid.action");
                message.reply(json);
        }
    }
}