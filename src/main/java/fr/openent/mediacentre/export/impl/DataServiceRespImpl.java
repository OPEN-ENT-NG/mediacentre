package fr.openent.mediacentre.export.impl;

import fr.openent.mediacentre.helper.impl.XmlExportHelperImpl;
import fr.openent.mediacentre.export.DataService;
import fr.wseduc.webutils.Either;
import org.vertx.java.core.Handler;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Container;

import static fr.openent.mediacentre.constants.GarConstants.*;
import static org.entcore.common.neo4j.Neo4jResult.validResultHandler;

public class DataServiceRespImpl extends DataServiceBaseImpl implements DataService {

    DataServiceRespImpl(Container container, String strDate) {
        super(container);
        xmlExportHelper = new XmlExportHelperImpl(container, RESP_ROOT, RESP_FILE_PARAM, strDate);
    }

    @Override
    public void exportData(final Handler<Either<String, JsonObject>> handler) {

        getRespFromNeo4j(new Handler<Either<String, JsonArray>>() {
            @Override
            public void handle(Either<String, JsonArray> respResults) {
                if(validResponseNeo4j(respResults,  handler)) {

                    processStucturesFos(respResults.right().getValue());
                    xmlExportHelper.closeFile();
                    handler.handle(new Either.Right<String, JsonObject>(new JsonObject()));
                }
            }
        });
    }



    /**
     * Get person in charge of affectation from Neo4j
     * Get academic email when available, else get structure email, no personal email
     * @param handler results
     */
    // todo bad return of resp aff (must return array a RESP_STRUCT)
    private void getRespFromNeo4j(Handler<Either<String, JsonArray>> handler) {
        String query = "MATCH (u:User)-[IN]->(n:ManualGroup{name:\"" + CONTROL_GROUP + "\"})" +
                "-[DEPENDS]->(s:Structure) ";
        String dataReturn = "RETURN u.id as `" + PERSON_ID + "`, " +
                "u.lastName as `" + PERSON_NAME + "`, " +
                "u.firstName as `" + PERSON_FIRST_NAME + "`, " +
                "coalesce(u.emailAcademy,s.email) as `" + PERSON_MAIL + "`, " +
                "s.UAI as `" + STRUCTURE_UAI + "` " +
                "order by `" + PERSON_ID + "`, `" + STRUCTURE_UAI + "`";
        neo4j.execute(query + dataReturn, new JsonObject(), validResultHandler(handler));
    }

    /**
     * Process person in chage
     * @param resps Array of respq from Neo4j
     */
    private void processStucturesFos(JsonArray resps) {
        processSimpleArray(resps, RESP_NODE);
    }
}
