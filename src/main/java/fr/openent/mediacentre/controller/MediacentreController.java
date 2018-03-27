package fr.openent.mediacentre.controller;

import fr.openent.mediacentre.service.ExportService;
import fr.openent.mediacentre.service.impl.ExportServiceImpl;
import fr.wseduc.rs.Get;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.security.SecuredAction;
import org.entcore.common.controller.ControllerHelper;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.RouteMatcher;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Container;

import java.util.Map;

import static fr.wseduc.webutils.http.response.DefaultResponseHandler.defaultResponseHandler;

public class MediacentreController extends ControllerHelper {

    private ExportService exportService;

    public MediacentreController() {
        super();
    }

    @Override
    public void init(Vertx vertx, Container container, RouteMatcher rm, Map<String, SecuredAction> securedActions) {
        super.init(vertx, container, rm, securedActions);
        this.exportService = new ExportServiceImpl(container);
    }

    @Get("testexport")
    public void testExport(HttpServerRequest request) {
        exportService.test(defaultResponseHandler(request));
        //defaultResponseHandler(request).handle(new Either.Left<String, JsonObject>("Toto"));
    }
}