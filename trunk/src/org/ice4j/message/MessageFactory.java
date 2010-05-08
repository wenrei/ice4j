/*
 * ice4j, the OpenSource Java Solution for NAT and Firewall Traversal.
 * Maintained by the SIP Communicator community (http://sip-communicator.org).
 *
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package org.ice4j.message;

import java.util.logging.*;

import org.ice4j.*;
import org.ice4j.attribute.*;

/**
 * This class provides factory methods to allow an application to create STUN
 * Messages from a particular implementation.
 *
 * @author Emil Ivov
 * @author Sebastien Vincent
 */
public class MessageFactory
{
    private static final Logger logger = Logger
                                                       .getLogger(MessageFactory.class
                                                                       .getName());

    /**
     * Creates a default binding request. The request DOES NOT contains a
     * ChangeRequest attribute with zero change ip and change port flags.
     *
     * @return a default binding request.
     */
    public static Request createBindingRequest()
    {
        Request bindingRequest = new Request();
        try
        {
            bindingRequest.setMessageType(Message.BINDING_REQUEST);
        } catch (IllegalArgumentException ex)
        {
            // there should be no exc here since we're the creators.
            logger.log(Level.FINE, "Failed to set message type.", ex);
        }

        /* do not add this by default */
        /*
         * //add a change request attribute ChangeRequestAttribute attribute =
         * AttributeFactory.createChangeRequestAttribute();
         *
         * try { bindingRequest.addAttribute(attribute); } catch (StunException
         * ex) { //shouldn't happen throw new
         * RuntimeException("Failed to add a change request "
         * +"attribute to a binding request!"); }
         */
        return bindingRequest;
    }

    /**
     * Creates a default binding request. The request contains a ChangeReqeust
     * attribute with zero change ip and change port flags. It also contains the
     * PRIORITY attribute used for ICE processing
     *
     * @param priority the value for the priority attribute
     * @return a BindingRequest header with ICE PRIORITY attribute
     * @throws StunException if we have a problem creating the request
     */
    public static Request createBindingRequest(long priority)
                    throws StunException
    {
        Request bindingRequest = createBindingRequest();

        PriorityAttribute attribute = AttributeFactory
                        .createPriorityAttribute(priority);
        bindingRequest.addAttribute(attribute);

        return bindingRequest;
    }

    /**
     * Creates a default binding request. The request contains a ChangeReqeust
     * attribute with zero change ip and change port flags. It contains the
     * PRIORITY, ICE-CONTROLLED or ICE-CONTROLLING attributes used for ICE
     * processing
     *
     * @param priority the value of the ICE priority attributes
     * @param controlling the value of the controlling attribute
     * @param tieBreaker the value of the ICE tie breaker attribute
     * @return a BindingRequest header with some ICE attributes (PRIORITY,
     * ICE-CONTROLLING / ICE-CONTROLLED)
     * @throws StunException if we have a problem creating the request
     */
    public static Request createBindingRequest(long priority,
                    boolean controlling, long tieBreaker)
                    throws StunException
    {
        Request bindingRequest = createBindingRequest();

        PriorityAttribute attribute = AttributeFactory
                        .createPriorityAttribute(priority);
        bindingRequest.addAttribute(attribute);

        if (controlling)
        {
            IceControllingAttribute iceControllingAttribute = AttributeFactory
                            .createIceControllingAttribute(tieBreaker);
            bindingRequest.addAttribute(iceControllingAttribute);
        } else
        {
            IceControlledAttribute iceControlledAttribute = AttributeFactory
                            .createIceControlledAttribute(tieBreaker);
            bindingRequest.addAttribute(iceControlledAttribute);
        }

        return bindingRequest;
    }

    /**
     * Creates a BindingResponse in a 3489 compliant manner, assigning the
     * specified values to mandatory headers.
     *
     * @param mappedAddress the address to assign the mappedAddressAttribute
     * @param sourceAddress the address to assign the sourceAddressAttribute
     * @param changedAddress the address to assign the changedAddressAttribute
     * @return a BindingResponse assigning the specified values to mandatory
     * headers.
     * @throws IllegalArgumentException if there was something wrong with the
     * way we are trying to create the response.
     */
    public static Response create3482BindingResponse(
                    TransportAddress mappedAddress,
                    TransportAddress sourceAddress,
                    TransportAddress changedAddress)
                    throws IllegalArgumentException
    {
        Response bindingResponse = new Response();
        bindingResponse.setMessageType(Message.BINDING_RESPONSE);

        // mapped address
        MappedAddressAttribute mappedAddressAttribute = AttributeFactory
                        .createMappedAddressAttribute(mappedAddress);

        // the changed address and source address attribute were removed in
        // RFC 5389 so we should be prepared to go without them.

        // source address
        SourceAddressAttribute sourceAddressAttribute = null;

        if (sourceAddress != null)
            sourceAddressAttribute = AttributeFactory
                            .createSourceAddressAttribute(sourceAddress);

        // changed address
        ChangedAddressAttribute changedAddressAttribute = null;

        if (changedAddress != null)
            changedAddressAttribute = AttributeFactory
                            .createChangedAddressAttribute(changedAddress);

        bindingResponse.addAttribute(mappedAddressAttribute);

        // the changed address and source address attribute were removed in
        // RFC 5389 so we should be prepared to go without them.

        if (sourceAddressAttribute != null)
            bindingResponse.addAttribute(sourceAddressAttribute);

        if (changedAddressAttribute != null)
            bindingResponse.addAttribute(changedAddressAttribute);

        return bindingResponse;
    }

    /**
     * Creates a BindingResponse in a 5389 compliant manner containing a single
     * <tt>XOR-MAPPED-ADDRESS</tt> attribute
     *
     * @param request the request that created the transaction that this
     * response will belong to.
     * @param mappedAddress the address to assign the mappedAddressAttribute
     * @return a BindingResponse assigning the specified values to mandatory
     * headers.
     * @throws IllegalArgumentException if there was something wrong with the
     * way we are trying to create the response.
     */
    public static Response createBindingResponse(Request request,
                    TransportAddress mappedAddress)
                    throws IllegalArgumentException
    {
        Response bindingResponse = new Response();
        bindingResponse.setMessageType(Message.BINDING_RESPONSE);

        // xor mapped address
        XorMappedAddressAttribute xorMappedAddressAttribute = AttributeFactory
                        .createXorMappedAddressAttribute(mappedAddress,
                                        request.getTransactionID());

        bindingResponse.addAttribute(xorMappedAddressAttribute);

        return bindingResponse;
    }

    /**
     * Creates a binding error response according to the specified error code
     * and unknown attributes.
     *
     * @param errorCode the error code to encapsulate in this message
     * @param reasonPhrase a human readable description of the error
     * @param unknownAttributes a char[] array containing the ids of one or more
     * attributes that had not been recognized.
     * @throws IllegalArgumentException INVALID_ARGUMENTS if one or more of the
     * given parameters had an invalid value.
     *
     * @return a binding error response message containing an error code and a
     * UNKNOWN-ATTRIBUTES header
     */
    public static Response createBindingErrorResponse(char errorCode,
                    String reasonPhrase, char[] unknownAttributes)
        throws IllegalArgumentException
    {
        Response bindingErrorResponse = new Response();
        bindingErrorResponse.setMessageType(Message.BINDING_ERROR_RESPONSE);

        // init attributes
        UnknownAttributesAttribute unknownAttributesAttribute = null;
        ErrorCodeAttribute errorCodeAttribute = AttributeFactory
                        .createErrorCodeAttribute(errorCode,
                                        reasonPhrase);

        bindingErrorResponse.addAttribute(errorCodeAttribute);

        if (unknownAttributes != null)
        {
            unknownAttributesAttribute = AttributeFactory
                            .createUnknownAttributesAttribute();
            for (int i = 0; i < unknownAttributes.length; i++)
            {
                unknownAttributesAttribute
                                .addAttributeID(unknownAttributes[i]);
            }
            bindingErrorResponse
                            .addAttribute(unknownAttributesAttribute);
        }

        return bindingErrorResponse;
    }

    /**
     * Creates a binding error response with UNKNOWN_ATTRIBUTES error code and
     * the specified unknown attributes.
     *
     * @param unknownAttributes a char[] array containing the ids of one or more
     * attributes that had not been recognized.
     * @throws StunException INVALID_ARGUMENTS if one or more of the given
     * parameters had an invalid value.
     * @return a binding error response message containing an error code and a
     * UNKNOWN-ATTRIBUTES header
     */
    public static Response createBindingErrorResponseUnknownAttributes(
                    char[] unknownAttributes) throws StunException
    {
        return createBindingErrorResponse(
                        ErrorCodeAttribute.UNKNOWN_ATTRIBUTE, null,
                        unknownAttributes);
    }

    /**
     * Creates a binding error response with UNKNOWN_ATTRIBUTES error code and
     * the specified unknown attributes and reason phrase.
     *
     * @param reasonPhrase a short description of the error.
     * @param unknownAttributes a char[] array containing the ids of one or more
     * attributes that had not been recognized.
     * @throws StunException INVALID_ARGUMENTS if one or more of the given
     * parameters had an invalid value.
     * @return a binding error response message containing an error code and a
     * UNKNOWN-ATTRIBUTES header
     */
    public static Response createBindingErrorResponseUnknownAttributes(
                    String reasonPhrase, char[] unknownAttributes)
                    throws StunException
    {
        return createBindingErrorResponse(
                        ErrorCodeAttribute.UNKNOWN_ATTRIBUTE,
                        reasonPhrase, unknownAttributes);
    }

    /**
     * Creates a binding error response with an ERROR-CODE attribute.
     *
     * @param errorCode the error code to encapsulate in this message
     * @param reasonPhrase a human readable description of the error.
     *
     * @return a binding error response message containing an error code and a
     * UNKNOWN-ATTRIBUTES header
     */
    public static Response createBindingErrorResponse(char errorCode,
                    String reasonPhrase)
    {
        return createBindingErrorResponse(errorCode, reasonPhrase, null);
    }

    /**
     * Creates a binding error response according to the specified error code.
     *
     * @param errorCode the error code to encapsulate in this message attributes
     * that had not been recognized.
     *
     * @return a binding error response message containing an error code and a
     * UNKNOWN-ATTRIBUTES header
     */
    public static Response createBindingErrorResponse(char errorCode)
    {
        return createBindingErrorResponse(errorCode, null, null);
    }

    /**
     * Create an allocate request without attribute.
     *
     * @return an allocate request
     */
    public static Request createAllocateRequest()
    {
        Request allocateRequest = new Request();

        try
        {
            allocateRequest.setMessageType(Message.ALLOCATE_REQUEST);
        } catch (IllegalArgumentException ex)
        {
            // there should be no exc here since we're the creators.
            logger.log(Level.FINE, "Failed to set message type.", ex);
        }
        return allocateRequest;
    }

    /**
     * Create an allocate request to allocate an even port. Attention this does
     * not have attributes for long-term authentication.
     *
     * @param protocol requested protocol number
     * @param rFlag R flag for the EVEN-PORT
     * @return an allocation request
     */
    public static Request createAllocateRequest(byte protocol,
                    boolean rFlag)
    {
        Request allocateRequest = new Request();

        try
        {
            allocateRequest.setMessageType(Message.ALLOCATE_REQUEST);

            /* XXX add enum somewhere for transport number */
            if (protocol != 6 && protocol != 17)
            {
                throw new StunException("Protocol not valid!");
            }

            /* add a REQUESTED-TRANSPORT attribute */
            RequestedTransportAttribute reqTransport = AttributeFactory
                            .createRequestedTransportAttribute(protocol);
            allocateRequest.addAttribute(reqTransport);

            /* add EVEN-PORT attribute */
            EvenPortAttribute reqProps = AttributeFactory
                            .createEvenPortAttribute(rFlag);
            allocateRequest.addAttribute(reqProps);
        } catch (StunException ex)
        {
            logger.log(Level.FINE, "Failed to set message type.", ex);
        }

        return allocateRequest;
    }

    /**
     * Add the required attributes for long-term authentication.<br/>
     * This will also add a MESSAGE-INTEGRITY and FINGERPRINT attribute.<br/>
     * Do not add another attributes after called this method.
     *
     * @param request the original request
     * @param username username value
     * @param realm realm value
     * @param nonce nonce value
     *
     * @throws StunException in case we have a problem creating the username
     * realm or nonce attributes.
     */
    public static void addLongTermAuthentifcationAttribute(
                    Request request, byte username[], byte realm[],
                    byte nonce[]) throws StunException
    {
        UsernameAttribute usernameAttr = AttributeFactory
                        .createUsernameAttribute(username);
        RealmAttribute realmAttr = AttributeFactory
                        .createRealmAttribute(realm);
        NonceAttribute nonceAttr = AttributeFactory
                        .createNonceAttribute(nonce);

        request.addAttribute(usernameAttr);
        request.addAttribute(realmAttr);
        request.addAttribute(nonceAttr);

        /* TODO calculate MESSAGE-INTEGRITY and FINGERPRINT */
    }

    /**
     * Create a refresh request.
     *
     * @param lifetime lifetime value
     * @return refresh request
     */
    public static Request createRefreshRequest(int lifetime)
    {
        Request refreshRequest = new Request();

        try
        {
            refreshRequest.setMessageType(Message.REFRESH_REQUEST);

            /* add a LIFETIME attribute */
            LifetimeAttribute lifetimeReq = AttributeFactory
                            .createLifetimeAttribute(lifetime);
            refreshRequest.addAttribute(lifetimeReq);
        } catch (IllegalArgumentException ex)
        {
            logger.log(Level.FINE, "Failed to set message type.", ex);
        }

        return refreshRequest;
    }

    /**
     * Create a ChannelBind request.
     *
     * @param channelNumber the channel number
     * @param peerAddress the peer address
     * @param tranID the ID of the transaction that we should be using
     *
     * @return channel bind request
     */
    public static Request createChannelBindRequest(char channelNumber,
                    TransportAddress peerAddress, byte[] tranID)
    {
        Request channelBindRequest = new Request();

        try
        {
            channelBindRequest
                            .setMessageType(Message.CHANNELBIND_REQUEST);

            // add a CHANNEL-NUMBER attribute
            ChannelNumberAttribute channelNumberAttribute = AttributeFactory
                            .createChannelNumberAttribute(channelNumber);
            channelBindRequest.addAttribute(channelNumberAttribute);

            // add a XOR-PEER-ADDRESS
            XorPeerAddressAttribute peerAddressAttribute = AttributeFactory
                            .createXorPeerAddressAttribute(peerAddress,
                                            tranID);

            channelBindRequest.addAttribute(peerAddressAttribute);
        } catch (IllegalArgumentException ex)
        {
            logger.log(Level.FINE, "Failed to set message type.", ex);
        }

        return channelBindRequest;
    }

    /**
     * Create a Send Indication.
     *
     * @param peerAddress peer address
     * @param data data (could be 0 byte)
     * @param tranID the ID of the transaction that we should be using
     *
     * @return send indication message
     */
    public static Indication createSendIndication(
                    TransportAddress peerAddress, byte[] data, byte[] tranID)
    {
        Indication sendIndication = new Indication();

        try
        {
            sendIndication.setMessageType(Message.SEND_INDICATION);

            /* add XOR-PEER-ADDRESS attribute */
            XorPeerAddressAttribute peerAddressAttribute = AttributeFactory
                            .createXorPeerAddressAttribute(peerAddress, tranID);
            sendIndication.addAttribute(peerAddressAttribute);

            /* add DATA if data */
            if (data != null && data.length > 0)
            {
                DataAttribute dataAttribute = AttributeFactory
                                .createDataAttribute(data);
                sendIndication.addAttribute(dataAttribute);
            }
        } catch (IllegalArgumentException ex)
        {
            logger.log(Level.FINE, "Failed to set message type.", ex);
        }

        return sendIndication;
    }

    // ======================== NOT CURRENTLY SUPPORTED
    public static Request createShareSecretRequest()
    {
        throw new UnsupportedOperationException(
                        "Shared Secret Support is not currently implemented");
    }

    public static Response createSharedSecretResponse()
    {
        throw new UnsupportedOperationException(
                        "Shared Secret Support is not currently implemented");
    }

    public static Response createSharedSecretErrorResponse()
    {
        throw new UnsupportedOperationException(
                        "Shared Secret Support is not currently implemented");
    }
}
