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
import fr.wseduc.cron.CronTrigger;
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
import java.text.ParseException;

import static fr.wseduc.webutils.Utils.handlerToAsyncHandler;
import static fr.wseduc.webutils.http.response.DefaultResponseHandler.arrayResponseHandler;
import static fr.wseduc.webutils.http.response.DefaultResponseHandler.defaultResponseHandler;

public class MediacentreController extends ControllerHelper {

    private ExportService exportService;
    private TarService tarService;
    private final ResourceService resourceService;
    private final EventService eventService;
    private final Vertx vertx;
    private JsonObject sftpGarConfig = null;
    private JsonObject garRessourcesConfig = null;
    private Logger log = LoggerFactory.getLogger(MediacentreController.class);
    private EventBus eb = null;
    private JsonObject config;

    public MediacentreController(Vertx vertx, JsonObject config) {
        super();
        eb = vertx.eventBus();
        this.config = config;
        this.vertx = vertx;
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
        this.launchExport();
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


    @Get("/launchExport")
    @SecuredAction(value = WorkflowUtils.EXPORT, type = ActionType.WORKFLOW)
    public void launchExportFromRoute(HttpServerRequest request) {
        this.launchExport();
        request.response().setStatusCode(200).end("Import started");
    }

    private void launchExport() {
        log.info("Start lauchExport (GAR export)------");
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
        log.info("Start Export (Generate xml files, compress to tar.gz, generate md5, send to GAR by sftp");
        emptyDIrectory(config.getString("export-path"));
        log.info("Generate XML files");
        exportService.launchExport((Either<String, JsonObject> event1) -> {
            if (event1.isRight()) {
                File directory = new File(config.getString("export-path"));
                log.info("Tar.GZ to Compress");
                emptyDIrectory(config.getString("export-archive-path"));
                tarService.compress(config.getString("export-archive-path"), directory, (Either<String, JsonObject> event2) -> {
                    if(event2.isRight() && event2.right().getValue().containsKey("archive")) {
                        String archiveName = event2.right().getValue().getString("archive");
                        //SFTP sender
                        log.info("Send to GAR tar GZ by sftp: "+ archiveName);
                        JsonObject sendTOGar = new JsonObject().put("action", "send")
                                .put("known-hosts", sftpGarConfig.getString("known-hosts"))
                                .put("hostname", sftpGarConfig.getString("host"))
                                .put("port", sftpGarConfig.getInteger("port"))
                                .put("username", sftpGarConfig.getString("username"))
                                .put("sshkey", sftpGarConfig.getString("sshkey"))
                                .put("passphrase", sftpGarConfig.getString("passphrase"))
                                .put("local-file", config.getString("export-archive-path") + archiveName)
                                .put("dist-file", sftpGarConfig.getString("dir-dest") + archiveName);
                        eb.send("sftp", sendTOGar, handlerToAsyncHandler(message -> {
                            if(message.body().containsKey("status") && message.body().getString("status") == "error"){
                                log.info("FAILED Send to GAR tar GZ by sftp");
                            }
                            else {
                                String md5File = event2.right().getValue().getString("md5File");
                                log.info("Send to GAR md5 by sftp: " + md5File);
                                sendTOGar
                                        .put("local-file", config.getString("export-archive-path") + md5File)
                                        .put("dist-file", sftpGarConfig.getString("dir-dest") + md5File);
                                eb.send("sftp", sendTOGar, handlerToAsyncHandler(message1 -> {
                                    if (message1.body().containsKey("status") && message1.body().getString("status") == "error") {
                                        log.info("FAILED Send to Md5 by sftp");
                                    } else {
                                        log.info("SUCCESS Export and Send to GAR");
                                    }
                                }));
                            }
                        }));
                    } else {
                        log.error("Failed Export and Send to GAR");
                    }
                });
            }
            else{
                log.error("Failed Export and Send to GAR");
            }
        });
    }

    private void emptyDIrectory (String path){
        File index = new File(path);
        String[] entries = index.list();
        for (String s : entries) {
            File currentFile = new File(index.getPath(), s);
            currentFile.delete();
        }
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