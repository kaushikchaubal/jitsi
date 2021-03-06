/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.muc;

import java.util.*;
import java.util.regex.*;

import net.java.sip.communicator.service.contactsource.*;
import net.java.sip.communicator.service.muc.*;
import net.java.sip.communicator.service.protocol.*;

/**
 * The <tt>ServerChatRoomQuery</tt> is a query over the
 * <tt>ServerChatRoomContactSourceService</tt>.
 * 
 * @author Hristo Terezov
 */
public class ServerChatRoomQuery
    extends AsyncContactQuery<ContactSourceService>
    implements ChatRoomProviderWrapperListener
{
    /**
     * The query string.
     */
    private String queryString;

    /**
     * List with the current results for the query.
     */
    private List<BaseChatRoomSourceContact> contactResults
        = new ArrayList<BaseChatRoomSourceContact>();
    
    /**
     * MUC service.
     */
    private MUCServiceImpl mucService;

    /**
     * The number of contact query listeners.
     */
    private int contactQueryListenersCount = 0;
    
    /**
     * The provider associated with the query.
     */
    private ChatRoomProviderWrapper provider = null;

    /**
     * Creates an instance of <tt>ChatRoomQuery</tt> by specifying
     * the parent contact source, the query string to match and the maximum
     * result contacts to return.
     *
     * @param contactSource the parent contact source
     * @param queryString the query string to match
     * @param provider the provider associated with the query
     * @param count the maximum result contact count
     */
    public ServerChatRoomQuery(String queryString, 
        ServerChatRoomContactSourceService contactSource, 
        ChatRoomProviderWrapper provider)
    {
        super(contactSource,
            Pattern.compile(queryString, Pattern.CASE_INSENSITIVE
                            | Pattern.LITERAL), true);
        this.queryString = queryString;
        
        mucService = MUCActivator.getMUCService();
        
        this.provider = provider;
    }
    
    /**
     * Adds listeners for the query
     */
    private void initListeners()
    {
        mucService.addChatRoomProviderWrapperListener(this);
    }
    
    @Override
    protected void run()
    {
        if(provider == null)
        {
            Iterator<ChatRoomProviderWrapper> chatRoomProviders
                = mucService.getChatRoomProviders();
            while (chatRoomProviders.hasNext())
            {
                ChatRoomProviderWrapper provider = chatRoomProviders.next();
                providerAdded(provider, true);
            }
        }
        else
        {
            providerAdded(provider, true);
        }
        
        if (getStatus() != QUERY_CANCELED)
            setStatus(QUERY_COMPLETED);
    }
    
    /**
     * Handles adding a chat room provider.
     * @param provider the provider.
     * @param addQueryResult indicates whether we should add the chat room to 
     * the query results or fire an event without adding it to the results. 
     */
    private void providerAdded(ChatRoomProviderWrapper provider, 
        boolean addQueryResult)
    {
        List<String> chatRoomNames 
            = MUCActivator.getMUCService().getExistingChatRooms(provider);
        if(chatRoomNames == null)
            return;
        for(String chatRoomName : chatRoomNames)
        {
            addChatRoom( provider.getProtocolProvider(), chatRoomName,
                chatRoomName, addQueryResult);
        }
    }
    
    
    /**
     * Adds found result to the query results.
     * 
     * @param pps the protocol provider associated with the found chat room.
     * @param chatRoomName the name of the chat room.
     * @param chatRoomID the id of the chat room.
     * @param addQueryResult indicates whether we should add the chat room to 
     * the query results or fire an event without adding it to the results.
     * @param isAutoJoin the auto join state of the contact.
     */
    private void addChatRoom(ProtocolProviderService pps, 
        String chatRoomName, String chatRoomID, boolean addQueryResult)
    {
        if((queryString == null
            || ((chatRoomName.contains(
                            queryString)
                    || chatRoomID.contains(queryString)
                    ))) && isMatching(chatRoomID, pps))
        {
            BaseChatRoomSourceContact contact 
                = new BaseChatRoomSourceContact(chatRoomName, chatRoomID, this, 
                    pps);
            synchronized (contactResults)
            {
                contactResults.add(contact);
            }
            
            if(addQueryResult)
            {
                addQueryResult(contact, false);
            }
            else
            {
                fireContactReceived(contact, false);
            }
        }
    }

    @Override
    public void chatRoomProviderWrapperAdded(ChatRoomProviderWrapper provider)
    {
        providerAdded(provider, false);
    }

    @Override
    public void chatRoomProviderWrapperRemoved(ChatRoomProviderWrapper provider)
    {
        LinkedList<BaseChatRoomSourceContact> tmpContactResults;
        synchronized (contactResults)
        {
            tmpContactResults 
                = new LinkedList<BaseChatRoomSourceContact>(contactResults);
        
            for(BaseChatRoomSourceContact contact : tmpContactResults)
            {
                if(contact.getProvider().equals(provider.getProtocolProvider()))
                {
                    contactResults.remove(contact);
                    fireContactRemoved(contact);
                }
            }
        }
    }

    
    /**
     * Clears any listener we used.
     */
    private void clearListeners()
    {
        mucService.removeChatRoomProviderWrapperListener(this);
    }
    
    /**
     * Cancels this <tt>ContactQuery</tt>.
     *
     * @see ContactQuery#cancel()
     */
    public void cancel()
    {
        clearListeners();

        super.cancel();
    }
    
    /**
     * If query has status changed to cancel, let's clear listeners.
     * @param status {@link ContactQuery#QUERY_CANCELED},
     * {@link ContactQuery#QUERY_COMPLETED}
     */
    public void setStatus(int status)
    {
        if(status == QUERY_CANCELED)
            clearListeners();

        super.setStatus(status);
    }
    
    @Override
    public void addContactQueryListener(ContactQueryListener l)
    {
        super.addContactQueryListener(l);
        contactQueryListenersCount++;
        if(contactQueryListenersCount == 1)
        {
            initListeners();
        }
    }
    
    @Override
    public void removeContactQueryListener(ContactQueryListener l)
    {
        super.removeContactQueryListener(l);
        contactQueryListenersCount--;
        if(contactQueryListenersCount == 0)
        {
            clearListeners();
        }
    }
    
    /**
     * Checks if the contact should be added to results or not.
     * @param chatRoomID the chat room id associated with the contact.
     * @param pps the provider of the chat room contact.
     * @return <tt>true</tt> if the result should be added to the results and 
     * <tt>false</tt> if not.
     */
    public boolean isMatching(String chatRoomID, ProtocolProviderService pps)
    {
        return (MUCActivator.getMUCService().findChatRoomWrapperFromChatRoomID(
            chatRoomID, pps) == null);
    }
}