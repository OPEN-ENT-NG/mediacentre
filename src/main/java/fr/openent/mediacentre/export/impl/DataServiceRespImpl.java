package fr.openent.mediacentre.export.impl;

import fr.openent.mediacentre.helper.impl.XmlExportHelperImpl;
import fr.openent.mediacentre.export.DataService;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;


import static fr.openent.mediacentre.constants.GarConstants.*;
import static org.entcore.common.neo4j.Neo4jResult.validResultHandler;

public class DataServiceRespImpl extends DataServiceBaseImpl implements DataService {

    DataServiceRespImpl(JsonObject config, String strDate) {
        super(config);
        xmlExportHelper = new XmlExportHelperImpl(config, RESP_ROOT, RESP_FILE_PARAM, strDate);
    }

    @Override
    public void exportData(final Handler<Either<String, JsonObject>> handler) {

        getRespFromNeo4j(new Handler<Either<String, JsonArray>>() {
            @Override
            public void handle(Either<String, JsonArray> respResults) {
                if(validResponseNeo4j(respResults,  handler)) {

                    processStucturesFos(respResults.right().getValue());
                    xmlExportHelper.closeFile();
                    handler.handle(new Either.Right<String, JsonObject>(
                            new JsonObject().put(
                                    FILE_LIST_KEY,
                                    xmlExportHelper.getFileList()
                            )));
                }
            }
        });
    }


    /**
     * Get person in charge of affectation from Neo4j
     * Get academic email when available, else get structure email, no personal email
     * @param handler results
     */
    private void getRespFromNeo4j(Handler<Either<String, JsonArray>> handler) {
        String query = "MATCH (sr:Structure)<-[ADMINISTRATIVE_ATTACHMENT]-(u:User)-[IN]->" +
                "(n:ManualGroup{name:\"" + CONTROL_GROUP + "\"})-[DEPENDS]->(s:Structure) ";
        String dataReturn = "RETURN u.id as `" + PERSON_ID + "`, " +
                "u.lastName as `" + PERSON_NAME + "`, " +
                "u.firstName as `" + PERSON_FIRST_NAME + "`, " +
                "coalesce(u.emailAcademy,sr.email) as `" + PERSON_MAIL + "`, " +
                "collect(s.UAI) as `" + RESP_ETAB + "` " +
                "order by `" + PERSON_ID + "`";
        neo4j.execute(query + dataReturn, new JsonObject(), validResultHandler(handler));
    }

    /**
     * Process person in chage
     * @param resps Array of respq from Neo4j
     */
    private void processStucturesFos(JsonArray resps) {
        processSimpleArray(resps, RESP_NODE, RESP_NODE_MANDATORY);
    }
}
