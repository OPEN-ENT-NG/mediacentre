package fr.openent.mediacentre.controller;

import fr.openent.mediacentre.Mediacentre;
import fr.openent.mediacentre.export.ExportService;
import fr.openent.mediacentre.export.impl.ExportServiceImpl;
import fr.openent.mediacentre.security.WorkflowUtils;
import fr.openent.mediacentre.service.EventService;
import fr.openent.mediacentre.service.ResourceService;
import fr.openent.mediacentre.service.TarService;
import fr.openent.mediacentre.service.impl.DefaultTarService;
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

import java.io.*;

import static fr.wseduc.webutils.Utils.handlerToAsyncHandler;
import static fr.wseduc.webutils.http.response.DefaultResponseHandler.arrayResponseHandler;
import static fr.wseduc.webutils.http.response.DefaultResponseHandler.defaultResponseHandler;

public class MediacentreController extends ControllerHelper {

    private ExportService exportService;
    private TarService tarService;
    private final ResourceService resourceService;
    private final EventService eventService;
    private JsonObject sftpGarConfig = null;
    private JsonObject garRessourcesConfig = null;
    private Logger log = LoggerFactory.getLogger(MediacentreController.class);
    private EventBus eb = null;
    private JsonObject config;

    public MediacentreController(Vertx vertx, JsonObject config) {
        super();
        eb = vertx.eventBus();
        this.config = config;
        this.sftpGarConfig = config.getJsonObject("gar-sftp");
        this.garRessourcesConfig = config.getJsonObject("gar-ressources");
        this.exportService = new ExportServiceImpl(config);
        this.tarService = new DefaultTarService();
        this.eventService = new DefaultEventService(config.getString("event-collection", "gar-events"));
        this.resourceService = new DefaultResourceService(
                vertx,
                garRessourcesConfig.getString("host"),
                config.getString("id-ent"),
                garRessourcesConfig.getString("cert"),
                garRessourcesConfig.getString("key")
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
            this.resourceService.get(userId, structureId, garRessourcesConfig.getString("host"), arrayResponseHandler(request));
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

    @Get("/export")
    @SecuredAction(value = WorkflowUtils.EXPORT , type = ActionType.WORKFLOW)
    public void testsftp(final HttpServerRequest request) {
        log.info("INIT SFTP SEND DATA READY");
        File directory = new File(config.getString("export-path"));
        tarService.compress(config.getString("export-archive-path"), directory, (Either<String, JsonObject> event) -> {
            if(event.isRight() && event.right().getValue().containsKey("archive")) {
                String archiveName = event.right().getValue().getString("archive");
                //SFTP sender
                JsonObject sendTOGar = new JsonObject().put("action", "send")
                        .put("known-hosts", sftpGarConfig.getString("known-hosts"))
                        .put("hostname", sftpGarConfig.getString("host"))
                        .put("port", sftpGarConfig.getInteger("port"))
                        .put("username", sftpGarConfig.getString("username"))
                        .put("sshkey", sftpGarConfig.getString("sshkey"))
                        .put("passphrase", sftpGarConfig.getString("passphrase"))
                        .put("local-file", config.getString("export-archive-path") + archiveName)
                        .put("dist-file", sftpGarConfig.getString("dir-dest") + archiveName);
                eb.send("sftp", sendTOGar, handlerToAsyncHandler(new Handler<Message<JsonObject>>() {
                    @Override
                    public void handle(Message<JsonObject> message) {
                        if(message.body().containsKey("status") && message.body().getString("status") == "error"){
                            defaultResponseHandler(request).handle(new Either.Left<>(message.body().toString()));
                        }
                        else {
                            request.response().setStatusCode(200).end();
                        }
                    }
                }));

            } else {
                defaultResponseHandler(request).handle(new Either.Left<>(event.toString()));
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
            case "getConfig":
                log.info("MEDIACENTRE GET CONFIG BUS RECEPTION");
                JsonObject data = (new JsonObject())
                        .put("status", "ok")
                        .put("message", config);
                message.reply(data);
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