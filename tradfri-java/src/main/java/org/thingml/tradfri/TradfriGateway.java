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
    
        /**
         * Gateway properties and constructor
         */
        protected String gateway_ip;
	protected String security_key;
        protected int polling_rate = 5000;
    
        public TradfriGateway(String gateway_ip, String security_key) {
		this.gateway_ip = gateway_ip;
		this.security_key = security_key;
	}

        public String getGateway_ip() {
            return gateway_ip;
        }

        public void setGateway_ip(String gateway_ip) {
            this.gateway_ip = gateway_ip;
        }

        public String getSecurity_key() {
            return security_key;
        }

        public void setSecurity_key(String security_key) {
            this.security_key = security_key;
        }

        public int getPolling_rate() {
            return polling_rate;
        }

        public void setPolling_rate(int polling_rate) {
            // between 1 and 60 seconds
            if (polling_rate < 1000) polling_rate = 1000;
            else if (polling_rate > 60000) polling_rate = 60000;
            this.polling_rate = polling_rate;
        }
        
        private boolean running = false;
        
        public boolean isRunning() {
            return running;
        }
        
        /**
         * Gateway public API
         */
        public void startTradfriGateway() {
            if (running) return;
            running = true;
            new Thread(this).start();
        }
        
        public void stopTradfriGateway() {
            running = false;
        }
        
        /**
         * Logger to be used for all console outputs, errors and exceptions
         */
        private Logger logger = Logger.getLogger(TradfriGateway.class.getName());
        public Logger getLogger() { return logger; }
    
        /**
         * Observer pattern for asynchronous event notification
         */
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
        
        // Collection of bulbs registered on the gateway
	ArrayList<LightBulb> bulbs = new ArrayList<LightBulb>();
	
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
        try {
        while(running) {
                Thread.sleep(getPolling_rate());
                Logger.getLogger(TradfriGateway.class.getName()).log(Level.INFO, "Polling bulbs status...");
                for (LightBulb b : bulbs) {
                        b.updateBulb();
                        //System.out.println(b.toString());
                }                
            }
        } catch (InterruptedException ex) {
            Logger.getLogger(TradfriGateway.class.getName()).log(Level.SEVERE, null, ex);
        }
        running = false;
    }
    
        /**
         * COAPS helpers to GET and SET on the IKEA Tradfri gateway using Californium
         */
	private CoapEndpoint coap = null;
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
}
