package org.opendaylight.ovsdb.openstack.netvirt.providers;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.clustering.Entity;
import org.opendaylight.controller.md.sal.common.api.clustering.CandidateAlreadyRegisteredException;
import org.opendaylight.controller.md.sal.common.api.clustering.EntityOwnershipCandidateRegistration;
import org.opendaylight.controller.md.sal.common.api.clustering.EntityOwnershipChange;
import org.opendaylight.controller.md.sal.common.api.clustering.EntityOwnershipListener;
import org.opendaylight.controller.md.sal.common.api.clustering.EntityOwnershipListenerRegistration;
import org.opendaylight.controller.md.sal.common.api.clustering.EntityOwnershipService;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ProviderContext;
import org.opendaylight.controller.sal.binding.api.BindingAwareProvider;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Sam Hague (shague@redhat.com)
 */
public class NetvirtProvidersProvider implements BindingAwareProvider, AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(NetvirtProvidersProvider.class);
    private static final String ENTITY_TYPE = "ovsdb-netvirt-provider";

    private BundleContext bundleContext = null;
    private static DataBroker dataBroker = null;
    private ConfigActivator activator;
    private static ProviderContext providerContext = null;
    private EntityOwnershipService entityOwnershipService;
    private ProviderEntityListener providerEntityListener = null;
    private AtomicBoolean hasProviderEntityOwnership = new AtomicBoolean(false);

    public NetvirtProvidersProvider(BundleContext bundleContext, EntityOwnershipService entityOwnershipService) {
        LOG.info("NetvirtProvidersProvider: bundleContext: {}", bundleContext);
        this.bundleContext = bundleContext;
        this.entityOwnershipService = entityOwnershipService;
    }

    public static DataBroker getDataBroker() {
        return dataBroker;
    }

    public static ProviderContext getProviderContext() {
        return providerContext;
    }

    @Override
    public void close() throws Exception {
        if (providerEntityListener != null) {
            providerEntityListener.close();
        }
        activator.stop(bundleContext);
    }

    @Override
    public void onSessionInitiated(ProviderContext providerContextRef) {
        dataBroker = providerContextRef.getSALService(DataBroker.class);
        providerContext = providerContextRef;
        LOG.info("NetvirtProvidersProvider: onSessionInitiated dataBroker: {}", dataBroker);
        this.activator = new ConfigActivator(providerContextRef);
        try {
            activator.start(bundleContext);
        } catch (Exception e) {
            LOG.warn("Failed to start Netvirt: ", e);
        }
        providerEntityListener = new ProviderEntityListener(this, entityOwnershipService);

    }

    private void handleOwnershipChange(EntityOwnershipChange ownershipChange) {
        if (ownershipChange.isOwner()) {
            LOG.info("*This* instance of OVSDB netvirt provider is set as a MASTER instance");
            hasProviderEntityOwnership.set(true);
        } else {
            LOG.info("*This* instance of OVSDB netvirt provider is set as a SLAVE instance");
            hasProviderEntityOwnership.set(false);
        }
    }

    private class ProviderEntityListener implements EntityOwnershipListener {
        private NetvirtProvidersProvider provider;
        private EntityOwnershipListenerRegistration listenerRegistration;
        private EntityOwnershipCandidateRegistration candidateRegistration;

        ProviderEntityListener(NetvirtProvidersProvider provider,
                               EntityOwnershipService entityOwnershipService) {
            this.provider = provider;
            this.listenerRegistration = entityOwnershipService.registerListener(ENTITY_TYPE, this);

            //register instance entity to get the ownership of the netvirt provider
            Entity instanceEntity = new Entity(ENTITY_TYPE, ENTITY_TYPE);
            try {
                this.candidateRegistration = entityOwnershipService.registerCandidate(instanceEntity);
            } catch (CandidateAlreadyRegisteredException e) {
                LOG.warn("OVSDB Netvirt Provider instance entity {} was already "
                        + "registered for {} ownership", instanceEntity, e);
            }
        }

        public void close() {
            this.listenerRegistration.close();
            this.candidateRegistration.close();
        }

        @Override
        public void ownershipChanged(EntityOwnershipChange ownershipChange) {
            provider.handleOwnershipChange(ownershipChange);
        }
    }
}
