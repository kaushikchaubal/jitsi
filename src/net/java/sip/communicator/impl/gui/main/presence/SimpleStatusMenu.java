/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.presence;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.customcontrols.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

/**
 * The <tt>SimpleStatusSelectorBox</tt> is a <tt>SIPCommMenu</tt> that contains
 * two statuses ONLINE and OFFLINE. It's used to represent the status of a
 * protocol provider which doesn't support presence operation set.
 * 
 * @author Yana Stamcheva
 * @author Lubomir Marinov
 */
public class SimpleStatusMenu
    extends StatusSelectorMenu
    implements ActionListener
{
    private final Logger logger = Logger.getLogger(SimpleStatusMenu.class);

    private final ProtocolProviderService protocolProvider;

    private final ImageIcon onlineIcon;

    private final ImageIcon offlineIcon;

    private final JMenuItem onlineItem;

    private final JMenuItem offlineItem;

    /**
     * Creates an instance of <tt>SimpleStatusMenu</tt>.
     * 
     * @param protocolProvider The protocol provider.
     */
    public SimpleStatusMenu(ProtocolProviderService protocolProvider)
    {
        this(protocolProvider,
            protocolProvider.getAccountID().getDisplayName(), new ImageIcon(
                protocolProvider.getProtocolIcon().getIcon(
                    ProtocolIcon.ICON_SIZE_16x16)));
    }

    private SimpleStatusMenu(ProtocolProviderService protocolProvider,
        String displayName, ImageIcon onlineIcon)
    {
        super(displayName, onlineIcon);

        this.protocolProvider = protocolProvider;

        this.onlineIcon = onlineIcon;
        this.offlineIcon =
            new ImageIcon(LightGrayFilter.createDisabledImage(onlineIcon
                .getImage()));

        this.setToolTipText("<html><b>" + displayName
            + "</b><br>Offline</html>");

        JLabel titleLabel = new JLabel(displayName);

        titleLabel.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 0));
        titleLabel.setFont(Constants.FONT.deriveFont(Font.BOLD));

        this.add(titleLabel);
        this.addSeparator();

        onlineItem =
            createMenuItem("service.gui.ONLINE", onlineIcon,
                Constants.ONLINE_STATUS);
        offlineItem =
            createMenuItem("service.gui.OFFLINE", offlineIcon,
                Constants.OFFLINE_STATUS);
    }

    private JMenuItem createMenuItem(String textKey, Icon icon, String name)
    {
        JMenuItem menuItem =
            new JMenuItem(GuiActivator.getResources().getI18NString(textKey),
                icon);

        menuItem.setName(Constants.ONLINE_STATUS);
        menuItem.addActionListener(this);
        this.add(menuItem);
        return menuItem;
    }

    /**
     * Handles the <tt>ActionEvent</tt> triggered when one of the items in the
     * list is selected.
     */
    public void actionPerformed(ActionEvent e)
    {
        JMenuItem menuItem = (JMenuItem) e.getSource();
        String itemName = menuItem.getName();

        if (itemName.equals(Constants.ONLINE_STATUS))
        {
            if (!protocolProvider.isRegistered())
            {
                GuiActivator.getUIService().getLoginManager().login(
                    protocolProvider);
            }
        }
        else
        {
            RegistrationState registrationState =
                protocolProvider.getRegistrationState();

            if (!registrationState.equals(RegistrationState.UNREGISTERED)
                && !registrationState.equals(RegistrationState.UNREGISTERING))
            {
                try
                {
                    GuiActivator.getUIService().getLoginManager()
                        .setManuallyDisconnected(true);
                    protocolProvider.unregister();
                }
                catch (OperationFailedException e1)
                {
                    logger.error("Unable to unregister the protocol provider: "
                        + protocolProvider
                        + " due to the following exception: " + e1);
                }
            }
        }

        saveStatusInformation(protocolProvider, itemName);
    }

    /**
     * Stops the timer that manages the connecting animated icon.
     */
    public void updateStatus()
    {
        String tooltip = this.getToolTipText();

        tooltip = tooltip.substring(0, tooltip.lastIndexOf("<br>"));

        if (protocolProvider.isRegistered())
        {
            setSelected(new SelectedObject(onlineIcon, onlineItem));

            // TODO Technically, we're not closing the html element.
            this.setToolTipText(tooltip.concat("<br>" + onlineItem.getText()));
        }
        else
        {
            setSelected(new SelectedObject(offlineIcon, offlineItem));

            this.setToolTipText(tooltip.concat("<br>" + offlineItem.getText()));
        }
    }

    public void updateStatus(Object status)
    {
    }
}
