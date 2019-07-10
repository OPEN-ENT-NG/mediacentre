package fr.openent.mediacentre.service.impl;

import fr.openent.mediacentre.controller.MediacentreController;
import fr.openent.mediacentre.export.ExportService;
import fr.openent.mediacentre.export.impl.ExportServiceImpl;
import fr.openent.mediacentre.service.EventService;
import fr.openent.mediacentre.service.ResourceService;
import fr.openent.mediacentre.service.TarService;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.collections.PersistantBuffer;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.vertx.java.busmods.BusModBase;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import static fr.openent.mediacentre.Mediacentre.CONFIG;
import static fr.wseduc.webutils.Utils.handlerToAsyncHandler;

public class ExportWorker extends BusModBase implements Handler<Message<JsonObject>> {

    private final Map<String, PersistantBuffer> buffers = new HashMap<>();
    private final Map<String, MessageConsumer<byte[]>> consumers = new HashMap<>();
    private Logger log = LoggerFactory.getLogger(MediacentreController.class);
    private ExportService exportService;
    private TarService tarService;
    private JsonObject sftpGarConfig = null;
    private JsonObject garRessourcesConfig = null;
    private EventBus eb = null;
    private JsonObject config;
    private ResourceService resourceService;
    private EventService eventService;

    @Override
    public void start() {
        super.start();
        vertx.eventBus().localConsumer(ExportWorker.class.getSimpleName(), this);
        eventService = null;
        this.config = CONFIG;
        this.vertx = vertx;
        this.eb = vertx.eventBus();
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

    @Override
    public void handle(Message<JsonObject> message) {
        final String action = message.body().getString("action", "");
        switch (action) {
            case "exportAndSend":
                exportAndSend(message);
                break;
        }
    }


    private void exportAndSend(Message<JsonObject> message) {
        log.info("Start exportAndSend GAR (Generate xml files, compress to tar.gz, generate md5, send to GAR by sftp");
        emptyDIrectory(config.getString("export-path"));
        log.info("Generate XML files");
        exportService.launchExport((Either<String, JsonObject> event1) -> {
            if (event1.isRight()) {
                File directory = new File(config.getString("export-path"));
                log.info("Tar.GZ to Compress");
                emptyDIrectory(config.getString("export-archive-path"));
                tarService.compress(config.getString("export-archive-path"), directory, (Either<String, JsonObject> event2) -> {
                    if (event2.isRight() && event2.right().getValue().containsKey("archive")) {
                        String archiveName = event2.right().getValue().getString("archive");
                        //SFTP sender
                        log.info("Send to GAR tar GZ by sftp: " + archiveName);
                        JsonObject sendTOGar = new JsonObject().put("action", "send")
                                .put("known-hosts", sftpGarConfig.getString("known-hosts"))
                                .put("hostname", sftpGarConfig.getString("host"))
                                .put("port", sftpGarConfig.getInteger("port"))
                                .put("username", sftpGarConfig.getString("username"))
                                .put("sshkey", sftpGarConfig.getString("sshkey"))
                                .put("passphrase", sftpGarConfig.getString("passphrase"))
                                .put("local-file", config.getString("export-archive-path") + archiveName)
                                .put("dist-file", sftpGarConfig.getString("dir-dest") + archiveName);

                        String n = (String) vertx.sharedData().getLocalMap("server").get("node");
                        String node = (n != null) ? n : "";

                        eb.send(node + "sftp", sendTOGar, handlerToAsyncHandler((Message<JsonObject> messageResponse) -> {
                            if (messageResponse.body().containsKey("status") && messageResponse.body().getString("status") == "error") {
                                log.error("FAILED Send to GAR tar GZ by sftp");
                            } else {
                                String md5File = event2.right().getValue().getString("md5File");
                                log.info("Send to GAR md5 by sftp: " + md5File);
                                sendTOGar
                                        .put("local-file", config.getString("export-archive-path") + md5File)
                                        .put("dist-file", sftpGarConfig.getString("dir-dest") + md5File);
                                eb.send(node + "sftp", sendTOGar, handlerToAsyncHandler(message1 -> {
                                    if (message1.body().containsKey("status") && message1.body().getString("status") == "error") {
                                        log.error("FAILED Send to Md5 by sftp");
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
            } else {
                log.error("Failed Export and Send to GAR");
            }
        });
    }

    //TODO prévenir les nullpointer ici
    private void emptyDIrectory(String path) {
        File index = new File(path);
        String[] entries = index.list();
        for (String s : entries) {
            File currentFile = new File(index.getPath(), s);
            currentFile.delete();
        }
    }

}
