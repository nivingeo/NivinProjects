package custom.app.inventory;

import psdi.util.MXException;
import psdi.app.inventory.MatRecTransSet;
import psdi.mbo.MboServerInterface;
import java.rmi.RemoteException;
import psdi.mbo.MboSet;
import psdi.app.inventory.MatRecTrans;
import psdi.mbo.Mbo;
import psdi.util.logging.MXLogger;
import psdi.util.logging.MXLoggerFactory;

public class MatRecTransExtSet extends MatRecTransSet implements MatRecTransExtSetRemote {

	final private String APPLOGGER = "maximo.application.INVENTORY";
	private MXLogger log;

	public MatRecTransExtSet(MboServerInterface mboServerInterface0) throws MXException, RemoteException {
		super(mboServerInterface0);
		log = MXLoggerFactory.getLogger(APPLOGGER);
	}

	protected Mbo getMboInstance(MboSet mboSet0) throws MXException, RemoteException {
		log.debug("MatRecTransExtSet.getMboInstance");
		return new MatRecTransExt(mboSet0);
	}

}//class 