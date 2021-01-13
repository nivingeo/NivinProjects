package custom.app.inventory;

import psdi.mbo.MboServerInterface;
import java.rmi.RemoteException;
import java.lang.String;
import psdi.mbo.MboSet;
import psdi.util.MXException;
import psdi.app.inventory.MatUseTransSet;
import psdi.mbo.Mbo;
import psdi.util.logging.MXLogger;
import psdi.util.logging.MXLoggerFactory;

public class MatUseTransExtSet extends MatUseTransSet implements MatUseTransExtSetRemote {

	final private String APPLOGGER = "maximo.application.INVENTORY";
	private MXLogger log;

	public MatUseTransExtSet(MboServerInterface mboServerInterface0) throws MXException, RemoteException {
		super(mboServerInterface0);
		log = MXLoggerFactory.getLogger(APPLOGGER);
	}


	protected Mbo getMboInstance(MboSet mboSet0) throws MXException, RemoteException {
		log.debug("MatUseTransExtSet.getMboInstance");
		return new MatUseTransExt(mboSet0);
	}


}//class 