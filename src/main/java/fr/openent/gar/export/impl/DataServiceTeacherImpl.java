package fr.openent.gar.export.impl;

import fr.openent.gar.helper.impl.PaginatorHelperImpl;
import fr.openent.gar.helper.impl.XmlExportHelperImpl;
import fr.openent.gar.export.DataService;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static fr.openent.gar.constants.GarConstants.*;

public class DataServiceTeacherImpl extends DataServiceBaseImpl implements DataService{
    private final PaginatorHelperImpl paginator;

    DataServiceTeacherImpl(JsonObject config, String strDate) {
        super(config);
        xmlExportHelper = new XmlExportHelperImpl(config, TEACHER_ROOT, TEACHER_FILE_PARAM, strDate);
        paginator = new PaginatorHelperImpl();
    }

    /**
     * Export Data to folder
     * - Export Teachers identities
     * - Export Teachers Mefs
     * @param handler response handler
     */
    @Override
    public void exportData(final Handler<Either<String, JsonObject>> handler) {
        getTeachersInfoFromNeo4j(resultTeachers -> {
            if (validResponseNeo4j(resultTeachers, handler)) {

                processTeachersInfo(resultTeachers.right().getValue());

                getTeachersMefFromNeo4j(
                        mefsResult -> {
                            if (mefsResult.isLeft()) {
                                handler.handle(new Either.Left<>(mefsResult.left().getValue()));
                            } else {
                                processTeachersMefs(mefsResult.right().getValue());
                                xmlExportHelper.closeFile();
                                handler.handle(new Either.Right<>(
                                        new JsonObject().put(
                                                FILE_LIST_KEY,
                                                xmlExportHelper.getFileList()
                                        )));
                            }
                        });
            } else {
                log.error("[DataServiceTeacherImpl@exportData] Failed to process");
            }
        });
    }

    /**
     * Get teachers infos from Neo4j
     * Set fields as requested by xsd, except for structures
     * @param handler results
     */
    private void getTeachersInfoFromNeo4j(Handler<Either<String, JsonArray>> handler) {
        String query = "MATCH (u:User)-[:IN|DEPENDS*1..2]->(pg:ProfileGroup)-[:DEPENDS]->(s:Structure), " +
                "(p:Profile)<-[:HAS_PROFILE]-(pg:ProfileGroup) WHERE HAS(s.exports) AND 'GAR' IN s.exports " +
                "AND NOT(HAS(u.deleteDate)) AND NOT(HAS(u.disappearanceDate)) AND p.name IN ['Personnel','Teacher'] " +
                // ADMINISTRATIVE ATTACHMENT can reference non GAR exported structure
                "OPTIONAL MATCH (u:User)-[:ADMINISTRATIVE_ATTACHMENT]->(sr:Structure)";
        String dataReturn = "return distinct u.id  as `" + PERSON_ID + "`, " +
                "u.lastName as `" + PERSON_PATRO_NAME + "`, " +
                "u.lastName as `" + PERSON_NAME + "`, " +
                "u.firstName as `" + PERSON_FIRST_NAME + "`, " +
                "coalesce(u.otherNames, [u.firstName]) as `" + PERSON_OTHER_NAMES + "`, " +
                //TODO GARPersonCivilite
                "collect(distinct sr.UAI)[0] as `" + PERSON_STRUCT_ATTACH + "`, " +
                "u.birthDate as `" + PERSON_BIRTH_DATE + "`, " +
                "u.functions as functions, " +
                "collect(distinct s.UAI+'$'+p.name) as UAIprofiles " +
                "order by " + "`" + PERSON_ID + "`";


        query = query + dataReturn;
        query += " ASC SKIP {skip} LIMIT {limit} ";

        JsonObject params = new JsonObject().put("limit", PaginatorHelperImpl.LIMIT);
        paginator.neoStreamList(query, params, new JsonArray(), 0, handler);
    }

    /**
     * Process teachers info
     * Add structures in arrays to match xsd
     * @param teachers Array of teachers from Neo4j
     */
    private void processTeachersInfo(JsonArray teachers) {
        try {
            for(Object o : teachers) {
                if(!(o instanceof JsonObject)) continue;

                JsonObject teacher = (JsonObject) o;
                JsonArray profiles = teacher.getJsonArray("UAIprofiles", null);
                if(profiles == null || profiles.size() == 0) {
                    log.error("Gar : Teacher with no profile or function for export, id "
                            + teacher.getString("u.id", "unknown"));
                    continue;
                }

                Map<String,String> userStructProfiles = new HashMap<>();

                processFunctions(teacher, userStructProfiles);
                processTeacherProfiles(teacher, userStructProfiles);

                if(isMandatoryFieldsAbsent(teacher, TEACHER_NODE_MANDATORY)) {
                    log.warn("Gar : mandatory attribut for Teacher : " + teacher);
                    continue;
                }

                reorganizeNodes(teacher);

                xmlExportHelper.saveObject(TEACHER_NODE, teacher);
            }
        } catch (Exception e) {
            log.error("Error when processing teachers Info : " + e.getMessage());
        }
    }

    /**
     * Process profiles, set profile from structMap for structures in it
     * Set default profile for other etabs
     * @param teacher person to process
     * @param structMap map for profile by structure
     */
    private void processTeacherProfiles(JsonObject teacher, Map<String, String> structMap) {
        JsonArray garProfiles = new fr.wseduc.webutils.collections.JsonArray();
        JsonArray garEtabs = new fr.wseduc.webutils.collections.JsonArray();

        JsonArray profilesUser = teacher.getJsonArray("UAIprofiles");
        String profileUser = TEACHER_PROFILE;

        for(Object profile : profilesUser) {
            if (!(profile instanceof String)) continue;
            String[] uaiProfile = ((String) profile).split("\\$");
            if (uaiProfile.length < 2) continue;
            String structure = uaiProfile[0];
            if (uaiProfile[1].equals("Personnel"))
                profileUser = PERSONNEL_ETAB_PROFILE;

            garEtabs.add(structure);

            if (structMap != null && structMap.containsKey(structure)) {
                addProfile(garProfiles, structure, structMap.get(structure));
                structMap.remove(structure);
            } else {
                addProfile(garProfiles, structure, profileUser);
            }
        }
        if (structMap != null) {
            for (String structUAI : structMap.keySet()) {
                garEtabs.add(structUAI);
                addProfile(garProfiles, structUAI, structMap.get(structUAI));
            }
        }
        teacher.put(PERSON_PROFILES, garProfiles);
        teacher.put(PERSON_STRUCTURE, garEtabs);
    }

    /**
     * XSD specify precise order for xml tags
     * @param teacher informations about the user
     */
    private void reorganizeNodes(JsonObject teacher) {
        JsonObject personCopy = teacher.copy();
        teacher.clear();
        teacher.put(PERSON_ID, personCopy.getValue(PERSON_ID));
        teacher.put(PERSON_PROFILES, personCopy.getValue(PERSON_PROFILES));
        teacher.put(PERSON_PATRO_NAME, personCopy.getValue(PERSON_PATRO_NAME));
        teacher.put(PERSON_NAME, personCopy.getValue(PERSON_NAME));
        teacher.put(PERSON_FIRST_NAME, personCopy.getValue(PERSON_FIRST_NAME));
        teacher.put(PERSON_OTHER_NAMES, personCopy.getValue(PERSON_OTHER_NAMES));
        //TODO GARPersonCivilite
        teacher.put(PERSON_STRUCT_ATTACH, personCopy.getValue(PERSON_STRUCT_ATTACH));
        teacher.put(PERSON_STRUCTURE, personCopy.getValue(PERSON_STRUCTURE));
        if(personCopy.getValue(PERSON_BIRTH_DATE) != null && !"".equals(personCopy.getValue(PERSON_BIRTH_DATE))){
            teacher.put(PERSON_BIRTH_DATE, personCopy.getValue(PERSON_BIRTH_DATE));
        }
        teacher.put(TEACHER_POSITION, personCopy.getValue(TEACHER_POSITION));
    }

    /**
     * Process teachers functions
     * Calc profile for Documentalist functions
     * Teacher function is in form structID$functionCode$functionDesc$roleCode and must be splited
     * and analyzed
     * Documentalists have specific role and profile
     * @param teacher to process functions for
     * @param structMap map between structures ID and profile
     */
    private void processFunctions(JsonObject teacher, Map<String,String> structMap) {
        JsonArray functions = teacher.getJsonArray("functions", null);
        if(functions == null || functions.size() == 0) {
            return;
        }
        JsonArray garFunctions = new fr.wseduc.webutils.collections.JsonArray();
        for(Object o : functions) {
            if(!(o instanceof String)) continue;
            String[] arrFunction = ((String)o).split("\\$");
            if(arrFunction.length < 4) continue;
            String structID = arrFunction[0];
            if(!mapStructures.containsKey(structID)) {
                continue;
            }
            String structUAI = mapStructures.get(structID);
            String functionCode = arrFunction[1];
            String roleCode = arrFunction[3];
            JsonArray profilesUser = teacher.getJsonArray("UAIprofiles");
            String profileUser = "Teacher";
            for(Object profile : profilesUser) {
                if(!(profile instanceof String)) continue;
                String[] uaiProfile = ((String)profile).split("\\$");
                if(uaiProfile.length < 2) continue;
                String uai = uaiProfile[0];
                if(structUAI.equals(uai))
                    profileUser = uaiProfile[1];
            }
            String profileType = TEACHER_PROFILE;
            if(profileUser.equals("Personnel"))
                profileType = PERSONNEL_ETAB_PROFILE;

            switch (functionCode) {
                case DIRECTOR_CODE :
                    profileType = DIRECTOR_PROFILE;
                    break;
                case COLLECTIVITE_TERRITORIALE_CODE :
                    profileType = COLLECTIVITE_TERRITORIALE_PROFILE;
                    break;
                case ENSEIGNANT_CODE :
                    profileType = TEACHER_PROFILE;
                    break;
            }

            List documentaliste_code = Arrays.asList(DOCUMENTALIST_CODE,CIO_CODE,DCT_CODE);

            List vie_scolaire_code = Arrays.asList(EDUCATION_CODE,EDUCATION_ASSISTANT_CODE,ACCOMPAGNEMENT_HANDICAP_2_CODE,
                    ACCOMPAGNMENT_HANDICAP_CODE,ETRANGER_ASSISTANT_CODE,SURVEILLANCE_CODE,CPE_CODE,AUXILIAIRE_VIE_SCOLAIRE_CODE,
                    BESOINS_EDUCATIFS_CODE, EDUCATEUR_INTERNAT_CODE, FORMATION_INSERTION_JEUNES_CODE, COORDINATION_INSERTION_JEUNES_CODE,
                    CONSEILLER_ORIENTATION_CODE);

            List personnel_etab_code = Arrays.asList(SANS_OBJET_CODE, NON_RENSEIGNE_CODE, PREMIER_DEGRE_CODE, PERSONNELS_SECOND_DEGRE_CODE,
                    ADMINISTRATION_CODE, PERSONNEL_ADMINISTRATIF_CODE, CHEF_DE_TRAVAUX_CODE, ACCOMPAGNANT_SOUTIEN_CODE,
                    PSYCHOLOGUE_CODE, OUVRIER_CODE, LABORATOIRE_CODE, PERSONNEL_MEDICO_SOCIAUX_CODE, PERSONNEL_TECHNIQUE_CODE,
                    READAPTATION_CODE, CONSEILLER_FORMATION_CONTINUE_CODE, APPRENTISSAGE_CODE, FORMATION_CONTINUE_ADULTES_CODE,
                    APPRENTI_CLASSIQUE_PROFESSEUR_CODE, PERSONNEL_ADMINISTRATIFS_DE_CENTRALE_CODE, PERSONNELS_ASU_DE_CENTRALE_CODE,
                    AUTRES_ADMINISTRATIONS_CODE, PERSONNELS_BIBLIOTHEQUES_MUSEES_CODE, COMITE_NATIONAL_EVALUATION_CODE,
                    COORDINATEUR_PEDAGOGIQUE_CFA_CODE, CONSEILLERS_PEDAGOGIQUES_EPS_CODE, PERSONNELS_CONTRACTUELS_DE_CENTRALE_CODE,
                    ELEVE_CYCLE_PRE_COP_STAGIAIRE_CODE, EMPLOIS_PARTICULIERS_ACTIONS_DIVERSES_CODE, COORDINATEUR_ORGANISATEUR_ANIMATION_MAFPEN_CODE,
                    INTERVENANTS_EXTERIEURS_CODE, PERSONNELS_INSPECTION_GENERALE_CODE, INSPECTION_CODE, PERSONNELS_INSPECTION_CENTRALE_CODE,
                    PERSONNELS_ITARF_CENTRALE_CODE, PERSONNELS_JEUNESSE_ENGAGEMENT_SPORT_CODE, LABORATOIRE_BIS_CODE, MISE_A_DISPOSITION_SANS_REMBOURSEMENT_CODE,
                    MISE_A_DISPOSITION_AVEC_REMBOURSEMENT_CODE, ENSEIGNANTS_CONGES_MOBILITE_CODE, MVT_PREMIER_DEGRE_CODE,
                    PILOTAGE_ANIMATION_PEDAGOGIE_CODE, PERSONNELS_ENSEIGNANTS_CENTRALE_CODE, REMPLACEMENT_CODE, RETRAITE_CODE,
                    REEMPLOI_AUPRES_CNED_CODE, STAGIAIRE_EN_FORMATION_CODE, _CODE);

            if(documentaliste_code.contains(functionCode)){
                profileType = DOCUMENTALIST_PROFILE;
            }else if(vie_scolaire_code.contains(functionCode)){
                profileType = VIE_SCOLAIRE_PROFILE;
            }else if(personnel_etab_code.contains(functionCode)){
                profileType = PERSONNEL_ETAB_PROFILE;
            }

            structMap.put(structUAI, profileType);

            JsonObject function = new JsonObject();
            function.put(STRUCTURE_UAI, structUAI);
            function.put(POSITION_CODE, roleCode);
            garFunctions.add(function);
        }
        teacher.put(TEACHER_POSITION, garFunctions);
        teacher.remove("functions");
    }

    /**
     * Get teachers mefs from Neo4j
     * @param handler results
     */
    private void getTeachersMefFromNeo4j(Handler<Either<String, JsonArray>> handler) {
        String query = "MATCH (u:User)-[:IN|DEPENDS*1..2]->(pg:ProfileGroup)-[:DEPENDS]->(s:Structure)," +
                "(p:Profile{name:'Teacher'})<-[:HAS_PROFILE]-(pg:ProfileGroup) ";
        query += "WHERE NOT(HAS(u.deleteDate)) AND NOT(HAS(u.disappearanceDate))" +
                "AND HAS(s.exports) AND 'GAR' IN s.exports " +
                "WITH s,u "+
                "UNWIND u.modules as module " +
                "WITH u, s, module " +
                "WHERE module <>\"\" AND s.externalId = split(module,'$')[0] " +
                "RETURN distinct " +
                "s.academy + '-' + split(module,'$')[0] as aca_structureID , " +
                "split(module,'$')[0] as structureID , " +
                "u.id as `" + PERSON_ID + "`, " +
                "split(module,'$')[1] as `" + MEF_CODE + "` " +
                "ORDER BY structureID, " + "`" + MEF_CODE + "` , `" + PERSON_ID + "`";

        query += " ASC SKIP {skip} LIMIT {limit} ";

        JsonObject params = new JsonObject().put("limit", PaginatorHelperImpl.LIMIT);
        paginator.neoStreamList(query, params, new JsonArray(), 0, handler);
    }

    /**
     * Process mefs info
     * @param mefs Array of mefs from Neo4j
     */
    private void processTeachersMefs(JsonArray mefs) {

        JsonArray filteredMEF = new fr.wseduc.webutils.collections.JsonArray();
        for(Object o : mefs) {
            if (!(o instanceof JsonObject)) continue;
            JsonObject mef = (JsonObject) o;
            String structID = mef.getString("structureID");
            String UAI = mapStructures.get(structID);
            if (UAI == null) {
                // MEF are not prefixed with academic name by feeder
                //so it's necessary to check in school map with academic prefix
                //before filter this MEF
                structID = mef.getString("aca_structureID");
                UAI = mapStructures.get(structID);
                if (UAI == null) continue;
            }

            JsonObject mefFiltered = new JsonObject();
            mefFiltered.put(STRUCTURE_UAI, UAI);
            mefFiltered.put(PERSON_ID, mef.getValue(PERSON_ID));
            mefFiltered.put(MEF_CODE, mef.getValue(MEF_CODE));
            filteredMEF.add(mefFiltered);
        }
        Either<String,JsonObject> event =  processSimpleArray(filteredMEF, PERSON_MEF, PERSON_MEF_NODE_MANDATORY);
        if(event.isLeft()) {
            log.error("Error when processing teacher mefs : " + event.left().getValue());
        }
    }
}
