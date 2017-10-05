package org.thingml.tradfri;

import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.coap.MediaTypeRegistry;
import org.eclipse.californium.core.network.CoapEndpoint;
import org.eclipse.californium.core.network.config.NetworkConfig;
import org.eclipse.californium.scandium.DTLSConnector;
import org.eclipse.californium.scandium.config.DtlsConnectorConfig;
import org.eclipse.californium.scandium.dtls.pskstore.StaticPskStore;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


public class TradfriGateway implements Runnable {
    
        private Logger logger = Logger.getLogger(TradfriGateway.class.getName());
        
        public Logger getLogger() { return logger; }
    
        private ArrayList<TradfriGatewayListener> listners = new ArrayList<TradfriGatewayListener>();
	
	public void addTradfriGatewayListener(TradfriGatewayListener l) {
		listners.add(l);
	}
	public void removeTradfriGatewayListener(TradfriGatewayListener l) {
		listners.remove(l);
	}
	public void clearTradfriGatewayListener() {
		listners.clear();
	}

	protected String gateway_ip;
	protected String security_key;
	
	private CoapEndpoint coap = null;
	
	ArrayList<LightBulb> bulbs = new ArrayList<LightBulb>();
	
	public TradfriGateway(String gateway_ip, String security_key) {
		this.gateway_ip = gateway_ip;
		this.security_key = security_key;
	}
	
	protected void initCoap() {
		DtlsConnectorConfig.Builder builder = new DtlsConnectorConfig.Builder(); //new InetSocketAddress(0)
		builder.setPskStore(new StaticPskStore("", security_key.getBytes()));
		coap = new CoapEndpoint(new DTLSConnector(builder.build()), NetworkConfig.getStandard());
	}
	
	protected CoapResponse get(String path) {
                Logger.getLogger(TradfriGateway.class.getName()).log(Level.INFO, "GET: " + "coaps://" + gateway_ip + "/" + path);
		CoapClient client = new CoapClient("coaps://" + gateway_ip + "/" + path);
		client.setEndpoint(coap);
		CoapResponse response = client.get(1);
		if (response == null) {
			logger.log(Level.SEVERE, "Connection to Gateway timed out, please check ip address or increase the ACK_TIMEOUT in the Californium.properties file");
		}
		return response;
	}
        
        protected void set(String path, String payload) {
            Logger.getLogger(TradfriGateway.class.getName()).log(Level.INFO, "SET: " + "coaps://" + gateway_ip + "/" + path + " = " + payload);
            CoapClient client = new CoapClient("coaps://" + gateway_ip + "/" + path);
            client.setEndpoint(coap);
            CoapResponse response = client.put(payload, MediaTypeRegistry.TEXT_PLAIN);
            if (response != null && response.isSuccess()) {
                    //System.out.println("Yay");
            } else {
                    logger.log(Level.SEVERE, "Sending payload to " + "coaps://" + gateway_ip + "/" + path + " failed!");
            }
            client.shutdown();
	}
        
        public void setBulbOnOff(LightBulb b, boolean on) {
            
        }
        
	
	protected void dicoverBulbs() {
		bulbs.clear();
		try {
			CoapResponse response = get(TradfriConstants.DEVICES);
			JSONArray devices = new JSONArray(response.getResponseText());
                        for (TradfriGatewayListener l : listners) l.discoveryStarted(devices.length());
			for (int i = 0; i < devices.length(); i++) {
				response = get(TradfriConstants.DEVICES + "/" + devices.getInt(i));
				if (response != null) {
					JSONObject json = new JSONObject(response.getResponseText());
					if (json.has(TradfriConstants.TYPE) && json.getInt(TradfriConstants.TYPE) == TradfriConstants.TYPE_BULB) {
						LightBulb b = new LightBulb(json.getInt(TradfriConstants.INSTANCE_ID), this, response);
                                                bulbs.add(b);
                                                for (TradfriGatewayListener l : listners) l.foundLightBulb(b);
					}
				}
                                for (TradfriGatewayListener l : listners) l.dicoveryProgress(i+1, devices.length());
			}
                        for (TradfriGatewayListener l : listners) l.discoveryCompleted();
		} catch (JSONException e) {
			logger.log(Level.SEVERE,"Error parsing response from the Tradfri gateway", e);
		}
	}

    public void run() {
        Logger.getLogger(TradfriGateway.class.getName()).log(Level.INFO, "Tradfri Gateway is initalizing...");
        initCoap();
        Logger.getLogger(TradfriGateway.class.getName()).log(Level.INFO, "Discovering Devices...");
        dicoverBulbs();
        Logger.getLogger(TradfriGateway.class.getName()).log(Level.INFO, "Discovered " + bulbs.size() + " Bulbs.");
        while(true) {
            try {
                Thread.sleep(5000);
                Logger.getLogger(TradfriGateway.class.getName()).log(Level.INFO, "Polling bulbs status...");
                for (LightBulb b : bulbs) {
                        b.updateBulb();
                        //System.out.println(b.toString());
                }                
            } catch (InterruptedException ex) {
                Logger.getLogger(TradfriGateway.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
}
