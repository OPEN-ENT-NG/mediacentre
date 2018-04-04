package fr.openent.mediacentre.service.impl;

import fr.openent.mediacentre.helper.impl.XmlExportHelperImpl;
import fr.openent.mediacentre.service.DataService;
import fr.wseduc.webutils.Either;
import org.vertx.java.core.Handler;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Container;
import static fr.openent.mediacentre.constants.GarConstants.*;
import static org.entcore.common.neo4j.Neo4jResult.validResultHandler;

public class DataServiceGroupImpl extends DataServiceBaseImpl implements DataService {

    DataServiceGroupImpl(Container container, String strDate) {
        super(container);
        xmlExportHelper = new XmlExportHelperImpl(container, GROUPS_ROOT, GROUPS_FILE_PARAM, strDate);
    }

    @Override
    public void exportData(final Handler<Either<String, JsonObject>> handler) {

        getGroupsInfoFromNeo4j(new Handler<Either<String, JsonArray>>() {
            @Override
            public void handle(Either<String, JsonArray> groupsResults) {
                if(getValidNeoResponse(groupsResults, handler)) {

                    processGroupsInfo(groupsResults.right().getValue());
                    getGroupsPersonFromNeo4j(new Handler<Either<String, JsonArray>>() {
                        @Override
                        public void handle(final Either<String, JsonArray> groupPersonResults) {
                            if(getValidNeoResponse(groupPersonResults, handler)) {

                                processGroupPersonInfo(groupPersonResults.right().getValue());
                                getGroupsSubjectsFromNeo4j(new Handler<Either<String, JsonArray>>() {
                                    @Override
                                    public void handle(Either<String, JsonArray> groupSubjectResults) {
                                        if(getValidNeoResponse(groupSubjectResults, handler)) {

                                            processGroupSubjectsInfo(groupSubjectResults.right().getValue());
                                            xmlExportHelper.closeFile();
                                            handler.handle(new Either.Right<String, JsonObject>(new JsonObject()));
                                        }
                                    }
                                });
                            }
                        }
                    });

                }
            }
        });
    }




    /**
     * Get groups info from Neo4j
     * Get classes (or divisions)
     * Get user groups. Link groups to classes only for students
     * Get group external id if it exists, else get internal id
     * @param handler results
     */
    private void getGroupsInfoFromNeo4j(Handler<Either<String, JsonArray>> handler) {
        String classQuery = "MATCH (c:Class)-[BELONGS]->(s:Structure)" +
                "<-[:DEPENDS]-(g:Group{name:\"" + CONTROL_GROUP + "\"}) " +
                "RETURN distinct s.UAI as `" + STRUCTURE_UAI + "`, " +
                "split(c.externalId,\"$\")[1] as `" + GROUPS_CODE + "`, " +
                "c.name as `" + GROUPS_DESC + "`, " +
                "null as `" + GROUPS_DIVISION + "`, " +
                "\"" + GROUPS_DIVISION_NAME + "\" as `" + GROUPS_STATUS + "` " +
                "order by `" + STRUCTURE_UAI + "`, `" + GROUPS_CODE + "` " +
                "UNION ";
        String groupsQuery = "MATCH (u:User)-[COMMUNIQUE]->(fg:FunctionalGroup)-[d2:DEPENDS]->" +
                "(s:Structure)<-[:DEPENDS]-(g:Group{name:\"" + CONTROL_GROUP + "\"}) " +
                "OPTIONAL MATCH (c:Class)<-[d:DEPENDS]-(pg:ProfileGroup)<-[IN]-(u:User) " +
                "WHERE u.profiles = ['Student'] " +
                "with s.UAI as uai, " +
                "coalesce(split(fg.externalId,\"$\")[1], fg.id) as id, " +
                "collect(split(c.externalId,\"$\")[1]) as dividlist, " +
                "fg.name as name " +
                "UNWIND (CASE dividlist WHEN [] then [null] else dividlist end) as divid " +
                "return distinct uai as `" + STRUCTURE_UAI + "`, " +
                "id as `" + GROUPS_CODE + "`, " +
                "name as `" + GROUPS_DESC + "`, " +
                "divid as `" + GROUPS_DIVISION + "`, " +
                "\"" + GROUPS_GROUP_NAME + "\" as `" + GROUPS_STATUS + "` " +
                "order by `" + STRUCTURE_UAI + "`, `" + GROUPS_CODE + "`";
        neo4j.execute(classQuery + groupsQuery, new JsonObject(), validResultHandler(handler));
    }

    /**
     * Process groups info
     * @param groups Array of groups from Neo4j
     */
    private void processGroupsInfo(JsonArray groups) {
        processSimpleArray(groups, GROUPS_NODE);
    }

    /**
     * Get groups content from Neo4j
     * @param handler results
     */
    private void getGroupsPersonFromNeo4j(Handler<Either<String, JsonArray>> handler) {
        String classQuery = "MATCH (u:User)-[IN]->(pg:ProfileGroup)-[DEPENDS]->(c:Class)-[BELONGS]->(s:Structure)" +
                "<-[:DEPENDS]-(g:Group{name:\"" + CONTROL_GROUP + "\"}) " +
                "return distinct s.UAI as `" + STRUCTURE_UAI + "`, " +
                "u.id as `" + PERSON_ID + "`, " +
                "coalesce(split(c.externalId,\"$\")[1], c.id) as `" + GROUPS_CODE + "` " +
                "UNION ";
        String groupsQuery = "MATCH (u:User)-[COMMUNIQUE]->(fg:FunctionalGroup)-[BELONGS]->(s:Structure)" +
                "<-[:DEPENDS]-(g:Group{name:\"" + CONTROL_GROUP + "\"}) " +
                "return distinct s.UAI as `" + STRUCTURE_UAI + "`, " +
                "u.id as `" + PERSON_ID + "`, " +
                "coalesce(split(fg.externalId,\"$\")[1], fg.id) as `" + GROUPS_CODE + "` ";
        neo4j.execute(classQuery + groupsQuery, new JsonObject(), validResultHandler(handler));
    }

    /**
     * Process groups content
     * @param groupPerson Array of group content from Neo4j
     */
    private void processGroupPersonInfo(JsonArray groupPerson) {
        processSimpleArray(groupPerson, GROUPS_PERSON_NODE);
    }

    /**
     * Get groups subjects from Neo4j
     * @param handler results
     */
    private void getGroupsSubjectsFromNeo4j(Handler<Either<String, JsonArray>> handler) {
        String query = "MATCH (u:User)-[t:TEACHES]->(sub:Subject)-[SUBJECT]->(s:Structure)" +
                "<-[:DEPENDS]-(g:Group{name:\"" + CONTROL_GROUP + "\"}) " +
                "with u.id as uid, t.groups + t.classes as grouplist, sub.code as code, s.UAI as uai " +
                "unwind(grouplist) as group ";
        String dataReturn = "return distinct uai as `" + STRUCTURE_UAI + "`, " +
                "uid as `" + PERSON_ID + "`, " +
                "split(group,\"$\")[1] as `" + GROUPS_CODE + "`, " +
                "coalesce(split(code,\"-\")[1], code) as `" + STUDYFIELD_CODE + "` " +
                "order by `" + PERSON_ID + "`, `" + STRUCTURE_UAI + "`";
        neo4j.execute(query + dataReturn, new JsonObject(), validResultHandler(handler));
    }

    /**
     * Process groups subjects
     * @param groupSubject Array of group subjects from Neo4j
     */
    private void processGroupSubjectsInfo(JsonArray groupSubject) {
        processSimpleArray(groupSubject, GROUPS_SUBJECT_NODE);
    }


}
