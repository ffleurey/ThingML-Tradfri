/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thingml.tradfri;

import java.util.logging.Logger;

/**
 *
 * @author franck
 */
public interface TradfriGatewayListener {
       
    public void discoveryStarted(int total_devices);
    public void dicoveryProgress(int current_device, int total_devices);
    public void discoveryCompleted();
    
    public void foundLightBulb(LightBulb b);
}
