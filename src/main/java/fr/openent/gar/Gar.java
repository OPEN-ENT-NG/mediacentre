package fr.openent.gar;

import fr.openent.gar.controller.GarController;
import fr.openent.gar.controller.SettingController;
import fr.openent.gar.export.ExportTask;
import fr.wseduc.cron.CronTrigger;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.core.eventbus.EventBus;
import org.entcore.common.http.BaseServer;

import java.text.ParseException;
import java.util.Arrays;

public class Gar extends BaseServer {

	public static final String GAR_ADDRESS = "openent.gar";
	public static boolean demo;
	public static JsonObject CONFIG;

	@Override
	public void start() throws Exception {
		super.start();
		final EventBus eb = getEventBus(vertx);

		addController(new GarController(vertx, config));
		addController(new SettingController(eb));

		final String exportCron = config.getString("export-cron", "");
		demo = config.getBoolean("demo", false);
		CONFIG = config;

		vertx.deployVerticle("fr.openent.gar.export.impl.ExportWorker", new DeploymentOptions().setConfig(config)
				.setIsolationGroup("mediacentre_worker_group")
				.setIsolatedClasses(Arrays.asList("fr.openent.mediacentre.export.impl.*",
						"fr.openent.mediacentre.helper.impl.*", "com.sun.org.apache.xalan.internal.xsltc.trax.*"))
				.setWorker(true));

		try{
			new CronTrigger(vertx, exportCron).schedule(new ExportTask(vertx.eventBus()));
		}catch (ParseException e) {
			log.fatal(e.getMessage(), e);
		}
	}

}
