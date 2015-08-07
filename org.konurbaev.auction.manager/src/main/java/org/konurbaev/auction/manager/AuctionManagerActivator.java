package org.konurbaev.auction.manager;

import java.util.HashMap;
import java.util.Map;

import org.osgi.framework.*;

import org.konurbaev.auction.Auction;
import org.konurbaev.auction.spi.Auctioneer;
import org.konurbaev.auction.spi.Auditor;

public class AuctionManagerActivator implements BundleActivator, ServiceListener {

    private BundleContext bundleContext;
    private Map<ServiceReference, ServiceRegistration> registeredAuctions =
            new HashMap<ServiceReference, ServiceRegistration>();
    private Map<ServiceReference, Auditor> registeredAuditors =
            new HashMap<ServiceReference, Auditor>();

    public void start(BundleContext bundleContext) throws Exception {
        this.bundleContext = bundleContext;

        ServiceReference[] auctioneerReferences = bundleContext.getServiceReferences(Auctioneer.class.getName(),null);
        if (auctioneerReferences != null) {
            for (ServiceReference serviceReference : auctioneerReferences) {
                registerService(serviceReference);
            }
        }

        ServiceReference[] auditorReferences = bundleContext.getServiceReferences(Auditor.class.getName(),null);
        if (auditorReferences != null) {
            for (ServiceReference serviceReference : auditorReferences) {
                registerService(serviceReference);
            }
        }

        String auctionOrAuctioneerFilter =
                "(|" +
                        "(objectClass=" + Auctioneer.class.getName() + ")" +
                        "(objectClass=" + Auditor.class.getName() + ")" +
                        ")";

        bundleContext.addServiceListener(this,
                auctionOrAuctioneerFilter);
    }

    public void stop(BundleContext bundleContext) throws Exception {
        bundleContext.removeServiceListener(this);
    }

    public void serviceChanged(ServiceEvent serviceEvent) {
        ServiceReference serviceReference = serviceEvent.getServiceReference();

        switch (serviceEvent.getType()) {
            case ServiceEvent.REGISTERED: {
                registerService(serviceReference);
                break;
            }
            case ServiceEvent.UNREGISTERING: {
                String [] serviceInterfaces =
                        (String[]) serviceReference.getProperty("objectClass");
                if (Auctioneer.class.getName().equals(serviceInterfaces[0])) {
                    unregisterAuctioneer(serviceReference);
                } else {
                    unregisterAuditor(serviceReference);
                }
                bundleContext.ungetService(serviceReference);
                break;
            }
            default:
                // do nothing
        }
    }

    private void registerService(ServiceReference serviceReference) {
        Object serviceObject =
                bundleContext.getService(serviceReference);

        if (serviceObject instanceof Auctioneer) {
            registerAuctioneer(serviceReference, (Auctioneer) serviceObject);
        } else {
            registerAuditor(serviceReference, (Auditor) serviceObject);
        }
    }

    private void registerAuditor(ServiceReference auditorServiceReference, Auditor auditor) {
        registeredAuditors.put(auditorServiceReference, auditor);
    }

    private void registerAuctioneer(ServiceReference auctioneerServiceReference,
                                    Auctioneer auctioneer) {
        Auction auction =
                new AuctionWrapper(auctioneer, registeredAuditors.values());

        ServiceRegistration auctionServiceRegistration =
                bundleContext.registerService(Auction.class.getName(),
                        auction, auctioneer.getAuctionProperties());

        registeredAuctions.put(auctioneerServiceReference, auctionServiceRegistration);
    }

    private void unregisterAuditor(ServiceReference serviceReference) {
        registeredAuditors.remove(serviceReference);
    }

    private void unregisterAuctioneer(ServiceReference auctioneerServiceReference) {
        ServiceRegistration auctionServiceRegistration =
                registeredAuctions.remove(auctioneerServiceReference);

        if (auctionServiceRegistration != null) {
            auctionServiceRegistration.unregister();
        }
    }
}