package fr.openent.gar.export.impl;

import fr.openent.gar.helper.impl.PaginatorHelperImpl;
import fr.openent.gar.helper.impl.XmlExportHelperImpl;
import fr.openent.gar.export.DataService;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;


import static fr.openent.gar.constants.GarConstants.*;

public class DataServiceRespImpl extends DataServiceBaseImpl implements DataService {

    private PaginatorHelperImpl paginator;

    DataServiceRespImpl(JsonObject config, String strDate) {
        super(config);
        xmlExportHelper = new XmlExportHelperImpl(config, RESP_ROOT, RESP_FILE_PARAM, strDate);
        paginator = new PaginatorHelperImpl();
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
                } else {
                    log.error("[DataServiceRespImpl@exportData] Failed to process");
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

        String query = "MATCH (us:User)-[:IN]->(n:ManualGroup{name:\"" + CONTROL_GROUP + "\"})-[:DEPENDS]->(s:Structure) " +
                " WHERE (us.profiles = ['Teacher'] OR us.profiles = ['Personnel']) " +
                " AND NOT(HAS(us.deleteDate)) " +
                " AND NOT(HAS(us.disappearanceDate))" +
                " AND HAS(s.exports) AND 'GAR' IN s.exports" +
                " AND (HAS(us.emailAcademy) OR HAS(us.emailInternal) OR HAS(us.email)) " +
                " WITH s, us ORDER BY s.id , us.id "+
                " WITH s, collect(us)[..15] as uc "+    // 15 first Teachers or Personnels in each Structures
                " UNWIND uc as u ";
        // CAUTION Don't use sr.UAI in dataReturn cause this structure is perhaps not a GAR structure
        String dataReturn = "RETURN DISTINCT u.id as `" + PERSON_ID + "`, " +
                "u.lastName as `" + PERSON_NAME + "`, " +
                "u.firstName as `" + PERSON_FIRST_NAME + "`, " +
                // Priority = emailAcademy > emailInternal > email
                "coalesce(u.emailAcademy, u.emailInternal, u.email) as `" + PERSON_MAIL + "`, " +
                "collect(s.UAI) as `" + RESP_ETAB + "` " +
                "order by `" + PERSON_ID + "` " ;

        query = query + dataReturn;
        query += " ASC SKIP {skip} LIMIT {limit} ";

        JsonObject params = new JsonObject().put("limit", paginator.LIMIT);
        paginator.neoStreamList(query, params, new JsonArray(), 0, handler);
    }

    /**
     * Process person in charge
     * @param resps Array of respq from Neo4j
     */
    private void processStucturesFos(JsonArray resps) {
        processSimpleArray(resps, RESP_NODE, RESP_NODE_MANDATORY);
    }
}
