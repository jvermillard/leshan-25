/*******************************************************************************
 * Copyright (c) 2022 Sierra Wireless and others.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * and Eclipse Distribution License v1.0 which accompany this distribution.
 * 
 * The Eclipse Public License is available at
 *    http://www.eclipse.org/legal/epl-v20.html
 * and the Eclipse Distribution License is available at
 *    http://www.eclipse.org/org/documents/edl-v10.html.
 * 
 * Contributors:
 *     Sierra Wireless - initial API and implementation
 *******************************************************************************/
package org.eclipse.leshan.server.californium.endpoint.coaps;

import java.util.Arrays;
import java.util.List;

import org.eclipse.californium.core.config.CoapConfig;
import org.eclipse.californium.core.config.CoapConfig.TrackerMode;
import org.eclipse.californium.elements.DtlsEndpointContext;
import org.eclipse.californium.elements.config.Configuration;
import org.eclipse.californium.elements.config.Configuration.ModuleDefinitionsProvider;
import org.eclipse.californium.elements.config.SystemConfig;
import org.eclipse.californium.elements.config.UdpConfig;
import org.eclipse.californium.scandium.config.DtlsConfig;
import org.eclipse.californium.scandium.config.DtlsConfig.DtlsRole;
import org.eclipse.leshan.server.californium.endpoint.CaliforniumEndpointFactory;
import org.eclipse.leshan.server.californium.endpoint.CaliforniumProtocolProvider;
import org.eclipse.leshan.server.endpoint.Protocol;

public class CoapsProtocolProvider implements CaliforniumProtocolProvider {

    @Override
    public Protocol getProtocol() {
        return Protocol.COAPS;
    }

    @Override
    public void applyDefaultValue(Configuration configuration) {
        configuration.set(CoapConfig.MID_TRACKER, TrackerMode.NULL);
        // Do no allow Server to initiated Handshake by default, for U device request will be allowed to initiate
        // handshake (see Registration.shouldInitiateConnection())
        configuration.set(DtlsConfig.DTLS_DEFAULT_HANDSHAKE_MODE, DtlsEndpointContext.HANDSHAKE_MODE_NONE);
        configuration.set(DtlsConfig.DTLS_ROLE, DtlsRole.BOTH);
    }

    @Override
    public List<ModuleDefinitionsProvider> getModuleDefinitionsProvider() {
        return Arrays.asList(SystemConfig.DEFINITIONS, CoapConfig.DEFINITIONS, UdpConfig.DEFINITIONS,
                DtlsConfig.DEFINITIONS);
    }

    @Override
    public CaliforniumEndpointFactory getDefaultEndpointFactory() {
        return new CoapsEndpointFactory();
    }
}
