package fr.openent.mediacentre.controller;

import fr.openent.mediacentre.Mediacentre;
import fr.openent.mediacentre.export.ExportService;
import fr.openent.mediacentre.export.impl.ExportServiceImpl;
import fr.openent.mediacentre.service.EventService;
import fr.openent.mediacentre.service.ResourceService;
import fr.openent.mediacentre.service.impl.DefaultEventService;
import fr.openent.mediacentre.service.impl.DefaultResourceService;
import fr.wseduc.bus.BusAddress;
import fr.wseduc.rs.Get;
import fr.wseduc.rs.Post;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.request.RequestUtils;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.controller.ControllerHelper;
import org.entcore.common.user.UserUtils;

import static fr.wseduc.webutils.Utils.handlerToAsyncHandler;
import static fr.wseduc.webutils.http.response.DefaultResponseHandler.arrayResponseHandler;
import static fr.wseduc.webutils.http.response.DefaultResponseHandler.defaultResponseHandler;

public class MediacentreController extends ControllerHelper {

    private ExportService exportService;
    private final ResourceService resourceService;
    private final EventService eventService;
    private Logger log = LoggerFactory.getLogger(MediacentreController.class);
    private EventBus eb = null;

    public MediacentreController(Vertx vertx, JsonObject config) {
        super();
        eb = vertx.eventBus();
        this.exportService = new ExportServiceImpl(config);
        this.eventService = new DefaultEventService(config.getString("event-collection", "gar-events"));
        this.resourceService = new DefaultResourceService(
                vertx,
                config.getString("gar-host"),
                config.getString("id-ent"),
                config.getString("cert-path"),
                config.getString("key-path")
        );
    }

    @Get("")
    @SecuredAction("mediacentre.view")
    public void render(HttpServerRequest request) {
        renderView(request);
    }

    @Get("/resources")
    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
    public void getResources(final HttpServerRequest request) {
        UserUtils.getUserInfos(eb, request, user -> {
            String structureId = request.params().contains("structure") ? request.getParam("structure") : user.getStructures().get(0);
            String userId = user.getUserId();
            this.resourceService.get(userId, structureId, getHost(request), arrayResponseHandler(request));
        });
    }

    @Post("/event")
    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
    public void postEvent(HttpServerRequest request) {
        RequestUtils.bodyToJson(request, pathPrefix + "event", body -> {
            UserUtils.getUserInfos(eb, request, user -> {
                eventService.add(body, user, defaultResponseHandler(request));
            });
        });
    }

    @Get("testexport")
    public void testExport(HttpServerRequest request) {
        exportService.launchExport(defaultResponseHandler(request));
        //defaultResponseHandler(request).handle(new Either.Left<String, JsonObject>("Toto"));
    }

    @Get("testsftp")
    public void testsftp(final HttpServerRequest request) {
        JsonObject test = new JsonObject().put("action","send")
                .put("known-hosts","C:\\Users\\colenot\\.ssh\\known_hosts")
                .put("hostname","213.32.49.235")
                .put("port",24242)
                .put("username","sftpODE")
                .put("sshkey","C:\\CGI\\DATA\\ARANGER\\DATA\\Support\\Alimentation\\priv_key.ppk")
                .put("local-file","C:\\Users\\colenot\\.ssh\\known_hosts")
                .put("dist-file","/testtcol.txt");
        eb.send("sftp", test,  handlerToAsyncHandler(new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {
                defaultResponseHandler(request).handle(new Either.Left<String, JsonObject>("Toto"));
            }
        }));
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
                        .put("status", "error")
                        .put("message", "invalid.action");
                message.reply(json);
        }
    }
}