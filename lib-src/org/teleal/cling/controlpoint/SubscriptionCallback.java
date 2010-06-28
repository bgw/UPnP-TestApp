/*
 * Copyright (C) 2010 Teleal GmbH, Switzerland
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.teleal.cling.controlpoint;

import org.teleal.cling.model.Constants;
import org.teleal.cling.model.Resource;
import org.teleal.cling.model.meta.DeviceService;
import org.teleal.cling.model.meta.LocalDevice;
import org.teleal.cling.model.meta.RemoteDevice;
import org.teleal.cling.model.gena.CancelReason;
import org.teleal.cling.model.gena.GENASubscription;
import org.teleal.cling.model.gena.LocalGENASubscription;
import org.teleal.cling.model.gena.RemoteGENASubscription;
import org.teleal.cling.model.message.UpnpResponse;
import org.teleal.common.util.Exceptions;

import java.util.Collections;
import java.util.logging.Logger;


public abstract class SubscriptionCallback implements Runnable {

    protected static Logger log = Logger.getLogger(SubscriptionCallback.class.getName());

    protected final DeviceService deviceService;
    protected final Integer requestedDurationSeconds;

    private ControlPoint controlPoint;
    private GENASubscription subscription;

    protected SubscriptionCallback(DeviceService deviceService) {
        this.deviceService = deviceService;
        this.requestedDurationSeconds = Constants.DEFAULT_SUBSCRIPTION_DURATION_SECONDS;
    }

    protected SubscriptionCallback(DeviceService deviceService, int requestedDurationSeconds) {
        this.deviceService = deviceService;
        this.requestedDurationSeconds = requestedDurationSeconds;
    }

    public DeviceService getDeviceService() {
        return deviceService;
    }

    synchronized public ControlPoint getControlPoint() {
        return controlPoint;
    }

    synchronized public void setControlPoint(ControlPoint controlPoint) {
        this.controlPoint = controlPoint;
    }

    synchronized public GENASubscription getSubscription() {
        return subscription;
    }

    synchronized public void setSubscription(GENASubscription subscription) {
        this.subscription = subscription;
    }

    synchronized public void run() {
        if (getControlPoint()  == null) {
            throw new IllegalStateException("Callback must be executed through ControlPoint");
        }

        if (getDeviceService().getDevice() instanceof LocalDevice) {
            establishLocalSubscription(deviceService);
        } else if (getDeviceService().getDevice() instanceof RemoteDevice) {
            establishRemoteSubscription(deviceService);
        }
    }

    private void establishLocalSubscription(DeviceService localService) {

        // Local execution of subscription on local service re-uses the procedure and lifecycle that is
        // used for incoming subscriptions from remote control points on local services!

        Resource foundResource =
                getControlPoint().getRegistry().getResource(localService.getEventSubscriptionURI());

        if (foundResource == null) {
            log.fine("Local device service is currently not registered, failing subscription immediately");
            failed(null, null, new IllegalStateException("Local device is not registered"));
            return;
        }

        LocalGENASubscription localSubscription =
                new LocalGENASubscription(localService, requestedDurationSeconds, Collections.EMPTY_LIST) {

                    public void failed(Exception ex) {
                        synchronized (SubscriptionCallback.this) {
                            SubscriptionCallback.this.setSubscription(null);
                            SubscriptionCallback.this.failed(null, null, ex);
                        }
                    }

                    public void established() {
                        synchronized (SubscriptionCallback.this) {
                            SubscriptionCallback.this.setSubscription(this);
                            SubscriptionCallback.this.established(this);
                        }
                    }

                    public void ended(CancelReason reason) {
                        synchronized (SubscriptionCallback.this) {
                            SubscriptionCallback.this.setSubscription(null);
                            SubscriptionCallback.this.ended(this, reason, null);
                        }
                    }

                    public void eventReceived() {
                        synchronized (SubscriptionCallback.this) {
                            log.fine("Local service state updated, notifying callback, sequence is: " + getCurrentSequence());
                            SubscriptionCallback.this.eventReceived(this);
                            incrementSequence();
                        }
                    }
                };

        try {
            log.fine("Local device service is currently registered, also registering subscription");
            getControlPoint().getRegistry().addLocalSubscription(localSubscription);

            log.fine("Notifying subscription callback of local subscription availablity");
            localSubscription.establish();

            log.fine("Simulating first initial event for local subscription callback, sequence: " + localSubscription.getCurrentSequence());
            eventReceived(localSubscription);
            localSubscription.incrementSequence();

            log.fine("Starting to monitor state changes of local service");
            localSubscription.registerOnService();

        } catch (Exception ex) {
            log.fine("Local callback creation failed: " + ex.toString());
            log.fine("Cause: " + Exceptions.unwrap(ex));
            log.fine("Source: " + Exceptions.unwrap(ex).getStackTrace()[0]);
            getControlPoint().getRegistry().removeLocalSubscription(localSubscription);
            localSubscription.end(CancelReason.LOCAL_CALLBACK_CREATION_FAILED);
            failed(localSubscription, null, ex);
        }
    }

    private void establishRemoteSubscription(DeviceService remoteService) {
        RemoteGENASubscription remoteSubscription =
                new RemoteGENASubscription(remoteService, requestedDurationSeconds) {

                    public void failed(UpnpResponse responseStatus) {
                        synchronized (SubscriptionCallback.this) {
                            SubscriptionCallback.this.setSubscription(null);
                            SubscriptionCallback.this.failed(this, responseStatus, null);
                        }
                    }

                    public void established() {
                        synchronized (SubscriptionCallback.this) {
                            SubscriptionCallback.this.setSubscription(this);
                            SubscriptionCallback.this.established(this);
                        }
                    }

                    public void ended(CancelReason reason, UpnpResponse responseStatus) {
                        synchronized (SubscriptionCallback.this) {
                            SubscriptionCallback.this.setSubscription(null);
                            SubscriptionCallback.this.ended(this, reason, responseStatus);
                        }
                    }

                    public void eventReceived() {
                        synchronized (SubscriptionCallback.this) {
                            SubscriptionCallback.this.eventReceived(this);
                        }
                    }

                    public void eventsMissed(int numberOfMissedEvents) {
                        synchronized (SubscriptionCallback.this) {
                            SubscriptionCallback.this.eventsMissed(this, numberOfMissedEvents);
                        }
                    }
                };

        getControlPoint().getProtocolFactory()
                .createSendingSubscribe(remoteSubscription)
                .run();
    }

    synchronized public void end() {
        if (subscription == null) return;
        if (subscription instanceof LocalGENASubscription) {
            endLocalSubscription((LocalGENASubscription)subscription);
        } else if (subscription instanceof RemoteGENASubscription) {
            endRemoteSubscription((RemoteGENASubscription)subscription);
        }
    }

    private void endLocalSubscription(LocalGENASubscription subscription) {
        log.fine("Removing local subscription and ending it in callback: " + subscription);
        getControlPoint().getRegistry().removeLocalSubscription(subscription);
        subscription.end(null); // No reason, on controlpoint request
    }

    private void endRemoteSubscription(RemoteGENASubscription subscription) {
        log.fine("Ending remote subscription: " + subscription);
        getControlPoint().getConfiguration().getSyncProtocolExecutor().execute(
                getControlPoint().getProtocolFactory().createSendingUnsubscribe(subscription)
        );
    }

    /**
     * Called when establishing a local or remote subscription failed. To get a nice error message that
     * transparently detects local or remote errors use <tt>createDefaultFailureMessage()</tt>.
     *
     * @param subscription   The failed subscription object, not very useful at this point.
     * @param responseStatus For a remote subscription, if a response was received at all, this is it, otherwise <tt>null</tt>.
     * @param exception      For a local subscription, any exception that caused the failure, otherwise <tt>null</tt>.
     * @see #createDefaultFailureMessage
     */
    protected abstract void failed(GENASubscription subscription, UpnpResponse responseStatus, Exception exception);

    protected abstract void established(GENASubscription subscription);

    /**
     * Called when a local or remote subscription ended, either on user request or because of a failure.
     *
     * @param subscription   The ended subscription instance.
     * @param reason         If the subscription ended regularly (through <tt>end()</tt>), this is <tt>null</tt>.
     * @param responseStatus For a remote subscription, if the cause implies a remopte response and it was
     *                       received, this is it (e.g. renewal failure response).
     */
    protected abstract void ended(GENASubscription subscription, CancelReason reason, UpnpResponse responseStatus);

    protected abstract void eventReceived(GENASubscription subscription);

    protected abstract void eventsMissed(GENASubscription subscription, int numberOfMissedEvents);

    public static String createDefaultFailureMessage(UpnpResponse responseStatus, Exception exception) {
        String message = "Subscription failed: ";
        if (responseStatus != null) {
            message = message + " HTTP response was: " + responseStatus.getResponseDetails();
        } else if (exception != null) {
            message = message + " Exception occured: " + exception;
        } else {
            message = message + " No response received.";
        }
        return message;
    }


    @Override
    public String toString() {
        return "(SubscriptionCallback) " + getDeviceService();
    }

}
