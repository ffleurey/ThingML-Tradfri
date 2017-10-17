package org.thingml.tradfri;

import java.util.ArrayList;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.californium.core.CoapResponse;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class LightBulb {
	
	private ArrayList<TradfriBulbListener> listners = new ArrayList<TradfriBulbListener>();
	
	public void addLightBulbListner(TradfriBulbListener l) {
		listners.add(l);
	}
	public void removeLightBulbListner(TradfriBulbListener l) {
		listners.remove(l);
	}
	public void clearLightBulbListners() {
		listners.clear();
	}
	
	private TradfriGateway gateway;

	// Immutable information
	private int id;
	private String name;

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public boolean isOnline() {
        return online;
    }

    public boolean isOn() {
        return on;
    }

    public void setOn(boolean on) {
        try {
                JSONObject json = new JSONObject();
                JSONObject settings = new JSONObject();
                JSONArray array = new JSONArray();
                array.put(settings);
                json.put(TradfriConstants.LIGHT, array);
                settings.put(TradfriConstants.ONOFF, (on)?1:0);
                String payload = json.toString();
		gateway.set(TradfriConstants.DEVICES + "/" + this.getId(), payload); 
                
            } catch (JSONException ex) {
                Logger.getLogger(TradfriGateway.class.getName()).log(Level.SEVERE, null, ex);
            }
        this.on = on;
    }

    public int getIntensity() {
        return intensity;
    }

    public void setIntensity(int intensity) {
        try {
                JSONObject json = new JSONObject();
                JSONObject settings = new JSONObject();
                JSONArray array = new JSONArray();
                array.put(settings);
                json.put(TradfriConstants.LIGHT, array);
                settings.put(TradfriConstants.DIMMER, intensity);
                String payload = json.toString();
		gateway.set(TradfriConstants.DEVICES + "/" + this.getId(), payload); 
                
            } catch (JSONException ex) {
                Logger.getLogger(TradfriGateway.class.getName()).log(Level.SEVERE, null, ex);
            }
        this.intensity = intensity;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        try {
                JSONObject json = new JSONObject();
                JSONObject settings = new JSONObject();
                JSONArray array = new JSONArray();
                array.put(settings);
                json.put(TradfriConstants.LIGHT, array);
                settings.put(TradfriConstants.COLOR, color);
                String payload = json.toString();
		gateway.set(TradfriConstants.DEVICES + "/" + this.getId(), payload); 
                
            } catch (JSONException ex) {
                Logger.getLogger(TradfriGateway.class.getName()).log(Level.SEVERE, null, ex);
            }
        this.color = color;
    }
	private String manufacturer;
        private String type;
        private String firmware;
	
	// Status
	private boolean online;
	
	// State of the bulb
	private boolean on;
	private int intensity;
	private String color;
        
        // Dates
        private Date dateInstalled;
        private Date dateLastSeen;

        public String getFirmware() {
            return firmware;
        }

        public Date getDateInstalled() {
            return dateInstalled;
        }

        public Date getDateLastSeen() {
            return dateLastSeen;
        }

        
	
	public LightBulb(int id, TradfriGateway gateway) {
		this.id = id;
		this.gateway = gateway;
	}
	
	public LightBulb(int id, TradfriGateway gateway, CoapResponse response) {
		this.id = id;
		this.gateway = gateway;
		if (response != null) parseResponse(response);
	}
	
	protected void updateBulb() {
		CoapResponse response = gateway.get(TradfriConstants.DEVICES + "/" + id);
		if (response != null) parseResponse(response);
	}
	
	protected void parseResponse(CoapResponse response) {
		boolean updateListeners = false;
                gateway.getLogger().log(Level.INFO, response.getResponseText());
		try {
			JSONObject json = new JSONObject(response.getResponseText());
			
                        String new_name = json.getString(TradfriConstants.NAME);
                        if (name == null || !name.equals(new_name)) updateListeners = true;
			name = new_name;
                        
                        dateInstalled = new Date(json.getLong(TradfriConstants.DATE_INSTALLED)*1000);
                        dateLastSeen = new Date(json.getLong(TradfriConstants.DATE_LAST_SEEN)*1000);
                        
                        boolean new_online = json.getInt(TradfriConstants.DEVICE_REACHABLE) != 0;
                        if (new_online != online) updateListeners = true;
                        online = new_online;
			
                        manufacturer = json.getJSONObject("3").getString("0");
			type = json.getJSONObject("3").getString("1");
                        firmware = json.getJSONObject("3").getString("3");
                        
			JSONObject light = json.getJSONArray(TradfriConstants.LIGHT).getJSONObject(0);
			
			if (light.has(TradfriConstants.ONOFF) && light.has(TradfriConstants.DIMMER)) {
				boolean new_on = (light.getInt(TradfriConstants.ONOFF) != 0);
				int new_intensity = light.getInt(TradfriConstants.DIMMER);
				if (on != new_on) updateListeners = true;
				if (intensity != new_intensity) updateListeners = true;
				on = new_on;
				intensity = new_intensity;
			}
			else {
				if (online) updateListeners = true;
				online = false;
			}
			if (light.has(TradfriConstants.COLOR)) {
				String new_color = light.getString(TradfriConstants.COLOR);
				if (color == null || !color.equals(new_color)) updateListeners = true;
				color = new_color;
			}
		} catch (JSONException e) {
			System.err.println("Cannot update bulb info: error parsing the response from the gateway.");
			e.printStackTrace();
		}
		if (updateListeners) {
			for (TradfriBulbListener l : listners) l.bulb_state_changed(this);
		}
	}
	
	public String toString() {
		String result = "[BULB " + id + "]";
		if (online) result += "\ton:" + on + "\tdim:" + intensity + "\tcolor:" + color;
		else result += "  ********** OFFLINE *********** ";
		result += "\ttype: " + type + "\tname: " + name;
		return result;
	}
	
}
