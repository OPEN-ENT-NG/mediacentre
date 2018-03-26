package fr.openent.mediacentre;

import fr.openent.mediacentre.controller.MediacentreController;
import org.entcore.common.http.BaseServer;

public class Mediacentre extends BaseServer {

	@Override
	public void start() {
		super.start();
		addController(new MediacentreController());
	}

}
