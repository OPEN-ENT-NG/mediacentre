package fr.openent.gar.controller;

import fr.openent.gar.Gar;
import fr.openent.gar.export.ExportService;
import fr.openent.gar.export.impl.ExportServiceImpl;
import fr.openent.gar.security.WorkflowUtils;
import fr.openent.gar.service.EventService;
import fr.openent.gar.service.ResourceService;
import fr.openent.gar.service.TarService;
import fr.openent.gar.service.impl.DefaultTarService;
import fr.openent.gar.service.impl.DefaultEventService;
import fr.openent.gar.service.impl.DefaultResourceService;
import fr.openent.gar.export.impl.ExportWorker;
import fr.wseduc.bus.BusAddress;
import fr.wseduc.cron.CronTrigger;
import fr.wseduc.rs.Get;
import fr.wseduc.rs.Post;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.http.Renders;
import fr.wseduc.webutils.request.RequestUtils;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.controller.ControllerHelper;
import org.entcore.common.user.UserUtils;

import java.text.ParseException;

import static fr.wseduc.webutils.Utils.handlerToAsyncHandler;
import static fr.wseduc.webutils.http.response.DefaultResponseHandler.defaultResponseHandler;

public class GarController extends ControllerHelper {

    private final ResourceService resourceService;
    private final EventService eventService;
    private final Vertx vertx;
    private JsonObject garRessourcesConfig = null;
    private Logger log = LoggerFactory.getLogger(GarController.class);
    private EventBus eb = null;
    private JsonObject config;

    public GarController(Vertx vertx, JsonObject config) {
        super();
        eb = vertx.eventBus();
        this.config = config;
        this.vertx = vertx;
        this.garRessourcesConfig = config.getJsonObject("gar-ressources");
        this.eventService = new DefaultEventService(config.getString("event-collection", "gar-events"));
        this.resourceService = new DefaultResourceService(
                vertx,
                Gar.demo ? config.getString("host") : garRessourcesConfig.getString("host"),
                config.getString("id-ent"),
                garRessourcesConfig.getString("cert"),
                garRessourcesConfig.getString("key")
        );
        this.launchExport();
    }

    @Get("")
    @SecuredAction("gar.view")
    public void render(HttpServerRequest request) {
        renderView(request, new JsonObject().put("demo", Gar.demo));
    }

    @Get("/resources")
    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
    public void getResources(final HttpServerRequest request) {
        UserUtils.getUserInfos(eb, request, user -> {
            String structureId = request.params().contains("structure") ? request.getParam("structure") : user.getStructures().get(0);
            String userId = user.getUserId();
            this.resourceService.get(userId, structureId, garRessourcesConfig.getString("host"), result -> {
                            if (result.isRight()) {
                                Renders.renderJson(request, result.right().getValue());
                            } else {
                                Renders.renderJson(request, new JsonArray());
                            }
                        }
            );
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


    @Get("/launchExport")
    @SecuredAction(value = WorkflowUtils.EXPORT, type = ActionType.WORKFLOW)
    public void launchExportFromRoute(HttpServerRequest request) {
        this.exportAndSend();
        request.response().setStatusCode(200).end("Import started");
    }

    private void launchExport() {
        log.info("Start lauchExport (CRON GAR export)------");
        try {
            new CronTrigger(vertx, config.getString("export-cron")).schedule(new Handler<Long>() {
                @Override
                public void handle(Long event) {
                    exportAndSend();
                }
            });

        } catch (ParseException e) {
            log.error("cron GAR failed");
            log.fatal(e.getMessage(), e);
            return;
        }
    }

    private void exportAndSend() {
        eb.send(ExportWorker.class.getSimpleName(),
                new JsonObject().put("action", "exportAndSend"),
                handlerToAsyncHandler(event -> log.info("Export Gar Launched")));
    }

    @BusAddress(Gar.GAR_ADDRESS)
    public void addressHandler(Message<JsonObject> message) {
        String action = message.body().getString("action", "");
        switch (action) {
            case "export" : exportAndSend();
                break;
            case "getConfig":
                log.info("MEDIACENTRE GET CONFIG BUS RECEPTION");
                JsonObject data = (new JsonObject())
                        .put("status", "ok")
                        .put("message", config);
                message.reply(data);
                break;
            case "getResources":
                JsonObject body = message.body();
                String structureId = body.getString("structure");
                String userId = body.getString("user");
                this.resourceService.get(userId, structureId, garRessourcesConfig.getString("host"), result -> {
                            if (result.isRight()) {
                                JsonObject response = new JsonObject()
                                        .put("status", "ok")
                                        .put("message", result.right().getValue());
                                message.reply(response);
                            } else {
                                JsonObject response = new JsonObject()
                                        .put("status", "ko")
                                        .put("message", result.left().getValue());
                                message.reply(response);
                            }
                        }
                );
                break;
            default:
                log.error("Gar invalid.action " + action);
                JsonObject json = (new JsonObject())
                        .put("status", "error")
                        .put("message", "invalid.action");
                message.reply(json);
        }
    }
}