package com.microfocus;

import com.hpe.adm.nga.sdk.Octane;
import com.hpe.adm.octane.ideplugins.services.connection.ConnectionSettings;
import com.hpe.adm.octane.ideplugins.services.connection.IdePluginsOctaneHttpClient;
import com.hpe.adm.octane.ideplugins.services.connection.granttoken.GrantTokenAuthentication;
import com.hpe.adm.octane.ideplugins.services.connection.granttoken.TokenPollingCompleteHandler;
import com.hpe.adm.octane.ideplugins.services.connection.granttoken.TokenPollingInProgressHandler;
import com.hpe.adm.octane.ideplugins.services.connection.granttoken.TokenPollingStartedHandler;
import com.hpe.adm.octane.ideplugins.services.util.ClientType;

public class OctaneService {
		
	public static Octane doLogin(ConnectionSettings connectionSettings, 
			TokenPollingStartedHandler tokenPollingStartedHandler, 
			TokenPollingInProgressHandler tokenPollingInProgressHandler, 
			TokenPollingCompleteHandler tokenPollingCompleteHandler) {

		TestIdePluginsOctaneHttpClient idePluginsOctaneHttpClient = new TestIdePluginsOctaneHttpClient(connectionSettings.getBaseUrl(), ClientType.OCTANE_IDE_PLUGIN);
		idePluginsOctaneHttpClient.setSsoTokenPollingCompleteHandler(tokenPollingCompleteHandler);
		idePluginsOctaneHttpClient.setSsoTokenPollingInProgressHandler(tokenPollingInProgressHandler);
		idePluginsOctaneHttpClient.setSsoTokenPollingStartedHandler(tokenPollingStartedHandler);
		
		return new Octane.Builder(new GrantTokenAuthentication(), idePluginsOctaneHttpClient)
				.Server(connectionSettings.getBaseUrl())
				.sharedSpace(connectionSettings.getSharedSpaceId())
				.workSpace(connectionSettings.getWorkspaceId())
				.build();
	}
	
}
