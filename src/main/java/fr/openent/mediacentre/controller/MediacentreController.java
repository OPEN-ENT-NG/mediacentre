package fr.openent.mediacentre.controller;

import fr.openent.mediacentre.Mediacentre;
import fr.openent.mediacentre.security.WorkflowUtils;
import fr.openent.mediacentre.service.EventService;
import fr.openent.mediacentre.service.ResourceService;
import fr.openent.mediacentre.service.impl.DefaultEventService;
import fr.openent.mediacentre.service.impl.DefaultResourceService;
import fr.openent.mediacentre.export.impl.ExportWorker;
import fr.wseduc.bus.BusAddress;
import fr.wseduc.rs.Get;
import fr.wseduc.rs.Post;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.http.Renders;
import fr.wseduc.webutils.request.RequestUtils;
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

import static fr.wseduc.webutils.Utils.handlerToAsyncHandler;
import static fr.wseduc.webutils.http.response.DefaultResponseHandler.defaultResponseHandler;

public class MediacentreController extends ControllerHelper {

    private final ResourceService resourceService;
    private final EventService eventService;
    private final Vertx vertx;
    private Logger log = LoggerFactory.getLogger(MediacentreController.class);
    private EventBus eb = null;
    private JsonObject config;

    public MediacentreController(Vertx vertx, JsonObject config) {
        super();
        eb = vertx.eventBus();
        this.config = config;
        this.vertx = vertx;
        this.eventService = new DefaultEventService(config.getString("event-collection", "gar-events"));
        this.resourceService = new DefaultResourceService(
                vertx,
                config.getJsonObject("gar-ressources"),
                config.getJsonObject("id-ent")
        );
    }

    @Get("")
    @SecuredAction("mediacentre.view")
    public void render(HttpServerRequest request) {
        renderView(request, new JsonObject().put("demo", Mediacentre.demo));
    }

    @Get("/resources")
    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
    public void getResources(final HttpServerRequest request) {
        UserUtils.getUserInfos(eb, request, user -> {
            String structureId = request.params().contains("structure") ? request.getParam("structure") : user.getStructures().get(0);
            String userId = user.getUserId();
            this.resourceService.get(userId, structureId, Renders.getHost(request), result -> {
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
        this.exportAndSend(config.getJsonObject("id-ent").getString(Renders.getHost(request)));
        request.response().setStatusCode(200).end("Import started");
    }

    private void exportAndSend(final String entId) {
        eb.send(ExportWorker.class.getSimpleName(),
                new JsonObject().put("action", "exportAndSend").put("entId", entId),
                handlerToAsyncHandler(event -> log.info("Export Gar Launched")));
    }

    @BusAddress(Mediacentre.MEDIACENTRE_ADDRESS)
    public void addressHandler(Message<JsonObject> message) {
        String action = message.body().getString("action", "");
        switch (action) {
            case "export" : exportAndSend(null);
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