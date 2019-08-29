package fr.openent.mediacentre.controller;

import fr.openent.mediacentre.export.impl.ExportImpl;
import fr.openent.mediacentre.service.ParameterService;
import fr.openent.mediacentre.service.impl.DefaultParameterService;
import fr.wseduc.rs.ApiDoc;
import fr.wseduc.rs.Get;
import fr.wseduc.rs.Post;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.request.RequestUtils;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpServerRequest;
import org.entcore.common.controller.ControllerHelper;
import org.entcore.common.http.filter.ResourceFilter;
import org.entcore.common.http.filter.SuperAdminFilter;
import org.entcore.common.http.response.DefaultResponseHandler;

public class SettingController extends ControllerHelper {

    private ParameterService parameterService;

    public SettingController(EventBus eb) {
        super();
        this.parameterService = new DefaultParameterService(eb);
    }
    /**
     * Access parameter url via url/mediacentre/parameter
     * Only access right
     */
    @Get("/parameter")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(SuperAdminFilter.class)
    @ApiDoc("Render parameter view")
    public void setting(HttpServerRequest request) {
        renderView(request, null, "parameter.html", null);
    }

    @Get("/structure/gar")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(SuperAdminFilter.class)
    @ApiDoc("get gar structure")
    public void getStructureGar(final HttpServerRequest request) {
        parameterService.getStructureGar(DefaultResponseHandler.arrayResponseHandler(request));
    }

    @Post("/structure/gar/group")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(SuperAdminFilter.class)
    @ApiDoc("Create group to gar structure")
    public void createGarGroupToStructure(final HttpServerRequest request) {
        RequestUtils.bodyToJson(request, parameter -> {
            parameterService.createGarGroupToStructure(parameter, DefaultResponseHandler.defaultResponseHandler(request));
        });

    }

    @Post("/structure/gar/group/user")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(SuperAdminFilter.class)
    @ApiDoc("Add user to gar group")
    public void addUserToGarGroup(final HttpServerRequest request) {
        RequestUtils.bodyToJson(request, parameter -> {
            parameterService.addUserToGarGroup(parameter, DefaultResponseHandler.defaultResponseHandler(request));
        });

    }

    @Get("/mail/test")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(SuperAdminFilter.class)
    @ApiDoc("Test send mail")
    public void testMail(HttpServerRequest request) {
        ExportImpl export = new ExportImpl(vertx);
        export.sendReport("This is a test mail");
        ok(request);
    }
}
