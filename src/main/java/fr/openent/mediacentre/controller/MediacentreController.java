package fr.openent.mediacentre.controller;

import fr.openent.mediacentre.service.ExportService;
import fr.openent.mediacentre.service.impl.ExportServiceImpl;
import fr.wseduc.rs.Get;
import org.entcore.common.controller.ControllerHelper;
import org.vertx.java.core.http.HttpServerRequest;

import static fr.wseduc.webutils.http.response.DefaultResponseHandler.defaultResponseHandler;

public class MediacentreController extends ControllerHelper {

    private final ExportService exportService;

    public MediacentreController() {
        super();
        exportService = new ExportServiceImpl();
    }

    @Get("testexport")
    public void testExport(HttpServerRequest request) {
        exportService.test(defaultResponseHandler(request));
    }
}