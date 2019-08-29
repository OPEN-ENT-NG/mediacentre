package fr.openent.mediacentre.export.impl;

import fr.openent.mediacentre.export.ExportService;
import fr.openent.mediacentre.export.XMLValidationHandler;
import fr.openent.mediacentre.service.TarService;
import fr.openent.mediacentre.service.impl.DefaultTarService;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.data.FileResolver;
import fr.wseduc.webutils.email.EmailSender;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.impl.BufferImpl;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.email.EmailFactory;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static fr.openent.mediacentre.Mediacentre.CONFIG;
import static fr.wseduc.webutils.Utils.handlerToAsyncHandler;

public class ExportImpl {

    private Logger log = LoggerFactory.getLogger(ExportImpl.class);
    private ExportService exportService;
    private TarService tarService;
    private JsonObject sftpGarConfig;
    private EventBus eb;
    private JsonObject config;
    private Vertx vertx;
    private EmailSender emailSender;

    public ExportImpl(Vertx vertx, Handler<String> handler) {
        this.vertx = vertx;
        this.config = CONFIG;
        this.eb = vertx.eventBus();
        this.exportService = new ExportServiceImpl(config);
        this.tarService = new DefaultTarService();
        this.sftpGarConfig = config.getJsonObject("gar-sftp");
        this.emailSender = new EmailFactory(vertx, config).getSender();

        this.exportAndSend(handler);
    }

    public ExportImpl(Vertx vertx) {
        this.config = CONFIG;
        this.emailSender = new EmailFactory(vertx, config).getSender();
    }

    private void exportAndSend(Handler<String> handler) {
        log.info("Start exportAndSend GAR (Generate xml files, XSD validation, compress to tar.gz, generate md5, send to GAR by sftp");
        try {
            emptyDIrectory(config.getString("export-path"));
        } catch (Exception e) {
            handler.handle(e.getMessage());
        }
        log.info("Generate XML files");
        exportService.launchExport((Either<String, JsonObject> event1) -> {
            if (event1.isRight()) {
                File directory = new File(config.getString("export-path"));
                Map<String, Object> validationResult = validateXml(directory);
                boolean isValid = (boolean) validationResult.get("valid");
                if (!isValid) {
                    log.info(validationResult.get("report"));
                    saveXsdValidation((String) validationResult.get("report"));
                    sendReport((String) validationResult.get("report"));
                    handler.handle("XSV VALIDATION ERROR");
                    return;
                }
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
                                String e = "Send to GAR tar GZ by sftp";
                                log.error(e);
                                handler.handle(e);
                            } else {
                                String md5File = event2.right().getValue().getString("md5File");
                                log.info("Send to GAR md5 by sftp: " + md5File);
                                sendTOGar
                                        .put("local-file", config.getString("export-archive-path") + md5File)
                                        .put("dist-file", sftpGarConfig.getString("dir-dest") + md5File);
                                eb.send(node + "sftp", sendTOGar, handlerToAsyncHandler(message1 -> {
                                    if (message1.body().containsKey("status") && message1.body().getString("status") == "error") {
                                        String e = "FAILED Send to Md5 by sftp";
                                        log.error(e);
                                        handler.handle(e);
                                    } else {
                                        log.info("SUCCESS Export and Send to GAR");
                                        handler.handle("SUCCESS");
                                    }
                                }));
                            }
                        }));
                    } else {
                        String e = "Failed Export and Send to GAR, tar service";
                        log.error(e);
                        handler.handle(e);
                    }
                });
            } else {
                String e = "Failed Export and Send to GAR export service";
                log.error(e);
                handler.handle(e);
            }
        });
    }

    public void sendReport(String report) {
        JsonArray recipients = config.getJsonArray("xsd-recipient-list", new JsonArray());
        String subject = "[GAR][" + config.getString("host") + "] XSD Validation error";
        for (int i = 0; i < recipients.size(); i++) {
            String recipient = recipients.getString(i);
            emailSender.sendEmail(null, recipient, null, null, subject, report, null, false, null);
        }
    }

    private void saveXsdValidation(String report) {
        String filePath = config.getString("export-archive-path") + "xsd_errors.log";
        vertx.fileSystem().writeFile(filePath, new BufferImpl().setBytes(0, report.getBytes()), event -> {
            if (event.failed()) log.error("Failed to write xsd errors");
        });
    }

    private Map<String, Object> validateXml(File directory) {
        Map<String, Object> result = new HashMap<>();
        XMLValidationHandler errorHandler = new XMLValidationHandler();
        try {
            SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            String schemaPath = FileResolver.absolutePath("public/xsd");
            Schema schema = factory.newSchema(new File(schemaPath + "/GAR-ENT.xsd"));
            Validator validator = schema.newValidator();
            validator.setErrorHandler(errorHandler);
            String[] files = directory.list();
            if (files == null) {
                result.put("valid", true);
                return result;
            }
            for (String f : files) {
                File currentFile = new File(directory.getPath(), f);
                validator.validate(new StreamSource(currentFile));
            }
        } catch (SAXException | IOException e) {
            log.error("Error while validating xml", e);
            result.put("valid", false);
        } finally {
            result.put("valid", errorHandler.isValid());
            result.put("report", errorHandler.report());
        }
        return result;
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
