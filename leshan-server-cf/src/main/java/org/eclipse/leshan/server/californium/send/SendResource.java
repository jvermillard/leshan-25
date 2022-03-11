/*******************************************************************************
 * Copyright (c) 2021 Sierra Wireless and others.
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
package org.eclipse.leshan.server.californium.send;

import static org.eclipse.leshan.core.californium.ResponseCodeUtil.toCoapResponseCode;

import java.util.List;
import java.util.Map;

import org.eclipse.californium.core.coap.CoAP.ResponseCode;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.server.resources.CoapExchange;
import org.eclipse.leshan.core.californium.LwM2mCoapResource;
import org.eclipse.leshan.core.node.LwM2mNode;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.node.codec.CodecException;
import org.eclipse.leshan.core.node.codec.LwM2mDecoder;
import org.eclipse.leshan.core.request.ContentFormat;
import org.eclipse.leshan.core.request.Identity;
import org.eclipse.leshan.core.request.SendRequest;
import org.eclipse.leshan.core.request.exception.InvalidRequestException;
import org.eclipse.leshan.core.response.SendResponse;
import org.eclipse.leshan.core.response.SendableResponse;
import org.eclipse.leshan.server.endpoint.LwM2mRequestReceiver;
import org.eclipse.leshan.server.endpoint.PeerProfile;
import org.eclipse.leshan.server.endpoint.PeerProfileProvider;

/**
 * A CoAP Resource used to handle "Send" request sent by LWM2M devices.
 * 
 * @see SendRequest
 */
public class SendResource extends LwM2mCoapResource {
    private LwM2mDecoder decoder;
    private LwM2mRequestReceiver receiver;
    private PeerProfileProvider profileProvider;

    public SendResource(LwM2mRequestReceiver receiver, LwM2mDecoder decoder, PeerProfileProvider profileProvider) {
        super("dp");
        this.decoder = decoder;
        this.receiver = receiver;
        this.profileProvider = profileProvider;
    }

    @Override
    public void handlePOST(CoapExchange exchange) {
        Request coapRequest = exchange.advanced().getRequest();
        Identity sender = extractIdentity(coapRequest.getSourceContext());
        PeerProfile clientProfile = profileProvider.getProfile(sender);

        // check we have a registration for this identity
        if (clientProfile == null) {
            exchange.respond(ResponseCode.NOT_FOUND, "no registration found");
            return;
        }

        try {
            // Decode payload
            byte[] payload = exchange.getRequestPayload();
            ContentFormat contentFormat = ContentFormat.fromCode(exchange.getRequestOptions().getContentFormat());
            if (!decoder.isSupported(contentFormat)) {
                exchange.respond(ResponseCode.BAD_REQUEST, "Unsupported content format");
                receiver.onError(sender, clientProfile,
                        new InvalidRequestException("Unsupported content format [%s] in [%s] from [%s]", contentFormat,
                                coapRequest, sender),
                        SendRequest.class, coapRequest.getLocalAddress());
                return;
            }
            Map<LwM2mPath, LwM2mNode> data = null;

            data = decoder.decodeNodes(payload, contentFormat, (List<LwM2mPath>) null, clientProfile.getModel());

            // Handle "send op request
            SendRequest sendRequest = new SendRequest(contentFormat, data, coapRequest);
            SendableResponse<SendResponse> sendableResponse = receiver.requestReceived(sender, clientProfile,
                    sendRequest, coapRequest.getLocalAddress());
            SendResponse response = sendableResponse.getResponse();

            // send reponse
            if (response.isSuccess()) {
                exchange.respond(toCoapResponseCode(response.getCode()));
                sendableResponse.sent();
                return;
            } else {
                exchange.respond(toCoapResponseCode(response.getCode()), response.getErrorMessage());
                sendableResponse.sent();
                return;
            }
        } catch (CodecException e) {
            exchange.respond(ResponseCode.BAD_REQUEST, "Invalid Payload");
            receiver.onError(sender, clientProfile,
                    new InvalidRequestException(e, "Invalid payload in [%s] from [%s]", coapRequest, sender),
                    SendRequest.class, coapRequest.getLocalAddress());
            return;
        } catch (RuntimeException e) {
            receiver.onError(sender, clientProfile, e, SendRequest.class, coapRequest.getLocalAddress());
            throw e;
        }
    }
}
